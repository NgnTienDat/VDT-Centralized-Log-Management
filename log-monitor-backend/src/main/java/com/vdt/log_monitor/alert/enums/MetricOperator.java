package com.vdt.log_monitor.alert.enums;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;

public enum MetricOperator {
    COUNT {
        @Override
        public Aggregation buildAggregation(String field) {
            return null; // COUNT mặc định lấy qua doc_count, không cần build agg riêng
        }

        @Override
        public Double extractValue(Aggregate aggregate) {
            return null;
        }
    },
    SUM {
        @Override
        public Aggregation buildAggregation(String field) {
            return Aggregation.of(a -> a.sum(s -> s.field(field)));
        }

        @Override
        public Double extractValue(Aggregate aggregate) {
            return aggregate.sum().value();
        }
    },
    MAX {
        @Override
        public Aggregation buildAggregation(String field) {
            return Aggregation.of(a -> a.max(m -> m.field(field)));
        }

        @Override
        public Double extractValue(Aggregate aggregate) {
            return aggregate.max().value();
        }
    },
    MIN {
        @Override
        public Aggregation buildAggregation(String field) {
            return Aggregation.of(a -> a.min(m -> m.field(field)));
        }

        @Override
        public Double extractValue(Aggregate aggregate) {
            return aggregate.min().value();
        }
    },
    AVG {
        @Override
        public Aggregation buildAggregation(String field) {
            return Aggregation.of(a -> a.avg(av -> av.field(field)));
        }

        @Override
        public Double extractValue(Aggregate aggregate) {
            return aggregate.avg().value();
        }
    };

    public abstract Aggregation buildAggregation(String field);

    public abstract Double extractValue(Aggregate aggregate);
}