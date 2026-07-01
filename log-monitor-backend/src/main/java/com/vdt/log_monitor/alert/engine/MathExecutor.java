package com.vdt.log_monitor.alert.engine;

import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.FetchDataResult;
import com.vdt.log_monitor.alert.model.PipelineStep;
import com.vdt.log_monitor.alert.enums.ResultType;
import com.vdt.log_monitor.alert.model.TriggerResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MathExecutor implements ExpressionExecutor {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public String getType() {
        return "MATH";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ExpressionResult execute(PipelineStep step, Map<String, ExpressionResult> context) {
        Map<String, Object> params = step.getParams();
        List<String> inputs = (List<String>) params.get("input");
        String expressionStr = (String) params.get("expression");

        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Step MATH yêu cầu tham số 'input' không được trống");
        }
        if (expressionStr == null || expressionStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Step MATH yêu cầu tham số 'expression' không được trống");
        }

        // Biên dịch biểu thức SpEL (Hỗ trợ tốt các toán tử &&, ||, ==, !=, >, <, >=, <=)
        Expression expression = parser.parseExpression(expressionStr);

        // Kiểm tra xem các input đầu vào có chứa dữ liệu phân nhóm (METRIC_RESULT) không
        boolean hasMetricResult = false;
        Set<String> allGroupKeys = new LinkedHashSet<>();
        List<String> combinedGroupByFields = new ArrayList<>();

        for (String inputId : inputs) {
            ExpressionResult inputResult = context.get(inputId);
            if (inputResult == null) {
                throw new IllegalArgumentException("Không tìm thấy dữ liệu ngữ cảnh của step: " + inputId);
            }
            if (inputResult.getType() == ResultType.METRIC_RESULT) {
                hasMetricResult = true;
                Object val = inputResult.getValue();
                if (val instanceof FetchDataResult) {
                    FetchDataResult fdr = (FetchDataResult) val;
                    if (fdr.getMetrics() != null) {
                        allGroupKeys.addAll(fdr.getMetrics().keySet());
                    }
                    if (fdr.getGroupByFields() != null) {
                        for (String field : fdr.getGroupByFields()) {
                            if (!combinedGroupByFields.contains(field)) {
                                combinedGroupByFields.add(field);
                            }
                        }
                    }
                } else if (val instanceof Map) {
                    allGroupKeys.addAll(((Map<String, ?>) val).keySet());
                } else {
                    // METRIC_RESULT bắt buộc phải là FetchDataResult (hoặc Map tương đương).
                    // Nếu có executor mới trả về METRIC_RESULT với dạng khác, fail rõ ràng ở đây
                    // thay vì âm thầm bỏ qua group key của input đó.
                    throw new IllegalArgumentException("Dữ liệu METRIC_RESULT từ step '" + inputId + "' không tương thích (cần FetchDataResult hoặc Map)");
                }
            }
        }

        // =========================================================================
        // TRƯỜNG HỢP 1: Có chứa dữ liệu nhóm -> Thực hiện tính toán song song theo từng Cụm Group
        // =========================================================================
        if (hasMetricResult) {
            if (allGroupKeys.isEmpty()) {
                allGroupKeys.add("DEFAULT");
            }

            Map<String, Object> groupEvaluations = new HashMap<>();
            boolean isBooleanResult = false;

            for (String groupKey : allGroupKeys) {
                StandardEvaluationContext evalContext = new StandardEvaluationContext();

                // Trích xuất giá trị chính xác của từng Input tại cụm GroupKey hiện tại
                for (String inputId : inputs) {
                    ExpressionResult inputResult = context.get(inputId);

                    if (inputResult.getType() == ResultType.METRIC_RESULT) {
                        Double metricValue = 0.0;
                        Object val = inputResult.getValue();

                        if (val instanceof FetchDataResult) {
                            FetchDataResult fdr = (FetchDataResult) val;
                            if (fdr.getMetrics() != null && fdr.getMetrics().containsKey(groupKey)) {
                                metricValue = fdr.getMetrics().get(groupKey);
                            } else if (fdr.getMetrics() != null && fdr.getMetrics().containsKey("DEFAULT")) {
                                metricValue = fdr.getMetrics().get("DEFAULT");
                            }
                        } else if (val instanceof Map) {
                            Map<String, ?> rawMap = (Map<String, ?>) val;
                            Object mVal = rawMap.get(groupKey);
                            if (mVal instanceof Number) {
                                metricValue = ((Number) mVal).doubleValue();
                            }
                        } else {
                            // Cùng lý do với nhánh thu thập allGroupKeys ở trên: không âm thầm dùng 0.0.
                            throw new IllegalArgumentException("Dữ liệu METRIC_RESULT từ step '" + inputId + "' không tương thích (cần FetchDataResult hoặc Map)");
                        }
                        evalContext.setVariable(inputId, metricValue);
                    } else {
                        // Nếu input là dạng cấu trúc khác (ví dụ TriggerResult của Threshold/Math khác), nạp nguyên thể payload.
                        // Lưu ý: từ khi BOOLEAN dùng TriggerResult, biểu thức SpEL truy cập field qua property
                        // (vd #step.triggered) thay vì cú pháp Map cũ (#step['triggered']).
                        evalContext.setVariable(inputId, inputResult.getValue());
                    }
                }

                Object resultObj = expression.getValue(evalContext);
                groupEvaluations.put(groupKey, resultObj);
                if (resultObj instanceof Boolean) {
                    isBooleanResult = true;
                }
            }

            // Đóng gói dữ liệu đầu ra dựa vào bản chất kiểu giá trị trả về của Expression
            if (isBooleanResult) {
                boolean globalTriggered = false;
                List<String> breachedGroups = new ArrayList<>();

                for (Map.Entry<String, Object> entry : groupEvaluations.entrySet()) {
                    if (Boolean.TRUE.equals(entry.getValue())) {
                        globalTriggered = true;
                        breachedGroups.add(entry.getKey());
                    }
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("expression", expressionStr);

                TriggerResult triggerResult = TriggerResult.builder()
                        .triggered(globalTriggered)
                        .breachedGroups(breachedGroups)
                        .groupByFields(combinedGroupByFields)
                        .metadata(metadata)
                        .build();

                return ExpressionResult.builder()
                        .type(ResultType.BOOLEAN)
                        .value(triggerResult)
                        .build();
            } else {
                Map<String, Double> finalMetrics = new HashMap<>();
                for (Map.Entry<String, Object> entry : groupEvaluations.entrySet()) {
                    double numVal = (entry.getValue() instanceof Number) ? ((Number) entry.getValue()).doubleValue() : 0.0;
                    finalMetrics.put(entry.getKey(), numVal);
                }

                FetchDataResult dataResult = FetchDataResult.builder()
                        .groupByFields(combinedGroupByFields)
                        .metrics(finalMetrics)
                        .build();

                return ExpressionResult.builder()
                        .type(ResultType.METRIC_RESULT)
                        .value(dataResult)
                        .build();
            }
        }
        // =========================================================================
        // TRƯỜNG HỢP 2: Không chứa dữ liệu nhóm (Tất cả inputs đều là Scalar / Toàn cục)
        // =========================================================================
        else {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            for (String inputId : inputs) {
                ExpressionResult inputResult = context.get(inputId);
                evalContext.setVariable(inputId, inputResult.getValue());
            }

            Object resultObj = expression.getValue(evalContext);

            if (resultObj instanceof Boolean) {
                boolean triggered = (Boolean) resultObj;

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("expression", expressionStr);

                TriggerResult triggerResult = TriggerResult.builder()
                        .triggered(triggered)
                        .breachedGroups(triggered ? List.of("DEFAULT") : List.of())
                        .groupByFields(List.of())
                        .metadata(metadata)
                        .build();

                return ExpressionResult.builder()
                        .type(ResultType.BOOLEAN)
                        .value(triggerResult)
                        .build();
            } else {
                Double numericVal = (resultObj instanceof Number) ? ((Number) resultObj).doubleValue() : 0.0;
                Map<String, Double> finalMetrics = new HashMap<>();
                finalMetrics.put("DEFAULT", numericVal);

                FetchDataResult dataResult = FetchDataResult.builder()
                        .groupByFields(List.of())
                        .metrics(finalMetrics)
                        .build();

                return ExpressionResult.builder()
                        .type(ResultType.METRIC_RESULT)
                        .value(dataResult)
                        .build();
            }
        }
    }
}