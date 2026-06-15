package com.vdt.log_monitor.query;

import com.vdt.log_monitor.common.entity.LogDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LogSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;
    private static final String INDEX_PATTERN = "sys-logs-*";
    private static final IndexCoordinates INDEX = IndexCoordinates.of(INDEX_PATTERN);

    public SearchHits<LogDocument> search(NativeQuery query) {
        return elasticsearchOperations.search(
                query,
                LogDocument.class,
                IndexCoordinates.of(INDEX_PATTERN));
    }

    /**
     * Fetch by Elasticsearch _id using an ids query.
     *
     * elasticsearchOperations.get() does not support wildcard index patterns
     * (it calls GET /{index}/_doc/{id} which requires an exact index name).
     * Using a search with an ids query works correctly across sys-logs-*.
     */
    public Optional<LogDocument> findById(String id) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.ids(i -> i.values(id)))
                .withMaxResults(1)
                .build();

        SearchHits<LogDocument> hits = elasticsearchOperations.search(
                query, LogDocument.class, INDEX);

        return hits.isEmpty()
                ? Optional.empty()
                : Optional.of(hits.getSearchHit(0).getContent());
    }

    /**
     * Fetch by the doc_id keyword field (term query across all shards).
     * Use this when the caller has a doc_id value but not the raw ES _id.
     */
    public Optional<LogDocument> findByDocId(String docId) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.term(t -> t.field("doc_id").value(docId)))
                .withMaxResults(1)
                .build();

        SearchHits<LogDocument> hits = elasticsearchOperations.search(
                query, LogDocument.class, INDEX);

        return hits.isEmpty()
                ? Optional.empty()
                : Optional.of(hits.getSearchHit(0).getContent());
    }
}
