package com.vdt.log_monitor.alert.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * Represents a single alert rule definition loaded from {@code rule.json}.
 * <p>
 * Each rule specifies the conditions under which an alert should be triggered,
 * including log level, threshold count, time window, and cooldown period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {
    private String id;
    private AlertRuleType type;
    private boolean enabled;
    /**
     * Environment filter. Use {@code "*"} to match all environments.
     */
    private String environment;
    /**
     * Application filter. Use {@code "*"} to match all applications.
     * Maps to the Elasticsearch field {@code "app"}.
     */
    private String application;
    /**
     * Log level to match (e.g., ERROR, WARN).
     * Maps to the Elasticsearch field {@code "level"}.
     */
    private String level;
    /**
     * Optional keyword for phrase matching against the log message.
     * Maps to the Elasticsearch field {@code "main_message"}.
     * If null or empty, keyword filtering is skipped.
     */
    private String keyword;
    private long threshold;
    private long windowMinutes;
    private long cooldownMinutes;
    private AlertSeverity severity;
}