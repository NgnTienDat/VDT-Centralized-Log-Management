package com.vdt.log_monitor.alert.service;

import com.vdt.log_monitor.alert.dto.AlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes {@link AlertEvent} payloads to the frontend via WebSocket STOMP.
 * <p>
 * Alerts are broadcast to all subscribers on {@code /topic/alerts}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertWebSocketPublisher {

    private static final String ALERT_DESTINATION = "/topic/alerts";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publishes the given alert event to all WebSocket subscribers.
     *
     * @param event the alert event to broadcast
     */
    public void publish(AlertEvent event) {
        messagingTemplate.convertAndSend(ALERT_DESTINATION, event);
        log.info("Published alert to {}: rule=[{}], severity={}, matchedCount={}",
                ALERT_DESTINATION, event.getRuleId(), event.getSeverity(), event.getMatchedCount());
    }
}
