package com.vdt.log_monitor.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Đăng ký endpoint STOMP mà client dùng để kết nối.
     *
     * ws://host/ws          → native WebSocket (dùng với @stomp/stompjs)
     * http://host/ws        → SockJS fallback (polling, SSE) nếu WS bị block
     *
     * allowedOriginPatterns("*") phù hợp cho môi trường dev/test nội bộ.
     * Production nên giới hạn origin cụ thể.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    /**
     * Cấu hình message broker — quyết định ai nhận message gì.
     *
     * enableSimpleBroker("/topic"):
     *   Spring dùng in-memory broker để route message.
     *   /topic  → publish-subscribe (1 server gửi → nhiều client nhận)
     *   Phù hợp cho log streaming và alert broadcasting.
     *
     * setApplicationDestinationPrefixes("/app"):
     *   Message từ client gửi lên server phải có prefix /app.
     *   VD: client SEND tới /app/logs/filter → gọi @MessageMapping("/logs/filter")
     *
     * Sơ đồ flow:
     *   Client SUBSCRIBE "/topic/logs.dev.error"
     *   Server publish  "/topic/logs.dev.error"  → client nhận
     *
     *   Client SEND     "/app/logs/filter"        → @MessageMapping xử lý
     *   Server reply    "/topic/logs.dev.error"   → client nhận
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}