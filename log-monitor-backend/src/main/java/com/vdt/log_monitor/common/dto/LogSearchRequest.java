package com.vdt.log_monitor.common.dto;

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
    String logLevel;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant to;

    @Builder.Default
    Integer page = 0;

    @Builder.Default
    Integer size = 20;
}