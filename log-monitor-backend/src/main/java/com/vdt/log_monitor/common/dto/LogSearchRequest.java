package com.vdt.log_monitor.common.dto;

import com.vdt.log_monitor.common.enums.LogLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LogSearchRequest {
    String environment;
    String appName;
    String serviceName;
    LogLevel logLevel;
    String q; // keyword for full-text search in log message content

    // @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    // Instant from;

    // @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    // Instant to;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant before;
    String beforeId;

    @Builder.Default
    Integer size = 5;
}