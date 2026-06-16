package com.vdt.log_monitor.query;

import co.elastic.clients.elasticsearch._types.FieldValue;
import com.vdt.log_monitor.common.dto.ApiResponse;
import com.vdt.log_monitor.common.dto.CursorPage;
import com.vdt.log_monitor.common.dto.LogMessageDto;
import com.vdt.log_monitor.common.dto.LogSearchRequest;
import com.vdt.log_monitor.common.entity.LogDocument;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class LogQueryController {

    private final LogQueryService logQueryService;

    /**
     * keyword, environment, appName, service, logLevel, before, size
     * /api/v1/logs?environment=prod&appName=myapp&logLevel=ERROR&before=2026-06-04T00:00:00Z&size=20&q=abc
     */
    @GetMapping
    public ApiResponse<CursorPage<LogMessageDto>> searchLogs(@Valid LogSearchRequest logSearchRequest) {
//        System.out.println("Search request: " + logSearchRequest);
        CursorPage<LogMessageDto> results = logQueryService.search(logSearchRequest);
//        System.out.println("Search results: " + results);
        return ApiResponse.success(results);
    }

    /**
     * Get a single log by its Elasticsearch document _id or by doc_id field.
     *
     * Priority: tries _id first (exact ES document lookup), then falls back
     * to a doc_id term query — so both of these work:
     *   GET /api/v1/logs/01HXYZ...              (ES _id)
     *   GET /api/v1/logs/01HXYZ...?by=doc_id    (doc_id field)
     *
     * @param id   the identifier value
     * @param by   optional hint: "id" (default) | "doc_id"
     */
    @GetMapping("/{id}")
    public ApiResponse<LogMessageDto> getLogById(
            @PathVariable String id,
            @RequestParam(name = "by", defaultValue = "id") String by) {

        LogMessageDto doc = "doc_id".equalsIgnoreCase(by)
                ? logQueryService.findByDocId(id)
                : logQueryService.findById(id);

        return ApiResponse.success(doc);
    }

    @GetMapping("/services")
    public ApiResponse<List<String>> getServices() {
        return ApiResponse.success(
                logQueryService.getUniqueServices()
        );
    }
}
