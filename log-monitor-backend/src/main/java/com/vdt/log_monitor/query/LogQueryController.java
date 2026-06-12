package com.vdt.log_monitor.query;

import com.vdt.log_monitor.common.dto.ApiResponse;
import com.vdt.log_monitor.common.dto.CursorPage;
import com.vdt.log_monitor.common.dto.LogSearchRequest;
import com.vdt.log_monitor.common.entity.LogDocument;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<CursorPage<LogDocument>> searchLogs(@Valid LogSearchRequest logSearchRequest) {
        CursorPage<LogDocument> results = logQueryService.search(logSearchRequest);
        return ApiResponse.success(results);
    }
}
