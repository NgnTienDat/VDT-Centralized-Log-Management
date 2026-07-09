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

    /** ES document _id — dùng cho GET /api/v1/logs/{id} */
    private String id;

    /** doc_id từ Logstash — tie-breaker cho cursor pagination */
    private String docId;

    private Instant eventTimestamp;

    private LogLevel logLevel;

    private String environment;

    private String serviceName;

    /** Nội dung log chính, đã tách stack trace */
    private String logMessage;
}