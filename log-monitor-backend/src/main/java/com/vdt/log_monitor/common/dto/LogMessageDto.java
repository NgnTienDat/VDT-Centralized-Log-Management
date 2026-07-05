package com.vdt.log_monitor.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vdt.log_monitor.common.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO đại diện cho 1 log entry được push qua STOMP.
 * JsonInclude.NON_NULL: bỏ qua các field null khi serialize
 * → payload gọn hơn, không gửi "stackTrace: null" với log INFO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogMessageDto {

    private String id;           // ES document _id — dùng để gọi GET /api/logs/{id}
    private String docId;        // doc_id gốc từ Logstash, có thể dùng để tra cứu log trong ES nếu cần
    private Instant eventTimestamp;
    private LogLevel logLevel;        // ERROR | WARN | INFO | DEBUG
    private String environment;  // dev | staging | test | prod
    private String serviceName;      // auth-service | order-service | ...
    private String appName;        // tên ứng dụng, có thể trùng serviceName hoặc khác nếu muốn phân tách rõ hơn
    private String hostName;
    private String logger;        // tên logger, thường là tên class (ví dụ: com.vdt.auth.AuthService)
    private String traceId;      // xuyên suốt 1 request qua nhiều service
    private String thread;
    private String logMessage;   // main message đã tách stack trace (nếu có)
    private Instant sentTimestamp;   // thời điểm backend publish message ra STOMP
    private Long durationMs;     // null với log không phải request log
    private String stackTrace;

}