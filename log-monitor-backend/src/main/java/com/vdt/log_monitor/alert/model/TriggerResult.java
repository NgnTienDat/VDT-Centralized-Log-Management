package com.vdt.log_monitor.alert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hợp đồng dữ liệu thống nhất cho ResultType.BOOLEAN.
 *
 * Thay đổi so với phiên bản cũ:
 * + computedValues: kết quả tính toán thực tế của biểu thức (%, ratio, số tuyệt
 * đối...)
 * khác với breachedGroupValues là raw count từ input.
 * MathExecutor điền field này; EvaluateThresholdExecutor điền
 * actualValues vào metadata như cũ (đã có sẵn).
 * + scopeLabel: chuỗi mô tả phạm vi đánh giá — thay thế "DEFAULT" khi không có
 * groupBy.
 * VD: "Toàn hệ thống", "auth-service|dev"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerResult {

    private boolean triggered;

    @Builder.Default
    private List<String> breachedGroups = List.of();

    /**
     * Raw value từ input — thường là count thô (số log, số request...).
     * Giữ nguyên để backward compatible với các nơi đang đọc field này.
     */
    @Builder.Default
    private Map<String, Double> breachedGroupValues = new HashMap<>();

    /**
     * [MỚI] Giá trị đã tính toán từ biểu thức — % lỗi, ratio, tổng hợp...
     * Key là groupKey (giống breachedGroups), value là kết quả số của biểu thức
     * tại group đó.
     *
     * Ví dụ với rule tỉ lệ lỗi:
     * breachedGroupValues = { "auth-service|dev": 18.0 } ← error count thô
     * computedValues = { "auth-service|dev": 15.0 } ← % thực tế (18/120*100)
     *
     * Với EvaluateThresholdExecutor (không có biểu thức tính toán):
     * computedValues = breachedGroupValues (cùng giá trị, vì không có bước tính
     * trung gian)
     */
    @Builder.Default
    private Map<String, Double> computedValues = new HashMap<>();

    @Builder.Default
    private List<String> groupByFields = List.of();

    /**
     * [MỚI] Nhãn mô tả phạm vi — thay thế "DEFAULT" khi groupByFields rỗng.
     * Được set bởi executor hoặc AlertSchedulerManager.
     * VD: "Toàn hệ thống", "All services", hoặc join của groupByFields.
     */
    @Builder.Default
    private String scopeLabel = null;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}