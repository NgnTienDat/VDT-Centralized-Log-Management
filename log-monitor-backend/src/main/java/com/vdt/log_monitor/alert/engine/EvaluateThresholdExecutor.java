package com.vdt.log_monitor.alert.engine;

import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.FetchDataResult;
import com.vdt.log_monitor.alert.model.PipelineStep;
import com.vdt.log_monitor.alert.enums.ResultType;
import com.vdt.log_monitor.alert.enums.ThresholdOperator;
import com.vdt.log_monitor.alert.model.TriggerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        ThresholdOperator operator = ThresholdOperator.valueOf(((String) params.get("operator")).toUpperCase());
        double thresholdValue = Double.parseDouble(params.get("value").toString());

        ExpressionResult prevResult = context.get(inputStepId);
        if (prevResult == null) {
            throw new IllegalArgumentException("Không tìm thấy dữ liệu của step trước: " + inputStepId);
        }

        Map<String, Double> metricData = new HashMap<>();
        List<String> groupByFields = new ArrayList<>();

        // Thích ứng động với mọi định dạng cấu trúc số học từ các Executor khác
        Object rawValue = prevResult.getValue();
        if (rawValue instanceof FetchDataResult) {
            FetchDataResult fdr = (FetchDataResult) rawValue;
            if (fdr.getMetrics() != null) {
                metricData.putAll(fdr.getMetrics());
            }
            if (fdr.getGroupByFields() != null) {
                groupByFields.addAll(fdr.getGroupByFields());
            }
        } else if (rawValue instanceof Map) {
            Map<?, ?> rawMap = (Map<?, ?>) rawValue;
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object val = entry.getValue();
                if (val instanceof Number) {
                    metricData.put(key, ((Number) val).doubleValue());
                }
            }
        } else if (rawValue instanceof Number) {
            metricData.put("DEFAULT", ((Number) rawValue).doubleValue());
        } else {
            throw new IllegalArgumentException("Dữ liệu từ step '" + inputStepId + "' không tương thích để so sánh ngưỡng (Cần FetchDataResult, Map hoặc Number)");
        }

        boolean isTriggered = false;
        List<String> breachedGroups = new ArrayList<>();

        for (Map.Entry<String, Double> entry : metricData.entrySet()) {
            double actualValue = entry.getValue();
            boolean conditionMet = operator.test(actualValue, thresholdValue);

            if (conditionMet) {
                isTriggered = true;
                breachedGroups.add(entry.getKey());
            }

            log.info("Kiểm tra ngưỡng [{}][Group: {}]: thực tế {} {} ngưỡng {} => {}",
                    step.getId(), entry.getKey(), actualValue, operator, thresholdValue, conditionMet);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("thresholdValue", thresholdValue);
        metadata.put("operator", operator.name());
        metadata.put("actualValues", metricData);

        TriggerResult triggerResult = TriggerResult.builder()
                .triggered(isTriggered)
                .breachedGroups(breachedGroups)
                .groupByFields(groupByFields)
                .metadata(metadata)
                .build();

        return ExpressionResult.builder()
                .type(ResultType.BOOLEAN)
                .value(triggerResult)
                .build();
    }
}