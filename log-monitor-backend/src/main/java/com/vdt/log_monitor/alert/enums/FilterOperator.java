package com.vdt.log_monitor.alert.enums;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

public enum FilterOperator {
    EQUALS {
        @Override
        public Query buildQuery(String key, String value) {
            return QueryBuilders.term(t -> t.field(key).value(value));
        }
    },
    CONTAINS {
        @Override
        public Query buildQuery(String key, String value) {
            return QueryBuilders.match(m -> m.field(key).query(value));
        }
    };

    public abstract Query buildQuery(String key, String value);
}
