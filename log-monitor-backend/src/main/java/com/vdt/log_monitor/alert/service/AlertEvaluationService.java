package com.vdt.log_monitor.alert.service;

import com.vdt.log_monitor.alert.config.AlertRuleLoader;
import com.vdt.log_monitor.alert.dto.AlertEvent;
import com.vdt.log_monitor.alert.dto.AlertRule;
import com.vdt.log_monitor.alert.repository.AlertQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Core orchestration service for the rule-based alerting engine.
 * <p>
 * Iterates over all enabled rules, queries Elasticsearch for matching log counts,
 * evaluates thresholds, respects cooldowns, and publishes alerts via WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluationService {

    private final AlertRuleLoader alertRuleLoader;
    private final AlertQueryRepository alertQueryRepository;
    private final AlertCooldownService alertCooldownService;
    private final AlertWebSocketPublisher alertWebSocketPublisher;

    /**
     * Evaluates all loaded alert rules against the current Elasticsearch data.
     * <p>
     * For each enabled rule:
     * <ol>
     *   <li>Queries ES Count API for matching logs within the sliding window</li>
     *   <li>Compares count against the rule's threshold</li>
     *   <li>Checks cooldown to prevent alert fatigue</li>
     *   <li>If all conditions are met, generates and publishes an {@link AlertEvent}</li>
     * </ol>
     */
    public void evaluateRules() {
        for (AlertRule rule : alertRuleLoader.getRules()) {
            if (!rule.isEnabled()) continue;

            try {
                // 1. Tính toán cửa sổ trượt tiêu chuẩn dựa trên cấu hình rule
                Instant windowStart = Instant.now().minus(rule.getWindowMinutes(), java.time.temporal.ChronoUnit.MINUTES);

                // 2. Lấy mốc alert gần nhất (nếu có)
                Instant lastTriggered = alertCooldownService.getLastTriggeredTime(rule.getId());

                // 3. Nếu thời điểm alert gần nhất cũ hơn cả windowStart (tức là cooldown > window),
                // kéo giãn windowStart về tận thời điểm alert đó để không bỏ sót khoảng trống
                if (lastTriggered != null && lastTriggered.isBefore(windowStart)) {
                    windowStart = lastTriggered;
                }

                // 4. Truyền mốc thời gian thông minh vào Repository để truy vấn
                long matchedCount = alertQueryRepository.countMatchingLogs(rule, windowStart);

                if (matchedCount < rule.getThreshold()) {
                    log.debug("Rule [{}]: count {} below threshold {}, no alert",
                            rule.getId(), matchedCount, rule.getThreshold());
                    continue;
                }

                if (!alertCooldownService.tryAcquire(rule)) {
                    log.debug("Rule [{}]: cooldown active, skip", rule.getId());
                    continue;
                }

                alertWebSocketPublisher.publish(buildAlertEvent(rule, matchedCount));

                log.warn("ALERT TRIGGERED — Rule [{}]: {} {} logs in the last {} minutes (threshold: {})",
                        rule.getId(), matchedCount, rule.getLevel(),
                        rule.getWindowMinutes(), rule.getThreshold());

            } catch (Exception e) {
                log.error("Error evaluating rule [{}]: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }

    private AlertEvent buildAlertEvent(AlertRule rule, long matchedCount) {
        String title = String.format("Alert: %s", rule.getId());
        String message = String.format("Detected %d %s logs during the last %d minutes.",
                matchedCount, rule.getLevel(), rule.getWindowMinutes());

        return AlertEvent.builder()
                .ruleId(rule.getId())
                .title(title)
                .message(message)
                .environment(rule.getEnvironment())
                .application(rule.getApplication())
                .severity(rule.getSeverity() != null ? rule.getSeverity().name() : "UNKNOWN")
                .matchedCount(matchedCount)
                .triggeredAt(Instant.now())
                .build();
    }
}
