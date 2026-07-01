package com.vdt.log_monitor.alert.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.Map;

@Document(indexName = "alert-notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotificationDocument {
    @Id
    private String id; // ID tự sinh của Elasticsearch

    @Field(type = FieldType.Keyword)
    private String ruleId;

    @Field(type = FieldType.Keyword)
    private String ruleName;

    @Field(type = FieldType.Keyword)
    private String triggerStepId;

    @Field(type = FieldType.Keyword)
    private String alertState; // OK, FIRING

    @Field(type = FieldType.Boolean)
    private boolean triggered;

    @Field(type = FieldType.Keyword)
    private List<String> breachedGroups;

    // Object type cho phép lưu trữ cấu trúc Map động một cách linh hoạt
    @Field(type = FieldType.Object)
    private Map<String, Double> breachedGroupValues;

    @Field(type = FieldType.Keyword)
    private List<String> groupByFields;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String message;

    @Field(type = FieldType.Long)
    private long timestamp;
}