package com.vdt.log_monitor.query;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import com.vdt.log_monitor.common.dto.CursorPage;
import com.vdt.log_monitor.common.dto.LogSearchRequest;
import com.vdt.log_monitor.common.entity.LogDocument;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
                    .field("environment").value(request.getEnvironment())));
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
                    request.getBefore().toEpochMilli(), // Khớp với @timestamp trong ES
                    request.getBeforeId()               // Khớp với _shard_doc tie-breaker
            );
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                // Sort 2 trường: @timestamp DESC + _doc DESC làm tie-breaker
                // Không dùng _id vì ES không bật doc_values cho _id → all shards failed
                .withSort(Sort.by(Sort.Direction.DESC, "@timestamp")
                        .and(Sort.by(Sort.Direction.DESC, "_doc")))
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
                : data.get(data.size() - 1).getId();

        return CursorPage.<LogDocument>builder()
                .data(data)
                .hasMore(hasMore)
                .nextCursor(nextCursorTs)
                .nextCursorId(nextCursorId)
                .build();
    }
}
