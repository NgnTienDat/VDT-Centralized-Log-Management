package com.vdt.log_monitor.alert.repository;
import com.vdt.log_monitor.alert.dto.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
/**
 * Queries Elasticsearch using the Count API to determine the number of
 * log entries matching a given {@link AlertRule}'s criteria within its
 * sliding time window.
 * <p>
 * <strong>ES Field Mapping (from {@code LogDocument}):</strong>
 * <ul>
 *   <li>{@code @timestamp} — event timestamp (Date)</li>
 *   <li>{@code environment} — environment keyword</li>
 *   <li>{@code app} — application name keyword</li>
 *   <li>{@code level} — log level keyword</li>
 *   <li>{@code main_message} — log message (Text, analyzed)</li>
 * </ul>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class AlertQueryRepository {
    private static final String INDEX_NAME = "sys-logs-*";
    private final ElasticsearchOperations elasticsearchOperations;
    /**
     * Counts log documents in Elasticsearch matching the given rule's filters
     * within the sliding time window ({@code now - windowMinutes}).
     * <p>
     * Uses the Count API — no documents are fetched.
     *
     * @param rule the alert rule defining the query filters
     * @return the count of matching log documents
     */
    public long countMatchingLogs(AlertRule rule, Instant windowStart) {
//        Instant windowStart = Instant.now().minus(rule.getWindowMinutes(), ChronoUnit.MINUTES);
        BoolQuery.Builder boolBuilder = QueryBuilders.bool();
        // Timestamp sliding window: @timestamp >= windowStart
        boolBuilder.filter(f -> f.range(r -> r
                .date(d -> d
                        .field("@timestamp")
                        .gte(windowStart.toString())
                )
        ));
        // Level: exact match (always required)
        boolBuilder.filter(f -> f.term(t -> t
                .field("level")
                .value(rule.getLevel())
        ));
        // Environment: exact match (skip if wildcard "*")
        if (rule.getEnvironment() != null && !"*".equals(rule.getEnvironment())) {
            boolBuilder.filter(f -> f.term(t -> t
                    .field("environment")
                    .value(rule.getEnvironment())
            ));
        }
        // Application: exact match on ES field "app" (skip if wildcard "*")
        if (rule.getApplication() != null && !"*".equals(rule.getApplication())) {
            boolBuilder.filter(f -> f.term(t -> t
                    .field("app")
                    .value(rule.getApplication())
            ));
        }
        // Keyword: match phrase on ES field "main_message" (skip if null/empty)
        if (rule.getKeyword() != null && !rule.getKeyword().isBlank()) {
            boolBuilder.must(m -> m.matchPhrase(mp -> mp
                    .field("main_message")
                    .query(rule.getKeyword())
            ));
        }
        Query query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolBuilder.build()))
                .build();
        long count = elasticsearchOperations.count(query, IndexCoordinates.of(INDEX_NAME));
        log.debug("Rule [{}]: counted {} matching logs in the last {} minutes",
                rule.getId(), count, rule.getWindowMinutes());
        return count;
    }
}