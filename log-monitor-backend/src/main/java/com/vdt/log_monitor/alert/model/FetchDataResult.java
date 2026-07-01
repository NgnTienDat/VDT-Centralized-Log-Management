package com.vdt.log_monitor.alert.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FetchDataResult {
    // Lưu danh sách các trường dùng để group by (Ví dụ: ["service", "status"])
    private List<String> groupByFields;

    // Lưu kết quả metric tính toán được (Ví dụ: {"auth-service|500": 15.0, "payment-service|200": 1200.0})
    private Map<String, Double> metrics;


    /*
     * JSON mẫu trả về từ Elasticsearch Aggregation có thể được ánh xạ sang cấu trúc FetchDataResult như sau:
     *  {
     *     "groupByFields": ["service", "status"],
     *     "metrics": {
     *       "auth-service|500": 15.0,
     *       "payment-service|200": 1200.0
     *   }
     * */
}