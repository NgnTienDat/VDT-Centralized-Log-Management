package com.vdt.log_monitor.query;

import com.vdt.log_monitor.common.entity.LogDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LogSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;
    private static final String INDEX_PATTERN = "sys-logs-*";

    public SearchHits<LogDocument> search(NativeQuery query) {
        return elasticsearchOperations.search(
                query,
                LogDocument.class,
                IndexCoordinates.of(INDEX_PATTERN));
    }
}
