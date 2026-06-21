package com.vdt.log_monitor.alert.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
/**
 * Represents an alert event generated when a rule's threshold is exceeded.
 * This payload is published to the frontend via WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertEvent {
    private String ruleId;
    private String title;
    private String message;
    private String environment;
    private String application;
    private String severity;
    private long matchedCount;
    private Instant triggeredAt;
}
