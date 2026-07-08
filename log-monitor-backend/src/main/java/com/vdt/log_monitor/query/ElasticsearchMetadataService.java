package com.vdt.log_monitor.query;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.FieldCapsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ElasticsearchMetadataService {

    private final ElasticsearchClient elasticsearchClient;

    private static final Set<String> BLACKLIST = Set.of(
            "_id",
            "_index",
            "_score",
            "_version",
            "_seq_no",
            "_primary_term",
            "_ignored",
            "_index_mode",
            "_tier",
            "host.name",
            "@timestamp",
            "doc_id");

    public List<String> getGroupByFields(String index) throws IOException {

        FieldCapsResponse response = elasticsearchClient.fieldCaps(fc -> fc
                .index(index)
                .fields("*"));

        Set<String> fields = new TreeSet<>();

        response.fields().forEach((fieldName, types) -> {

            if (BLACKLIST.contains(fieldName)) {
                return;
            }

            boolean aggregatable = types.values().stream()
                    .anyMatch(type -> Boolean.TRUE.equals(type.aggregatable()));

            if (!aggregatable) {
                return;
            }

            if (fieldName.endsWith(".keyword")) {
                fields.add(fieldName.substring(0, fieldName.length() - ".keyword".length()));
            } else if (!fieldName.contains(".")) {
                fields.add(fieldName);
            }
        });

        return new ArrayList<>(fields);
    }

}