package com.vdt.log_monitor.query;


import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import com.vdt.log_monitor.common.dto.LogSearchRequest;
import com.vdt.log_monitor.common.entity.LogDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LogQueryService {

    private final LogSearchRepository logSearchRepository;

    public Page<LogDocument> search(LogSearchRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(),
                Sort.by(Sort.Direction.DESC, "@timestamp"));

        var boolQueryBuilder = new BoolQuery.Builder();

        if (StringUtils.hasText(request.getEnvironment())) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("environment")
                    .value(request.getEnvironment())));
        }

        if (StringUtils.hasText(request.getAppName())) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("app_name")
                    .value(request.getAppName())));
        }

        if (StringUtils.hasText(request.getLogLevel())) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("log_level")
                    .value(request.getLogLevel().toUpperCase())));
        }

        if (request.getFrom() != null || request.getTo() != null) {
            boolQueryBuilder.filter(f -> f.range(r -> r
                    .date(d -> {
                        d.field("@timestamp");
                        if (request.getFrom() != null) {
                            d.gte(request.getFrom().toString());
                        }
                        if (request.getTo() != null) {
                            d.lte(request.getTo().toString());
                        }
                        return d;
                    })));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(pageable)
                .build();

        SearchHits<LogDocument> hits = logSearchRepository.search(query);
        return toPage(hits, pageable);
    }

    public Page<LogDocument> searchByKeyword(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "@timestamp"));

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.match(m -> m
                        .field("log_message")
                        .query(keyword)))
                .withTrackTotalHits(true)
                .withPageable(pageable)
                .build();

        SearchHits<LogDocument> hits = logSearchRepository.search(query);
        return toPage(hits, pageable);
    }

    private Page<LogDocument> toPage(SearchHits<LogDocument> hits, Pageable pageable) {
        List<LogDocument> content = hits.stream().map(SearchHit::getContent).toList();
        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }
}
