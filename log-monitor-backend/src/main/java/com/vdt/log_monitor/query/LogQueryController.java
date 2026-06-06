package com.vdt.log_monitor.query;

import com.vdt.log_monitor.common.dto.ApiResponse;
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
public class LogQueryController {

    private final LogQueryService logQueryService;

    /**
     * /api/v1/logs?environment=prod&appName=myapp&logLevel=ERROR&from=2026-06-04T00:00:00Z&to=2026-06-05T23:59:59Z&page=0&size=20
     */
    @GetMapping
    public ApiResponse<Page<LogDocument>> searchLogs(@Valid LogSearchRequest logSearchRequest) {

//        LogSearchRequest logSearchRequest = LogSearchRequest.builder()
//                .environment(environment)
//                .appName(appName)
//                .logLevel(logLevel)
//                .from(from != null ? Instant.parse(from) : null)
//                .to(to != null ? Instant.parse(to) : null)
//                .page(page)
//                .size(size)
//                .build();

        Page<LogDocument> results = logQueryService.search(logSearchRequest);
        return ApiResponse.success(results);
    }

    /**
     * Full-text search on log message content.
     * Always sorted by @timestamp descending (newest first).
     *
     * @param keyword the search keyword
     * @param page    page number (0-indexed, default 0)
     * @param size    page size (default 20)
     * @return paginated log results matching the keyword, wrapped in ApiResponse
     */
    @GetMapping("/search")
    public ApiResponse<Page<LogDocument>> searchByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<LogDocument> results = logQueryService.searchByKeyword(keyword, page, size);
        return ApiResponse.success(results);
    }
}
