package com.vdt.log_monitor.alert.engine;

import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.FetchDataResult;
import com.vdt.log_monitor.alert.model.PipelineStep;
import com.vdt.log_monitor.alert.enums.ResultType;
import com.vdt.log_monitor.alert.enums.ThresholdOperator;
import com.vdt.log_monitor.alert.model.TriggerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class EvaluateThresholdExecutor implements ExpressionExecutor {

    @Override
    public String getType() {
        return "EVALUATE_THRESHOLD";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExpressionResult execute(PipelineStep step, Map<String, ExpressionResult> context) {
        Map<String, Object> params = step.getParams();
        String inputStepId = (String) params.get("input");
        ThresholdOperator operator = ThresholdOperator.valueOf(
                ((String) params.get("operator")).toUpperCase());
        double thresholdValue = Double.parseDouble(params.get("value").toString());

        ExpressionResult prevResult = context.get(inputStepId);
        if (prevResult == null)
            throw new IllegalArgumentException("Không tìm thấy dữ liệu của step trước: " + inputStepId);

        // ── Normalize dữ liệu đầu vào về Map<groupKey, Double> ──────────────────────
        Map<String, Double> metricData = new LinkedHashMap<>();
        List<String> groupByFields = new ArrayList<>();

        Object rawValue = prevResult.getValue();
        if (rawValue instanceof FetchDataResult fdr) {
            if (fdr.getMetrics() != null)
                metricData.putAll(fdr.getMetrics());
            if (fdr.getGroupByFields() != null)
                groupByFields.addAll(fdr.getGroupByFields());
        } else if (rawValue instanceof Map) {
            ((Map<?, ?>) rawValue).forEach((k, v) -> {
                if (v instanceof Number)
                    metricData.put(String.valueOf(k), ((Number) v).doubleValue());
            });
        } else if (rawValue instanceof Number) {
            metricData.put("DEFAULT", ((Number) rawValue).doubleValue());
        } else {
            throw new IllegalArgumentException(
                    "Dữ liệu từ step '" + inputStepId + "' không tương thích để so sánh ngưỡng");
        }

        // ── scopeLabel: mô tả phạm vi đánh giá ──────────────────────────────────────
        // Với EVALUATE_THRESHOLD, không có biểu thức toán học → groupByFields
        // được kế thừa từ FETCH_ES_DATA phía trước.
        // "DEFAULT" nghĩa là không groupBy → label = "Toàn hệ thống"
        String scopeLabel = groupByFields.isEmpty()
                ? "Toàn hệ thống"
                : "Theo " + String.join(", ", groupByFields);

        // ── Evaluate từng group
        // ───────────────────────────────────────────────────────
        boolean isTriggered = false;
        List<String> breachedGroups = new ArrayList<>();
        Map<String, Double> breachedGroupValues = new LinkedHashMap<>(); // raw actual value
        Map<String, Double> computedValues = new LinkedHashMap<>(); // cùng actual value vì không có bước tính trung
                                                                    // gian

        for (Map.Entry<String, Double> entry : metricData.entrySet()) {
            String groupKey = entry.getKey();
            double actualValue = entry.getValue();
            boolean conditionMet = operator.test(actualValue, thresholdValue);

            log.info("Kiểm tra ngưỡng [{}][Group: {}]: {} {} {} → {}",
                    step.getId(), groupKey, actualValue, operator, thresholdValue, conditionMet);

            if (!conditionMet)
                continue;

            isTriggered = true;

            // Thay "DEFAULT" bằng scopeLabel để frontend không thấy chuỗi kỹ thuật
            String displayKey = "DEFAULT".equals(groupKey) ? scopeLabel : groupKey;
            breachedGroups.add(displayKey);
            breachedGroupValues.put(displayKey, actualValue);

            /*
             * computedValues với EVALUATE_THRESHOLD:
             * Không có bước tính toán trung gian (khác MathExecutor).
             * actualValue chính là giá trị cần so sánh với threshold,
             * nên computedValues = actualValue.
             *
             * Frontend có thể dùng computedValues để render:
             * "auth-service|dev: 18 log ERROR (ngưỡng: 5)"
             */
            computedValues.put(displayKey, actualValue);
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("thresholdValue", thresholdValue);
        metadata.put("operator", operator.name());
        metadata.put("actualValues", new LinkedHashMap<>(metricData)); // toàn bộ, không chỉ breached

        TriggerResult triggerResult = TriggerResult.builder()
                .triggered(isTriggered)
                .breachedGroups(breachedGroups)
                .breachedGroupValues(breachedGroupValues)
                .computedValues(computedValues)
                .groupByFields(groupByFields)
                .scopeLabel(scopeLabel)
                .metadata(metadata)
                .build();

        return ExpressionResult.builder()
                .type(ResultType.BOOLEAN)
                .value(triggerResult)
                .build();
    }
}