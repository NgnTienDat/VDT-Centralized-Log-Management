package com.vdt.log_monitor.websocket;

import com.vdt.log_monitor.common.dto.LogIngestedEvent;
import com.vdt.log_monitor.common.dto.LogMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe LogIngestedEvent và broadcast log ra các STOMP topic.
 *
 * Topic naming convention:
 *   /topic/logs.{env}.{level}   → filter cụ thể, VD: /topic/logs.dev.error
 *   /topic/logs.{env}.all       → tất cả level của 1 env
 *   /topic/logs.all             → tất cả log, không phân biệt env/level
 *
 * Client subscribe topic nào thì chỉ nhận log khớp topic đó.
 * Server không cần biết client đang filter gì — routing do STOMP broker lo.
 *
 * Ví dụ:
 *   Client A subscribe /topic/logs.dev.error  → chỉ nhận ERROR của DEV
 *   Client B subscribe /topic/logs.all        → nhận tất cả
 *   Cùng 1 log ERROR từ DEV → Client A nhận, Client B cũng nhận
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void onLogIngested(LogIngestedEvent event) {
        LogMessageDto log = event.getLog();

        // Chuẩn hóa để tránh NullPointerException khi build topic string
        String env   = normalize(log.getEnvironment());
        String service = normalize(log.getServiceName());
        String level = normalize(log.getLogLevel().name());

        System.out.println("ENV: " + env + ", SERVICE: " + service + ", LEVEL: " + level);

        // 1. Topic cụ thể nhất — client filter đúng env + service + level
        //    VD: /topic/logs.dev.logs-service.error
        String specificTopic = buildTopic(env, service, level);
        sendToTopic(specificTopic, log);

        // 2. Topic theo env, tất cả level
        //    VD: /topic/logs.dev.all
        //    Hữu ích khi client muốn xem tất cả log của DEV bất kể level
        String envAllTopic = buildTopic(env, service, "all");
        sendToTopic(envAllTopic, log);

        // 3. Topic tổng — tất cả env, tất cả level
        //    /topic/logs.all
        //    Hữu ích cho dashboard tổng quan
        sendToTopic("/topic/logs.all", log);
    }

    private void sendToTopic(String topic, LogMessageDto logMessageDto) {
        try {
            messagingTemplate.convertAndSend(topic, logMessageDto);
            log.debug("Published log [{}] [{}] to {}", logMessageDto.getLogLevel(), logMessageDto.getServiceName(), topic);

        } catch (Exception e) {
            // Không để lỗi WS làm crash luồng ingest chính
            log.error("Failed to publish log to topic {}: {}", topic, e.getMessage());
        }
    }


    private String buildTopic(String env, String service, String level) {
        return "/topic/logs." + env + "." + service + "." + level;
    }

    private String normalize(String value) {
        return value != null ? value.toLowerCase() : "unknown";
    }
}