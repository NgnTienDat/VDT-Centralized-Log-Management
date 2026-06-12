package com.vdt.log_monitor.collector;

import com.vdt.log_monitor.common.dto.LogIngestRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint nhận log từ Logstash HTTP output.
 *
 * URL: POST /internal/logs/ingest
 *
 * "internal" prefix ngụ ý endpoint này không dành cho browser hay mobile —
 * chỉ Logstash trong cùng Docker network gọi vào.
 * Production: giới hạn bằng firewall rule hoặc Spring Security IP whitelist.
 *
 * Flow sau khi nhận request:
 * Controller (validate) → Service (map + publish event) → [Event Bus]
 * → LogWebSocketPublisher (broadcast STOMP)
 * → AlertEvaluator (check rule engine)
 */
@Slf4j
@RestController
@RequestMapping("/internal/logs")
@RequiredArgsConstructor
public class LogCollectorController {

    private final LogCollectorService logCollectorService;

    /**
     * Logstash gọi endpoint này mỗi khi có log ERROR/WARN mới.
     *
     * Trả về nhanh nhất có thể — Logstash có timeout mặc định 60s,
     * nhưng response chậm sẽ làm pipeline Logstash bị block.
     *
     * Toàn bộ xử lý nặng (publish event, WebSocket broadcast) chạy async
     * ở LogCollectorService → không làm chậm response này.
     */
    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@RequestBody @Valid LogIngestRequest request) {
        if (request.getLogLevel().name().equalsIgnoreCase("WARN")) {
            log.warn("Received log — service: {}, level: {}, traceId: {}",
                    request.getServiceName(), request.getLogLevel(), request.getTraceId());
        } else if (request.getLogLevel().name().equalsIgnoreCase("ERROR")) {
            log.error("Received log — service: {}, level: {}, traceId: {}",
                    request.getServiceName(), request.getLogLevel(), request.getTraceId());
        }
         logCollectorService.ingest(request);

        // 202 Accepted: đã nhận, đang xử lý async
        // Không dùng 200 OK vì chưa xử lý xong tại thời điểm response
        return ResponseEntity.accepted().build();
    }
}