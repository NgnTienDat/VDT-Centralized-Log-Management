package com.vdt.log_monitor.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vdt.log_monitor.common.enums.LogLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogSummaryDto {

    private String id;

    private String docId;

    private Instant eventTimestamp;

    private LogLevel logLevel;

    private String environment;

    private String serviceName;

    private String logMessage;
}