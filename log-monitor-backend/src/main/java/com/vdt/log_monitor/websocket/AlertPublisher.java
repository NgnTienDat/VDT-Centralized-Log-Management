package com.vdt.log_monitor.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher mỏng, chỉ chịu trách nhiệm gửi AlertNotificationPayload lên
 * WebSocket topic /topic/alerts. KHÔNG chứa logic nghiệp vụ (build message,
 * render template, đọc RuleConfig...) - logic đó thuộc về alert/scheduler.
 * Giữ ranh giới: alert/ biết "nói gì", websocket/ chỉ biết "gửi đi như thế nào".
 * <p>
 * Giả định project đã cấu hình STOMP broker dạng:
 *
 * @Configuration
 * @EnableWebSocketMessageBroker public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
 * public void configureMessageBroker(MessageBrokerRegistry registry) {
 * registry.enableSimpleBroker("/topic");
 * registry.setApplicationDestinationPrefixes("/app");
 * }
 * public void registerStompEndpoints(StompEndpointRegistry registry) {
 * registry.addEndpoint("/ws").withSockJS();
 * }
 * }
 * <p>
 * Nếu chưa có cấu hình này, SimpMessagingTemplate sẽ không được Spring tạo Bean
 * và việc inject vào đây sẽ lỗi khi khởi động ứng dụng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertPublisher {

    private static final String ALERT_TOPIC = "/topic/alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(AlertNotificationPayload payload) {
        messagingTemplate.convertAndSend(ALERT_TOPIC, payload);
        log.info("📤 Đã gửi alert qua WebSocket [topic={}] cho Rule [{}]", ALERT_TOPIC, payload.getRuleName());
    }
}