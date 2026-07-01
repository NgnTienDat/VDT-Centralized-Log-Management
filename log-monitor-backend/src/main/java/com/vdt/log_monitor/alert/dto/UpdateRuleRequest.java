package com.vdt.log_monitor.alert.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO cho PATCH /api/v1/alerts/rules/{ruleId}.
 * Tất cả field đều nullable — null nghĩa là "giữ nguyên giá trị hiện tại".
 */
@Data
public class UpdateRuleRequest {

    @Size(min = 1, max = 255, message = "phải từ 1 đến 255 ký tự")
    private String name;

    @Min(value = 1, message = "phải >= 1")
    @Max(value = 10080, message = "phải <= 10080 (7 ngày)")
    private Integer intervalMinutes;

    private Boolean isActive;

    @Min(value = 1, message = "phải >= 1")
    @Max(value = 10080, message = "phải <= 10080 (7 ngày)")
    private Integer repeatIntervalMinutes;

    private String triggerStepId;

    // Gửi null = giữ nguyên pipeline cũ
    // Gửi list = thay thế toàn bộ pipeline
    private List<com.vdt.log_monitor.alert.model.PipelineStep> pipelineSteps;

    private com.vdt.log_monitor.alert.model.RuleConfig.NotificationTemplate notificationTemplate;
}