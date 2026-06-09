package com.vdt.log_monitor.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    private Instant timestamp;

    private String level;        // ERROR | WARN | INFO | DEBUG

    private String environment;  // dev | staging | test | prod

    private String service;      // auth-service | order-service | ...

    private String traceId;      // xuyên suốt 1 request qua nhiều service

    private String spanId;

    private String thread;

    private String message;

    private Long durationMs;     // null với log không phải request log

    private String stackTrace;   // chỉ có khi level = ERROR
}