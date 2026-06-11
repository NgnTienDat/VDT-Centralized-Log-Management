package com.vdt.log_monitor.common.dto;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record CursorPage<T>(
        List<T> data,
        boolean hasMore,
        Instant nextCursor,
        String nextCursorId
) {
}
