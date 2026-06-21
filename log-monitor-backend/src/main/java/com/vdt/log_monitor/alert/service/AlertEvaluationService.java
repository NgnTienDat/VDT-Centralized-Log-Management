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
            // Skip disabled rules
            if (!rule.isEnabled()) {
                log.debug("Skipping disabled rule [{}]", rule.getId());
                continue;
            }

            try {
                // Query ES for matching log count
                long matchedCount = alertQueryRepository.countMatchingLogs(rule);

                // Check threshold
                if (matchedCount < rule.getThreshold()) {
                    log.debug("Rule [{}]: count {} below threshold {}, no alert",
                            rule.getId(), matchedCount, rule.getThreshold());
                    continue;
                }

                // Check cooldown
                if (!alertCooldownService.canTrigger(rule)) {
                    log.debug("Rule [{}]: threshold exceeded ({} >= {}) but cooldown is active",
                            rule.getId(), matchedCount, rule.getThreshold());
                    continue;
                }

                // Build and publish alert event
                AlertEvent event = buildAlertEvent(rule, matchedCount);
                alertWebSocketPublisher.publish(event);

                // Update cooldown state
                alertCooldownService.markTriggered(rule);

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
