package com.vdt.log_monitor.alert.scheduler;

import com.vdt.log_monitor.alert.engine.ExpressionEngine;
import com.vdt.log_monitor.alert.enums.AlertState;
import com.vdt.log_monitor.alert.model.AlertNotificationDocument;
import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.PipelineResult;
import com.vdt.log_monitor.alert.model.RuleConfig;
import com.vdt.log_monitor.alert.model.TriggerResult;
import com.vdt.log_monitor.alert.AlertNotificationRepository;
import com.vdt.log_monitor.alert.AlertRuleRepository;
import com.vdt.log_monitor.websocket.AlertNotificationPayload;
import com.vdt.log_monitor.websocket.AlertPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertSchedulerManager {

    private final ThreadPoolTaskScheduler alertTaskScheduler;
    private final ExpressionEngine expressionEngine;
    private final AlertRuleRepository ruleRepository;
    private final AlertNotificationRepository notificationRepository;
    private final AlertPublisher alertPublisher;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleRule(RuleConfig rule) {
        cancelRule(rule.getRuleId());

        if (rule.getIsActive() != null && !rule.getIsActive()) {
            return;
        }
        /*
         * Ví dụ: 10:00 phát alert, lastNotifiedTime = 10:00, mỗi 2p nó lại lỗi và có
         * trigger.
         * Nếu cứ liên tục trượt về quá khứ 5p mà vẫn còn trigger alert thì không cập
         * nhật lại lastNotifiedTime,
         * để tránh spam alert. Cho đến khi now-lastNotifiedTime >= repeatIntervalMs thì
         * mới gửi alert tiếp.
         *
         * NGOẠI LỆ: nếu xuất hiện group breach MỚI (group chưa từng có trong lần thông
         * báo gần nhất)
         * trong lúc đang FIRING + còn cooldown, thì BỎ QUA cooldown và báo ngay - vì
         * đây là thông tin
         * mới (vd thêm 1 service khác cũng đang lỗi), không phải lặp lại thông báo cũ.
         */

        Runnable alertTask = () -> {
            try {
                log.info("⏰ Bắt đầu thực thi Rule hẹn giờ: {}", rule.getName());

                // 1. Chạy core pipeline engine
                PipelineResult pipelineResult = expressionEngine.runPipeline(rule.getPipelineSteps(),
                        rule.getTriggerStepId());
                boolean isAlertTriggered = pipelineResult.isTriggered();

                long now = Instant.now().toEpochMilli();
                boolean shouldNotify = false;

                if (isAlertTriggered) {
                    log.warn("🚨 [ALERT TRIGGERED] Rule: {}", rule.getName());

                    TriggerResult triggerResult = extractTriggerResult(rule, pipelineResult);
                    List<String> currentBreachedGroups = (triggerResult != null
                            && triggerResult.getBreachedGroups() != null)
                                    ? triggerResult.getBreachedGroups()
                                    : List.of();
                    List<String> lastNotifiedGroups = rule.getLastNotifiedBreachedGroups() != null
                            ? rule.getLastNotifiedBreachedGroups()
                            : List.of();
                    // true nếu có ít nhất 1 group đang breach mà lần thông báo gần nhất CHƯA từng
                    // có
                    boolean hasNewBreachedGroup = !lastNotifiedGroups.containsAll(currentBreachedGroups);

                    // Kiểm tra trạng thái Alert State & Cooldown để chặn spam tin nhắn
                    if (rule.getAlertState() != AlertState.FIRING) {
                        // Trạng thái chuyển giao từ OK -> FIRING: Gửi cảnh báo ngay lập tức
                        shouldNotify = true;
                        rule.setAlertState(AlertState.FIRING);
                    } else if (hasNewBreachedGroup) {
                        // Đang FIRING nhưng có group mới chưa từng được báo -> bỏ qua cooldown
                        shouldNotify = true;
                        log.info(
                                "🆕 Rule [{}] phát hiện group breach mới (chưa từng báo) -> bỏ qua cooldown, báo ngay.",
                                rule.getName());
                    } else {
                        // Đang FIRING, breach set không có gì mới -> tính chu kỳ nhắc lại như cũ
                        // (repeatIntervalMinutes)
                        long lastNotified = rule.getLastNotifiedTime() != null ? rule.getLastNotifiedTime() : 0L;
                        // Nếu không có repeatIntervalMinutes, mặc định là 30 phút
                        long repeatIntervalMs = (rule.getRepeatIntervalMinutes() != null
                                ? rule.getRepeatIntervalMinutes()
                                : 30) * 60 * 1000L;

                        // Nếu đã qua thời gian chờ (cooldown), cho phép gửi thông báo tiếp
                        if (now - lastNotified >= repeatIntervalMs) {
                            shouldNotify = true;
                        }
                    }

                    // Gửi thông báo qua WebSocket nếu thỏa mãn điều kiện Cooldown (hoặc bypass do
                    // có group mới)
                    if (shouldNotify) {
                        rule.setLastNotifiedTime(now);
                        rule.setLastNotifiedBreachedGroups(currentBreachedGroups);
                        if (triggerResult != null) {
                            // printTriggerResultAndNotification(rule, pipelineResult);
                            publishAlertNotification(rule, triggerResult, now);

                        }
                    } else {
                        log.info(
                                "⏳ Rule [{}] duy trì trạng thái FIRING nhưng bỏ qua do đang trong thời gian chờ (cooldown).",
                                rule.getName());
                    }
                } else {
                    // Hệ thống đã hoạt động ổn định trở lại
                    if (rule.getAlertState() == AlertState.FIRING) {
                        log.info("✅ [ALERT RESOLVED] Rule: {} đã quay trở lại trạng thái bình thường.", rule.getName());
                    }
                    rule.setAlertState(AlertState.OK);
                    // Reset lại danh sách đã báo - lần FIRING tiếp theo sẽ coi mọi group là "mới"
                    // và báo ngay
                    rule.setLastNotifiedBreachedGroups(List.of());
                }

                // Cập nhật thông tin thực thi về Elasticsearch làm dữ liệu cho Frontend hiển
                // thị
                rule.setLastRunTime(now);
                ruleRepository.save(rule);

            } catch (Exception e) {
                log.error("❌ Lỗi khi thực thi Rule [{}]: {}", rule.getRuleId(), e.getMessage(), e);
            }
        };

        Duration interval = Duration.ofMinutes(rule.getIntervalMinutes());
        ScheduledFuture<?> futureTask = alertTaskScheduler.scheduleWithFixedDelay(alertTask, interval);
        scheduledTasks.put(rule.getRuleId(), futureTask);
        log.info("🚀 Đã bật đồng hồ hẹn giờ cho Rule [{}] - Chu kỳ: {} phút", rule.getRuleId(),
                rule.getIntervalMinutes());
    }

    public void cancelRule(String ruleId) {
        ScheduledFuture<?> futureTask = scheduledTasks.remove(ruleId);
        if (futureTask != null) {
            futureTask.cancel(false);
        }
    }

    /**
     * Lấy TriggerResult từ context của step được gán làm Trigger.
     * Trả về null + log lỗi nếu dữ liệu không hợp lệ (không nên xảy ra vì
     * ExpressionEngine đã validate, đây là lớp phòng hờ).
     */
    private TriggerResult extractTriggerResult(RuleConfig rule, PipelineResult pipelineResult) {
        ExpressionResult triggerStepResult = pipelineResult.getContext().get(rule.getTriggerStepId());
        Object rawValue = triggerStepResult != null ? triggerStepResult.getValue() : null;

        if (rawValue instanceof TriggerResult triggerResult) {
            return triggerResult;
        }
        log.error("Trigger step '{}' không trả về TriggerResult hợp lệ.", rule.getTriggerStepId());
        return null;
    }

    private void publishAlertNotification(RuleConfig rule, TriggerResult triggerResult, long timestamp) {
        RuleConfig.NotificationTemplate template = rule.getNotificationTemplate();
        String title = template != null ? renderTemplate(template.getTitle(), rule, triggerResult) : null;
        String message = template != null ? renderTemplate(template.getMessage(), rule, triggerResult) : null;

        if (title == null) {
            title = "🚨 [ALERT] " + rule.getName();
        }
        if (message == null) {
            // Dùng computedValues nếu có (% hoặc giá trị đã tính), fallback về
            // breachedGroupValues
            Map<String, Double> displayValues = (triggerResult.getComputedValues() != null
                    && !triggerResult.getComputedValues().isEmpty())
                            ? triggerResult.getComputedValues()
                            : triggerResult.getBreachedGroupValues();

            message = "Rule [" + rule.getName() + "] đã breach: "
                    + formatBreachedGroupValues(displayValues);
        }

        AlertNotificationPayload payload = AlertNotificationPayload.builder()
                .ruleId(rule.getRuleId())
                .ruleName(rule.getName())
                .alertState(rule.getAlertState().name())
                .triggerStepId(rule.getTriggerStepId())
                .triggered(triggerResult.isTriggered())
                .breachedGroups(triggerResult.getBreachedGroups())
                .breachedGroupValues(triggerResult.getBreachedGroupValues())
                .computedValues(triggerResult.getComputedValues())
                .groupByFields(triggerResult.getGroupByFields())
                .scopeLabel(triggerResult.getScopeLabel())
                .title(title)
                .message(message)
                .timestamp(timestamp)
                .build();

        System.out.println("📢 [ALERT NOTIFICATION PAYLOAD] " + payload.toString());

        try {
            AlertNotificationDocument doc = AlertNotificationDocument.builder()
                    .ruleId(payload.getRuleId())
                    .ruleName(payload.getRuleName())
                    .triggerStepId(payload.getTriggerStepId())
                    .alertState(payload.getAlertState())
                    .triggered(payload.isTriggered())
                    .breachedGroups(payload.getBreachedGroups())
                    .breachedGroupValues(payload.getBreachedGroupValues())
                    .groupByFields(payload.getGroupByFields())
                    .title(payload.getTitle())
                    .message(payload.getMessage())
                    .timestamp(payload.getTimestamp())
                    .build();

            notificationRepository.save(doc);
            log.debug("💾 Đã lưu log alert notification đầy đủ vào ES cho ruleId: {}", rule.getRuleId());

            alertPublisher.publish(payload);
            log.debug("📤 Đã publish alert notification qua WebSocket cho ruleId: {}", rule.getRuleId());
        } catch (Exception e) {
            log.error("❌ Lỗi khi lưu alert notification vào Elasticsearch: ", e);
        }
    }

    private String renderTemplate(String template, RuleConfig rule, TriggerResult triggerResult) {
        if (template == null) {
            return null;
        }
        String result = template;
        result = result.replace("{ruleName}", String.valueOf(rule.getName()));
        result = result.replace("{breachedGroups}", String.valueOf(triggerResult.getBreachedGroups()));
        result = result.replace("{breachedGroupValues}",
                formatBreachedGroupValues(triggerResult.getBreachedGroupValues()));
        result = result.replace("{groupByFields}", String.valueOf(triggerResult.getGroupByFields()));
        if (triggerResult.getMetadata() != null) {
            for (Map.Entry<String, Object> entry : triggerResult.getMetadata().entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private String formatBreachedGroupValues(Map<String, Double> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }
}