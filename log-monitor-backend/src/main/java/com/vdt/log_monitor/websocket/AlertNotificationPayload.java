package com.vdt.log_monitor.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Payload gửi qua WebSocket (topic /topic/alerts) khi 1 Rule alert được
 * trigger.
 *
 * Thay đổi so với phiên bản cũ:
 * + computedValues: kết quả tính toán thực tế (% lỗi, ratio...) tương ứng
 * với từng group trong breachedGroups.
 * + scopeLabel: mô tả phạm vi đánh giá thay thế "DEFAULT".
 *
 * Frontend có thể render:
 * breachedGroupValues → "18 log ERROR" (raw count)
 * computedValues → "15.00%" (tỉ lệ đã tính)
 * scopeLabel → "Toàn hệ thống" (khi không groupBy)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotificationPayload {

    private String ruleId;
    private String ruleName;
    private String triggerStepId;
    private String alertState; // "OK" | "FIRING"

    private boolean triggered;

    /** Danh sách group vi phạm — key đã được thay "DEFAULT" bằng scopeLabel */
    private List<String> breachedGroups;

    /** Raw value từ input (count thô, số tuyệt đối) theo từng group */
    private Map<String, Double> breachedGroupValues;

    /**
     * [MỚI] Giá trị đã tính toán (%, ratio...) theo từng group.
     * Với EVALUATE_THRESHOLD: bằng breachedGroupValues (không có bước tính trung
     * gian).
     * Với MATH: kết quả của biểu thức tại group đó (vd 15.0 = 15%).
     */
    private Map<String, Double> computedValues;

    /** Các trường được dùng để phân nhóm. Rỗng nếu không groupBy */
    private List<String> groupByFields;

    /**
     * [MỚI] Mô tả phạm vi đánh giá.
     * "Toàn hệ thống" khi groupByFields rỗng.
     * "Theo service, environment" khi có groupBy.
     */
    private String scopeLabel;

    private String title;
    private String message;
    private long timestamp;
}