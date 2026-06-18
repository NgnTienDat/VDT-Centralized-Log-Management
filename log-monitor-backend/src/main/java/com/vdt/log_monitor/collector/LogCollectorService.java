package com.vdt.log_monitor.collector;

import com.vdt.log_monitor.common.dto.LogIngestRequest;
import com.vdt.log_monitor.common.dto.LogIngestedEvent;
import com.vdt.log_monitor.common.dto.LogMessageDto;
import com.vdt.log_monitor.common.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Nhận LogIngestRequest từ controller, map sang LogMessageDto,
 * rồi publish LogIngestedEvent vào Spring event bus.
 *
 * Tại sao dùng ApplicationEvent thay vì gọi trực tiếp?
 *
 * Nếu gọi thẳng:
 * collector → websocket (import trực tiếp) — vi phạm feature boundary
 * collector → alert (import trực tiếp) — vi phạm feature boundary
 *
 * Dùng event:
 * collector → publish(LogIngestedEvent) — chỉ biết common/dto
 * websocket ← @EventListener — tự subscribe
 * alert ← @EventListener — tự subscribe
 *
 * ┌─────────────────────┐
 * LogCollectorService │ publishEvent(dto) │
 * └──────────┬──────────┘
 * │ Spring Event Bus
 * ┌───────────┴────────────┐
 * ▼ ▼
 * LogWebSocketPublisher AlertEvaluator
 * (broadcast STOMP) (check rule engine)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogCollectorService {

    private final ApplicationEventPublisher eventPublisher;
    private final LogMapper logMapper;
    /**
     * @Async: chạy trên thread pool riêng. Controller trả 202 ngay,
     *         không chờ method này — không block HTTP thread của Logstash.
     */
    @Async
    public void ingest(LogIngestRequest request) {
        try {
            LogMessageDto dto = logMapper.toDto(request);

            // Publish — tất cả @EventListener với LogIngestedEvent sẽ được gọi:
            // LogWebSocketPublisher.onLogIngested() → broadcast STOMP
            // AlertEvaluator.onLogIngested() → check alert rule
            eventPublisher.publishEvent(new LogIngestedEvent(this, dto));
            log.info("DTO doc ID: {}", dto.getDocId());
//            log.debug("Event published — traceId: {}, level: {}, service: {}",
//                    dto.getTraceId(), dto.getLogLevel(), dto.getServiceName());

        } catch (Exception e) {
            log.error("Failed to process ingested log — traceId: {}, error: {}",
                    request.getTraceId(), e.getMessage(), e);
        }
    }
}