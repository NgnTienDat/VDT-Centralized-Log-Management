package com.vdt.log_monitor.alert.enums;

/**
 * Tách các toán tử so sánh ngưỡng ra enum riêng, theo đúng pattern đã dùng ở
 * MetricOperator (enum + abstract method). Trước đây EvaluateThresholdExecutor
 * switch trực tiếp trên String -> mỗi lần thêm toán tử mới (vd BETWEEN,
 * PERCENT_CHANGE...) phải sửa lại switch-case trong code cũ.
 *
 * Giờ thêm toán tử mới chỉ cần thêm 1 constant ở đây, EvaluateThresholdExecutor
 * không cần đổi gì.
 */
public enum ThresholdOperator {
    GREATER_THAN {
        @Override
        public boolean test(double actual, double threshold) {
            return actual > threshold;
        }
    },
    LESS_THAN {
        @Override
        public boolean test(double actual, double threshold) {
            return actual < threshold;
        }
    },
    GREATER_THAN_OR_EQUALS {
        @Override
        public boolean test(double actual, double threshold) {
            return actual >= threshold;
        }
    },
    LESS_THAN_OR_EQUALS {
        @Override
        public boolean test(double actual, double threshold) {
            return actual <= threshold;
        }
    },
    EQUALS {
        @Override
        public boolean test(double actual, double threshold) {
            return Math.abs(actual - threshold) < 0.0001;
        }
    },
    NOT_EQUALS {
        @Override
        public boolean test(double actual, double threshold) {
            return Math.abs(actual - threshold) >= 0.0001;
        }
    };

    public abstract boolean test(double actual, double threshold);
}
