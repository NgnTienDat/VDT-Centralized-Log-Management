package com.vdt.log_monitor.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Theo dõi vòng đời STOMP session để:
 *   1. Log audit trail — ai connect/disconnect lúc nào
 *   2. Đếm số client đang subscribe từng topic (hữu ích cho monitoring)
 *   3. Phát hiện client disconnect bất thường (có thể trigger alert)
 *
 * Không dùng để lưu filter của client — STOMP broker tự quản lý subscription.
 * Class này chỉ observes, không điều khiển routing.
 */
@Slf4j
@Component
public class WebSocketSessionListener {

    /**
     * Map topic → số client đang subscribe.
     * ConcurrentHashMap vì nhiều thread có thể cập nhật đồng thời.
     * Key: "/topic/logs.dev.error", Value: AtomicInteger (thread-safe counter)
     */
    private final Map<String, AtomicInteger> topicSubscriberCount = new ConcurrentHashMap<>();

    /**
     * Tổng số session đang kết nối — dùng cho health check endpoint.
     */
    private final AtomicInteger activeSessionCount = new AtomicInteger(0);

    // Thêm map này để track session nào đang subscribe topic nào
    private final Map<String, Set<String>> sessionTopics = new ConcurrentHashMap<>();

    @EventListener
    public void onSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId   = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination != null) {
            // Track topic theo session
            sessionTopics.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                    .add(destination);

            int count = topicSubscriberCount
                    .computeIfAbsent(destination, k -> new AtomicInteger(0))
                    .incrementAndGet();
            log.info("[WS] Subscribe — session: {}, topic: {}, subscribers: {}", sessionId, destination, count);
        }
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        int total = activeSessionCount.decrementAndGet();
        log.info("[WS] Client disconnected — sessionId: {}, total active: {}", sessionId, total);

        // Giảm counter của tất cả topic mà session này đang subscribe
        Set<String> topics = sessionTopics.remove(sessionId);
        if (topics != null) {
            topics.forEach(topic -> {
                AtomicInteger counter = topicSubscriberCount.get(topic);
                if (counter != null) {
                    int count = counter.decrementAndGet();
                    log.info("[WS] Auto-unsubscribe on disconnect — session: {}, topic: {}, subscribers: {}",
                            sessionId, topic, count);
                }
            });
        }
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        int total = activeSessionCount.incrementAndGet();
        log.info("[WS] Client connected — sessionId: {}, total active: {}", sessionId, total);
    }

//    @EventListener
//    public void onSessionDisconnect(SessionDisconnectEvent event) {
//        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
//        String sessionId = accessor.getSessionId();
//        int total = activeSessionCount.decrementAndGet();
//        log.info("[WS] Client disconnected — sessionId: {}, total active: {}", sessionId, total);
//    }

    /**
     * Ghi lại khi client subscribe vào 1 topic.
     * Destination VD: "/topic/logs.dev.error"
     */
//    @EventListener
//    public void onSessionSubscribe(SessionSubscribeEvent event) {
//        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
//        String sessionId   = accessor.getSessionId();
//        String destination = accessor.getDestination();
//
//        if (destination != null) {
//            int count = topicSubscriberCount
//                    .computeIfAbsent(destination, k -> new AtomicInteger(0))
//                    .incrementAndGet();
//            log.info("[WS] Subscribe — session: {}, topic: {}, subscribers: {}", sessionId, destination, count);
//        }
//    }

    /**
     * Ghi lại khi client unsubscribe (đổi filter hoặc đóng tab).
     */
    @EventListener
    public void onSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId   = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination != null) {
            // 1. Xóa khỏi map track của session để tránh bị trừ lần 2 khi disconnect
            Set<String> topics = sessionTopics.get(sessionId);
            if (topics != null) {
                topics.remove(destination);
                if (topics.isEmpty()) {
                    sessionTopics.remove(sessionId);
                }
            }

            // 2. Trừ biến đếm
            AtomicInteger counter = topicSubscriberCount.get(destination);
            if (counter != null) {
                int count = counter.decrementAndGet();
                log.info("[WS] Unsubscribe — session: {}, topic: {}, subscribers: {}", sessionId, destination, count);
            }
        }
    }

    /**
     * Expose cho health check hoặc /api/stats endpoint.
     */
    public int getActiveSessionCount() {
        return activeSessionCount.get();
    }

    public Map<String, Integer> getTopicSubscriberSnapshot() {
        Map<String, Integer> snapshot = new ConcurrentHashMap<>();
        topicSubscriberCount.forEach((topic, count) -> snapshot.put(topic, count.get()));
        return snapshot;
    }
}