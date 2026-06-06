package com.vdt.log_monitor.common.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * Elasticsearch document entity mapped to the {@code sys-logs-*} index pattern.
 * Represents a single structured log entry ingested by Logstash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "sys-logs", createIndex = false)
public class LogDocument {

    @Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant eventTimestamp;

    @Field(name = "log_timestamp", type = FieldType.Keyword)
    private String logTimestamp;

    @Field(name = "environment", type = FieldType.Keyword)
    private String environment;

    @Field(name = "app_name", type = FieldType.Keyword)
    private String appName;

    // Tên dịch vụ (nếu app_name chưa đủ hoặc muốn phân tách rõ theo chuẩn microservices)
    @Field(name = "service_name", type = FieldType.Keyword)
    private String serviceName;

    // Trace ID dùng để bắt cặp toàn bộ hành trình của 1 request qua các microservices
    @Field(name = "trace_id", type = FieldType.Keyword)
    private String traceId;

    @Field(name = "host_name", type = FieldType.Keyword)
    private String hostName;

    @Field(name = "log_level", type = FieldType.Keyword)
    private String logLevel;

    @Field(name = "logger", type = FieldType.Keyword)
    private String logger;

    @Field(name = "thread", type = FieldType.Keyword)
    private String thread;

    @Field(name = "log_message", type = FieldType.Text, analyzer = "standard")
    private String logMessage;
}