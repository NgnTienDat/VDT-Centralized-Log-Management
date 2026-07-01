package com.vdt.log_monitor.alert.engine;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.FetchDataResult;
import com.vdt.log_monitor.alert.enums.MetricOperator;
import com.vdt.log_monitor.alert.model.PipelineStep;
import com.vdt.log_monitor.alert.enums.ResultType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FetchEsDataExecutor implements ExpressionExecutor {

    private final ElasticsearchClient esClient;

    @Override
    public String getType() {
        return "FETCH_ES_DATA";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExpressionResult execute(PipelineStep step, Map<String, ExpressionResult> context) {
        Map<String, Object> params = step.getParams();
        String indexPattern = (String) params.get("index");
        String rawQuery = (String) params.get("query");
        List<String> groupBy = (List<String>) params.get("groupBy");
        String metricField = (String) params.get("metricField");
        String typeOp = (String) params.getOrDefault("metricType", "COUNT");
        MetricOperator metricOp = MetricOperator.valueOf(typeOp.toUpperCase());

        // 1. Khởi tạo BoolQuery Builder
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 2. Thêm bộ lọc thời gian vào filter (giúp tăng tốc độ cache của ES)
        boolQueryBuilder.filter(f -> f.range(r -> r
                .date(d -> d
                        .field((String) params.getOrDefault("timeField", "@timestamp"))
                        .gte("now-" + params.getOrDefault("lookBackMinutes", 5) + "m")
                        .lte("now")
                )
        ));

        // 3. Thêm truy vấn rawQuery nếu có
        if (rawQuery != null && !rawQuery.trim().isEmpty()) {
            boolQueryBuilder.must(m -> m
                    .queryString(qs -> qs.query(rawQuery))
            );
        }

        // 4. Thực hiện truy vấn ES và xử lý kết quả
        try {
            // Tạo SearchRequest định nghĩa yêu cầu tìm kiếm với index, query và aggregations
            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                    .index(indexPattern)
                    .query(q -> q.bool(boolQueryBuilder.build()));

            // Xây dựng Aggregation cho metricField nếu metricOp không phải là COUNT
            Aggregation metricAgg = metricOp == MetricOperator.COUNT ? null : metricOp.buildAggregation(metricField);


            // Xây dựng Aggregation lồng nhau nếu có groupBy
            if (groupBy != null && !groupBy.isEmpty()) {
                requestBuilder.aggregations("group_by", buildNestedAggregations(groupBy, 0, metricAgg));
                requestBuilder.size(0);
            } else if (metricAgg != null) { // Nếu không có groupBy nhưng metricOp là SUM, MAX, MIN, AVG thì vẫn cần aggregation
                requestBuilder.aggregations("metric_value", metricAgg);
                requestBuilder.size(0);
            }

            // Thực hiện truy vấn ES
            SearchResponse<Void> response = esClient.search(requestBuilder.build(), Void.class);
            Map<String, Double> results = new HashMap<>();

            // 5. Xử lý kết quả trả về từ ES
            if (groupBy != null && !groupBy.isEmpty() && response.aggregations().containsKey("group_by")) {
                parseNestedBuckets("", response.aggregations().get("group_by"), groupBy, 0, metricOp, results);
            } else {
                double val = (metricOp == MetricOperator.COUNT)
                        ? (response.hits().total() != null ? response.hits().total().value() : 0)
                        : metricOp.extractValue(response.aggregations().get("metric_value"));
                results.put("DEFAULT", val);
            }

            // ĐÓNG GÓI CHUẨN HOÁ VÀO FETCH_DATA_RESULT KHÔNG CÒN HARDCODE MAP TRẦN
            FetchDataResult dataResult = FetchDataResult.builder()
                    .groupByFields(groupBy != null ? groupBy : List.of())
                    .metrics(results)
                    .build();
            /*
             * JSON mẫu trả về từ Elasticsearch Aggregation có thể được ánh xạ sang cấu trúc FetchDataResult như sau:
             *  {
             *     "groupByFields": ["service", "status"],
             *     "metrics": {
             *       "auth-service|500": 15.0,
             *       "payment-service|200": 1200.0
             *   }
             * */


            return ExpressionResult.builder()
                    .type(ResultType.METRIC_RESULT)
                    .value(dataResult)
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi thực thi FETCH_ES_DATA tại step {}", step.getId(), e);
            throw new RuntimeException("Lỗi thực thi bước FETCH_ES_DATA", e);
        }
    }

    private Aggregation buildNestedAggregations(List<String> fields, int index, Aggregation metricAgg) {
        String field = fields.get(index);
        return Aggregation.of(a -> a
                .terms(t -> t.field(field).size(100))
                .aggregations(
                        index == fields.size() - 1
                                ? (metricAgg != null ? Map.of("metric_value", metricAgg) : Map.of())
                                : Map.of("sub_group", buildNestedAggregations(fields, index + 1, metricAgg))
                )
        );
    }

    private void parseNestedBuckets(String currentKey, Aggregate aggregate, List<String> fields, int index, MetricOperator metricOp, Map<String, Double> results) {
        StringTermsAggregate termsAgg = aggregate.sterms();
        for (StringTermsBucket bucket : termsAgg.buckets().array()) {
            String bucketKey = bucket.key().stringValue();
            String combinedKey = currentKey.isEmpty() ? bucketKey : currentKey + "|" + bucketKey;

            if (index == fields.size() - 1) {
                double val = (metricOp == MetricOperator.COUNT)
                        ? bucket.docCount()
                        : metricOp.extractValue(bucket.aggregations().get("metric_value"));
                results.put(combinedKey, val);
            } else {
                Aggregate subAggregate = bucket.aggregations().get("sub_group");
                parseNestedBuckets(combinedKey, subAggregate, fields, index + 1, metricOp, results);
            }
        }
    }
}