package com.vdt.log_monitor.alert.scheduler;

import com.vdt.log_monitor.alert.engine.ExpressionEngine;
import com.vdt.log_monitor.alert.enums.AlertState;
import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.PipelineResult;
import com.vdt.log_monitor.alert.model.RuleConfig;
import com.vdt.log_monitor.alert.model.TriggerResult;
import com.vdt.log_monitor.alert.AlertRuleRepository;
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

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleRule(RuleConfig rule) {
        cancelRule(rule.getRuleId());

        if (rule.getIsActive() != null && !rule.getIsActive()) {
            return;
        }
        /*
        * Ví dụ: 10:00 phát alert, lastNotifiedTime = 10:00, mỗi 2p nó lại lỗi và có trigger.
        * Nếu cứ liên tục trượt về quá khứ 5p mà vẫn còn trigger alert thì không cập nhật lại lastNotifiedTime,
        * để tránh spam alert. Cho đến khi now-lastNotifiedTime >= repeatIntervalMs thì mới gửi alert tiếp.
        * */
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

                    // Kiểm tra trạng thái Alert State & Cooldown để chặn spam tin nhắn
                    if (rule.getAlertState() != AlertState.FIRING) {
                        // Trạng thái chuyển giao từ OK -> FIRING: Gửi cảnh báo ngay lập tức
                        shouldNotify = true;
                        rule.setAlertState(AlertState.FIRING);
                    } else {
                        // Đang ở trạng thái FIRING, tính toán chu kỳ nhắc lại (repeatIntervalMinutes)
                        long lastNotified = rule.getLastNotifiedTime() != null ? rule.getLastNotifiedTime() : 0L;
                        // Nếu không có repeatIntervalMinutes, mặc định là 30 phút
                        long repeatIntervalMs = (rule.getRepeatIntervalMinutes() != null
                                ? rule.getRepeatIntervalMinutes()
                                : 30) * 60 * 1000L;

                        // Nếu đã qua thời gian chờ (cooldown), cho phép gửi thông báo tiếp
                        // Cụ thể: nếu lần cuối thông báo cách đây repeatIntervalMs hoặc lần đầu tiên (lastNotified = 0), thì gửi thông báo
                        if (now - lastNotified >= repeatIntervalMs) {
                            shouldNotify = true;
                        }
                    }

                    // In kết quả thô + thông báo đã render nếu thỏa mãn điều kiện Cooldown
                    if (shouldNotify) {
                        rule.setLastNotifiedTime(now);
                        printTriggerResultAndNotification(rule, pipelineResult);
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
     * In ra console kết quả thô của step Trigger (TriggerResult) và bản thông báo
     * đã render
     * từ RuleConfig.notificationTemplate.
     *
     * Placeholder hỗ trợ: {ruleName}, {breachedGroups}, {groupByFields}, và BẤT KỲ
     * key nào
     * có trong TriggerResult.metadata (vd {thresholdValue}, {operator},
     * {actualValues},
     * {expression}...). Vì lấy trực tiếp từ metadata, executor mới thêm key
     * metadata mới
     * thì template tự nhận, không cần sửa hàm này -> giữ đúng tinh thần OCP.
     */
    private void printTriggerResultAndNotification(RuleConfig rule, PipelineResult pipelineResult) {
        ExpressionResult triggerStepResult = pipelineResult.getContext().get(rule.getTriggerStepId());
        Object rawValue = triggerStepResult != null ? triggerStepResult.getValue() : null;

        // log.info("--- [KẾT QUẢ THÔ TỪ TRIGGER STEP: {}] ---",
        // rule.getTriggerStepId());
        // log.info("Dữ liệu thô (TriggerResult): {}", rawValue);
        // log.info("---------------------------------------------------------------------");

        System.out.println("--- [KẾT QUẢ THÔ TỪ TRIGGER STEP: " + rule.getTriggerStepId() + "] ---");
        System.out.println("Dữ liệu thô (TriggerResult): " + rawValue);
        System.out.println("---------------------------------------------------------------------");

        if (!(rawValue instanceof TriggerResult triggerResult)) {
            // Không nên xảy ra vì ExpressionEngine đã validate, nhưng phòng hờ để tránh
            // NPE/ClassCastException
            log.error("Trigger step '{}' không trả về TriggerResult hợp lệ, bỏ qua việc render thông báo.",
                    rule.getTriggerStepId());
            return;
        }

        RuleConfig.NotificationTemplate template = rule.getNotificationTemplate();
        String title = template != null ? renderTemplate(template.getTitle(), rule, triggerResult) : null;
        String message = template != null ? renderTemplate(template.getMessage(), rule, triggerResult) : null;

        if (title == null && message == null) {
            // Rule chưa cấu hình notificationTemplate -> vẫn in thông báo mặc định để
            // dev/tester nhìn thấy gì đó
            // log.warn("📢 [THÔNG BÁO] Rule [{}] đã breach các group: {}", rule.getName(),
            // triggerResult.getBreachedGroups());
            System.out.println("📢 WARN [THÔNG BÁO] Rule [" + rule.getName() + "] đã breach các group: "
                    + triggerResult.getBreachedGroups());
            System.out.println("---------------------------------------------------------------------");

        } else {
            // log.warn("📢 [THÔNG BÁO] {}", title);
            // log.warn(" {}", message);
            System.out.println("📢 WARN [THÔNG BÁO] " + title);
            System.out.println("    " + message);
            System.out.println("---------------------------------------------------------------------");

        }
    }

    private String renderTemplate(String template, RuleConfig rule, TriggerResult triggerResult) {
        if (template == null) {
            return null;
        }
        String result = template;
        result = result.replace("{ruleName}", String.valueOf(rule.getName()));
        result = result.replace("{breachedGroups}", String.valueOf(triggerResult.getBreachedGroups()));
        result = result.replace("{groupByFields}", String.valueOf(triggerResult.getGroupByFields()));
        if (triggerResult.getMetadata() != null) {
            for (Map.Entry<String, Object> entry : triggerResult.getMetadata().entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
        }
        return result;
    }
}