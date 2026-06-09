package com.vdt.log_monitor.collector;

import com.vdt.log_monitor.common.dto.LogIngestRequest;
import com.vdt.log_monitor.common.dto.LogIngestedEvent;
import com.vdt.log_monitor.common.dto.LogMessageDto;
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
 *   Nếu gọi thẳng:
 *     collector → websocket (import trực tiếp) — vi phạm feature boundary
 *     collector → alert     (import trực tiếp) — vi phạm feature boundary
 *
 *   Dùng event:
 *     collector → publish(LogIngestedEvent)    — chỉ biết common/dto
 *     websocket ← @EventListener               — tự subscribe
 *     alert     ← @EventListener               — tự subscribe
 *
 *                     ┌─────────────────────┐
 *   LogCollectorService │  publishEvent(dto)  │
 *                     └──────────┬──────────┘
 *                                │  Spring Event Bus
 *                    ┌───────────┴────────────┐
 *                    ▼                        ▼
 *        LogWebSocketPublisher          AlertEvaluator
 *        (broadcast STOMP)              (check rule engine)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogCollectorService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * @Async: chạy trên thread pool riêng. Controller trả 202 ngay,
     * không chờ method này — không block HTTP thread của Logstash.
     */
    @Async
    public void ingest(LogIngestRequest request) {
        try {
            LogMessageDto dto = mapToDto(request);

            // Publish — tất cả @EventListener với LogIngestedEvent sẽ được gọi:
            //   LogWebSocketPublisher.onLogIngested()  → broadcast STOMP
            //   AlertEvaluator.onLogIngested()         → check alert rule
            eventPublisher.publishEvent(new LogIngestedEvent(this, dto));

            log.debug("Event published — traceId: {}, level: {}, service: {}",
                    dto.getTraceId(), dto.getLevel(), dto.getService());

        } catch (Exception e) {
            log.error("Failed to process ingested log — traceId: {}, error: {}",
                    request.getTraceId(), e.getMessage(), e);
        }
    }

    private LogMessageDto mapToDto(LogIngestRequest req) {
        return LogMessageDto.builder()
                .traceId(req.getTraceId())
                .level(req.getLevel())
                .environment(normalizeEnv(req.getEnvironment()))
                .service(req.getService())
                .thread(req.getThread())
                .message(req.getMessage())
                .timestamp(req.getTimestamp())
                .build();
    }


    private String normalizeEnv(String env) {
        return env != null ? env.toLowerCase().trim() : "unknown";
    }
}