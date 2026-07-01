package com.vdt.log_monitor.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Payload gửi qua WebSocket (topic /topic/alerts) khi 1 Rule alert được trigger.
 *
 * Đây là "wire format" tách biệt khỏi TriggerResult nội bộ của alert engine.
 * Nếu sau này cấu trúc TriggerResult ở alert/model thay đổi (vd thêm field mới
 * cho 1 executor mới), hợp đồng dữ liệu với frontend ở đây không tự nhiên bị vỡ -
 * AlertSchedulerManager là nơi duy nhất ánh xạ TriggerResult -> payload này.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotificationPayload {

    private String ruleId;
    private String ruleName;
    private String triggerStepId;
    private String alertState; // OK, FIRING

    private boolean triggered;
    private List<String> breachedGroups;
    private Map<String, Double> breachedGroupValues;
    private List<String> groupByFields;

    // Thông tin bổ sung tùy executor nào quyết định trigger (thresholdValue, operator, actualValues, expression...)
    // private Map<String, Object> metadata;

    private String title;
    private String message;

    private long timestamp;
}