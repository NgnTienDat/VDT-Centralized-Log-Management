package com.vdt.log_monitor.query;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.vdt.log_monitor.common.dto.CursorPage;
import com.vdt.log_monitor.common.dto.LogMessageDto;
import com.vdt.log_monitor.common.dto.LogSearchRequest;
import com.vdt.log_monitor.common.dto.LogSummaryDto;
import com.vdt.log_monitor.common.entity.LogDocument;
import com.vdt.log_monitor.common.exception.AppException;
import com.vdt.log_monitor.common.exception.ErrorCode;
import com.vdt.log_monitor.common.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LogQueryService {

        private final LogSearchRepository logSearchRepository;
        private final LogMapper logMapper;

        /**
         * Search log với cursor-based pagination.
         *
         * Return type thay đổi: CursorPage<LogMessageDto> → CursorPage<LogSummaryDto>
         *
         * Lý do: list view chỉ cần 6 field (id, docId, eventTimestamp, logLevel,
         * environment, serviceName, logMessage). Trả LogMessageDto đầy đủ 14 field
         * là thừa, tốn bandwidth và tạo kỳ vọng sai cho client rằng mọi field đều có.
         *
         * Detail đầy đủ → findById() / findByDocId() vẫn trả LogMessageDto.
         */
        public CursorPage<LogSummaryDto> search(LogSearchRequest request) {
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
                                        .should(s -> s.term(t -> t
                                                        .field("host_name").value(request.getQ())))
                                        .minimumShouldMatch("1")));
                }

                List<Object> searchAfter = null;
                if (request.getBefore() != null && StringUtils.hasText(request.getBeforeId())) {
                        searchAfter = List.of(
                                        request.getBefore().toEpochMilli(),
                                        request.getBeforeId());
                }

                NativeQuery query = NativeQuery.builder()
                                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                                .withSourceFilter(new FetchSourceFilter(null, null, new String[] { "stack_trace" }))
                                // .withSourceFilter(new FetchSourceFilter(
                                //                 null, // includes: null = lấy tất cả field
                                //                 new String[] { "stack_trace" }, // excludes: chỉ bỏ field nặng nhất
                                //                 null // sourceExcludes: không cần
                                // ))
                                .withSort(s -> s.field(f -> f.field("@timestamp").order(SortOrder.Desc)))
                                .withSort(s -> s.field(f -> f.field("doc_id").order(SortOrder.Desc)))
                                .withSearchAfter(searchAfter)
                                .withPageable(PageRequest.of(0, limit + 1))
                                .build();

                SearchHits<LogDocument> hits = logSearchRepository.search(query);
                List<LogDocument> allHits = hits.stream()
                                .map(SearchHit::getContent)
                                .toList();

                boolean hasMore = allHits.size() > limit;

                // Map sang LogSummaryDto thay vì LogMessageDto
                List<LogSummaryDto> data = allHits
                                .subList(0, Math.min(allHits.size(), limit))
                                .stream()
                                .map(logMapper::toSummaryDto)
                                .toList();

                Instant nextCursorTs = data.isEmpty() ? null
                                : data.get(data.size() - 1).getEventTimestamp();

                String nextCursorId = data.isEmpty() ? null
                                : data.get(data.size() - 1).getDocId();

                return CursorPage.<LogSummaryDto>builder()
                                .data(data)
                                .hasMore(hasMore)
                                .nextCursor(nextCursorTs)
                                .nextCursorId(nextCursorId)
                                .build();
        }

        /** Full detail — dùng khi user click vào row trong UI */
        public LogMessageDto findById(String id) {
                return logMapper.toDto(logSearchRepository.findById(id)
                                .orElseThrow(() -> new AppException(ErrorCode.LOG_NOT_FOUND)));
        }

        public LogMessageDto findByDocId(String docId) {
                return logMapper.toDto(logSearchRepository.findByDocId(docId)
                                .orElseThrow(() -> new AppException(ErrorCode.LOG_NOT_FOUND)));
        }

        public List<String> getUniqueServicesOrApps(String fieldName) {
                NativeQuery query = NativeQuery.builder()
                                .withMaxResults(0)
                                .withAggregation("results",
                                                Aggregation.of(a -> a.terms(t -> t.field(fieldName).size(100))))
                                .build();

                SearchHits<LogDocument> searchHits = logSearchRepository.search(query);
                ElasticsearchAggregations aggregations = (ElasticsearchAggregations) searchHits.getAggregations();

                return aggregations.get("results")
                                .aggregation()
                                .getAggregate()
                                .sterms()
                                .buckets()
                                .array()
                                .stream()
                                .map(bucket -> bucket.key().stringValue())
                                .toList();
        }
}