package com.vdt.log_monitor.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogIngestRequest {

    private String traceId;

    /**
     * ERROR | WARN — chỉ 2 level này được gửi từ Logstash theo config.
     * (INFO/DEBUG không đi qua webhook, chỉ lưu thẳng vào ES)
     */
    private String level;
    private String environment;
    private String service;
    private String thread;
    private String logger;
    private String message;
    private Instant timestamp;
    private String hostName;
}