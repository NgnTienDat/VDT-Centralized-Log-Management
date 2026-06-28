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
 * Trước đây MathExecutor và EvaluateThresholdExecutor mỗi nơi tự build một
 * Map<String, Object> với các key dạng String ("triggered", "breachedGroups"...)
 * -> ExpressionEngine phải đoán cấu trúc Map đó bằng instanceof + get("triggered").
 * Nếu sau này có executor mới trả về BOOLEAN nhưng quên đúng key string,
 * lỗi sẽ không được phát hiện tới khi chạy.
 *
 * Từ giờ: BẤT KỲ executor nào trả về ResultType.BOOLEAN đều PHẢI đóng gói value
 * vào TriggerResult. Đây là phần mở rộng tương thích ngược an toàn (type-safe),
 * không cần sửa ExpressionEngine mỗi khi có executor BOOLEAN mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerResult {

    private boolean triggered;

    @Builder.Default
    private List<String> breachedGroups = List.of();

    @Builder.Default
    private List<String> groupByFields = List.of();

    // Thông tin bổ sung riêng của từng executor (expression, operator, thresholdValue, actualValues...)
    // Không bắt buộc phải đọc field này để pipeline chạy đúng -> engine không phụ thuộc vào nó.
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
