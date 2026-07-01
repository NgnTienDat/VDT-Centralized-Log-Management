package com.vdt.log_monitor.alert.model;

import com.vdt.log_monitor.alert.enums.AlertState;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Document(indexName = "alert-rules")
public class RuleConfig {
    @Id
    private String ruleId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Integer)
    private Integer intervalMinutes;

    @Field(type = FieldType.Boolean)
    private Boolean isActive;

    @Field(type = FieldType.Long)
    private Long lastRunTime;

    @Field(type = FieldType.Keyword)
    private AlertState alertState; // OK hoặc FIRING

    @Field(type = FieldType.Long)
    private Long lastNotifiedTime;

    @Field(type = FieldType.Integer)
    private Integer repeatIntervalMinutes;

    // Danh sách group (breachedGroups) đã được gửi thông báo ở lần gần nhất.
    // Dùng để phát hiện "group mới breach" trong lúc đang FIRING + còn trong cooldown,
    // để bỏ qua cooldown và báo ngay thay vì chờ tới khi repeatIntervalMinutes hết hạn.
    // Reset về rỗng mỗi khi Rule chuyển về trạng thái OK (xem AlertSchedulerManager).
    @Field(type = FieldType.Keyword)
    private List<String> lastNotifiedBreachedGroups;

    @Field(type = FieldType.Keyword)
    private String triggerStepId;

    @Field(type = FieldType.Nested)
    private List<PipelineStep> pipelineSteps;

    @Field(type = FieldType.Object)
    private NotificationTemplate notificationTemplate;

    @Data
    public static class NotificationTemplate {
        @Field(type = FieldType.Text)
        private String title;

        @Field(type = FieldType.Text)
        private String message;
    }
}