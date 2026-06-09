package com.vdt.log_monitor.common.dto;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent được publish mỗi khi Logstash webhook đẩy log mới vào.
 *
 * Flow:
 *   LogCollectorService.ingest()
 *       → applicationEventPublisher.publishEvent(new LogIngestedEvent(this, dto))
 *       → LogWebSocketPublisher.onLogIngested(event)   [lắng nghe event]
 *       → messagingTemplate.convertAndSend(topic, dto) [broadcast qua STOMP]
 *
 * Dùng ApplicationEvent thay vì gọi trực tiếp để giữ collector và websocket
 * layer tách biệt nhau — collector không import gì của websocket package.
 */
@Getter
public class LogIngestedEvent extends ApplicationEvent {

    private final LogMessageDto log;

    public LogIngestedEvent(Object source, LogMessageDto log) {
        super(source);
        this.log = log;
    }
}