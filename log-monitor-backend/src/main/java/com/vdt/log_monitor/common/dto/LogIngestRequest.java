package com.vdt.log_monitor.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vdt.log_monitor.common.enums.LogLevel;
import lombok.Data;

import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // bỏ qua các field không có trong class
public class LogIngestRequest {
    private String docId;
    private String traceId;
    private LogLevel logLevel;
    private String environment;
    private String serviceName;
    private String appName;
    private String thread;
    private String logger;
    private String message;
//    private String stackTrace;
    private Instant eventTimestamp;
    private String hostName;
}