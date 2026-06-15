package com.vdt.log_monitor.query;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import com.vdt.log_monitor.common.dto.CursorPage;
import com.vdt.log_monitor.common.dto.LogSearchRequest;
import com.vdt.log_monitor.common.entity.LogDocument;

import java.time.Instant;

import com.vdt.log_monitor.common.exception.AppException;
import com.vdt.log_monitor.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LogQueryService {

    private final LogSearchRepository logSearchRepository;

    public CursorPage<LogDocument> search(LogSearchRequest request) {

        int limit = request.getSize() != null ? request.getSize() : 20;

        var boolQueryBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(request.getEnvironment())) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("environment").value(request.getEnvironment().toLowerCase())));
        }
        if (StringUtils.hasText(request.getAppName())) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("app").value(request.getAppName())));
        }
        if (StringUtils.hasText(request.getServiceName())) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("service").value(request.getServiceName())));
        }
        if (request.getLogLevel() != null) {
            boolQueryBuilder.filter(f -> f.term(t -> t
                    .field("level").value(request.getLogLevel().name())));
        }
        if (StringUtils.hasText(request.getQ())) {
            boolQueryBuilder.must(m -> m.bool(b -> b
                    .should(s -> s.multiMatch(mm -> mm
                            .query(request.getQ())
                            .fields("main_message", "stack_trace", "logger")))
                    .should(s -> s.term(t -> t
                            .field("trace_id").value(request.getQ())))
                    .minimumShouldMatch("1")));
        }

        // Dùng search_after thay vì range filter cho cursor
        List<Object> searchAfter = null;

        // Chỉ kích hoạt search_after từ trang 2 trở đi khi có ĐỦ cả timestamp và ID tie-break
        if (request.getBefore() != null && StringUtils.hasText(request.getBeforeId())) {
            searchAfter = List.of(
                    request.getBefore().toEpochMilli(),
                    request.getBeforeId()   // beforeId giờ là docId (keyword), không phải _shard_doc
            );
        }

        // Sort đúng: @timestamp DESC + doc_id DESC (field thường, có doc_values)
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withSort(s -> s.field(f -> f
                        .field("@timestamp")
                        .order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f
                        .field("doc_id")
                        .order(SortOrder.Desc)))
                .withSearchAfter(searchAfter)
                .withPageable(PageRequest.of(0, limit + 1))
                .build();

        SearchHits<LogDocument> hits = logSearchRepository.search(query);
        List<LogDocument> allHits = hits.stream()
                .map(SearchHit::getContent)
                .toList();

        boolean hasMore = allHits.size() > limit;
        List<LogDocument> data = new ArrayList<>(
                allHits.subList(0, Math.min(allHits.size(), limit)));

        Instant nextCursorTs = data.isEmpty() ? null
                : data.get(data.size() - 1).getEventTimestamp();

        String nextCursorId = data.isEmpty() ? null
                : data.get(data.size() - 1).getDocId();

        return CursorPage.<LogDocument>builder()
                .data(data)
                .hasMore(hasMore)
                .nextCursor(nextCursorTs)
                .nextCursorId(nextCursorId)
                .build();
    }

    public LogDocument findById(String id) {
        return logSearchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_NOT_FOUND));
    }

    /**
     * Fetch by the doc_id keyword field.
     * Throws ResourceNotFoundException (→ 404) when not found.
     */
    public LogDocument findByDocId(String docId) {
        return logSearchRepository.findByDocId(docId)
                .orElseThrow(() -> new AppException(ErrorCode.LOG_NOT_FOUND));
    }
}
