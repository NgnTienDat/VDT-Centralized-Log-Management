package com.vdt.log_monitor.websocket;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import org.springframework.stereotype.Controller;

/**
 * Xử lý message từ client gửi lên qua STOMP.
 *
 * Trong kiến trúc STOMP của hệ thống này, client KHÔNG cần gửi message
 * lên server để đăng ký filter — client tự subscribe đúng topic là xong.
 *
 * Class này xử lý 2 trường hợp phụ:
 *   1. Client ping để check connection còn sống
 *   2. Client yêu cầu replay N log gần nhất sau khi reconnect
 *      (tránh mất log trong khoảng thời gian mất kết nối)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LogWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Client gửi: SEND /app/logs/ping
     * Server reply về đúng session đó (không broadcast).
     *
     * Dùng để client biết kết nối còn hoạt động trước khi subscribe.
     *
     * headerAccessor.getSessionId(): lấy STOMP session ID của người gửi,
     * dùng để reply về đúng client (convertAndSendToUser).
     */
    @MessageMapping("/logs/ping")
    public void handlePing(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.debug("Ping from session: {}", sessionId);

        // Reply về đúng session, không broadcast ra tất cả
        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/pong",   // /user/{sessionId}/queue/pong
                new PongResponse("pong", System.currentTimeMillis()),
                buildSessionHeader(sessionId)
        );
    }

    /**
     * Client gửi sau khi reconnect để lấy lại log bị miss.
     *
     * Payload: { "env": "dev", "level": "error", "lastReceivedId": "abc123" }
     * Server tìm trong ES các log sau lastReceivedId và push lại cho client đó.
     *
     * Hiện tại chỉ log request — implementation thực tế cần inject
     * LogQueryService để query ES và push lại.
     */
    @MessageMapping("/logs/replay")
    public void handleReplay(
            @Payload ReplayRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        log.info("Replay request from session {} — env: {}, level: {}, after: {}",
                sessionId, request.getEnv(), request.getLevel(), request.getLastReceivedId());

        // TODO: inject LogQueryService, query ES với filter + after lastReceivedId
        // List<LogMessageDto> missed = logQueryService.findAfter(request);
        // missed.forEach(log -> messagingTemplate.convertAndSendToUser(sessionId, "/queue/replay", log, ...));
    }

    /**
     * Spring STOMP cần header "simpSessionId" để route convertAndSendToUser
     * về đúng session thay vì dùng username-based routing.
     */
    private java.util.Map<String, Object> buildSessionHeader(String sessionId) {
        java.util.Map<String, Object> headers = new java.util.HashMap<>();
        headers.put(org.springframework.messaging.simp.SimpMessageHeaderAccessor.SESSION_ID_HEADER, sessionId);
        return headers;
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    @Data
    public static class ReplayRequest {
        private String env;
        private String level;
        private String lastReceivedId; // ES document _id của log cuối client đã nhận
    }

    @Data
    @RequiredArgsConstructor
    public static class PongResponse {
        private final String status;
        private final long serverTime;
    }
}