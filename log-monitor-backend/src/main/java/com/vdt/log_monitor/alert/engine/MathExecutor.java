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

        if (inputs == null || inputs.isEmpty())
            throw new IllegalArgumentException("Step MATH yêu cầu tham số 'input' không được trống");
        if (expressionStr == null || expressionStr.trim().isEmpty())
            throw new IllegalArgumentException("Step MATH yêu cầu tham số 'expression' không được trống");

        Expression expression = parser.parseExpression(expressionStr);

        // ── Thu thập tất cả group keys từ các input METRIC_RESULT ────────────────────
        boolean hasMetricResult = false;
        Set<String> allGroupKeys = new LinkedHashSet<>();
        List<String> combinedGroupByFields = new ArrayList<>();

        for (String inputId : inputs) {
            ExpressionResult inputResult = context.get(inputId);
            if (inputResult == null)
                throw new IllegalArgumentException("Không tìm thấy dữ liệu ngữ cảnh của step: " + inputId);

            if (inputResult.getType() == ResultType.METRIC_RESULT) {
                hasMetricResult = true;
                Object val = inputResult.getValue();
                if (val instanceof FetchDataResult fdr) {
                    if (fdr.getMetrics() != null)
                        allGroupKeys.addAll(fdr.getMetrics().keySet());
                    if (fdr.getGroupByFields() != null) {
                        for (String field : fdr.getGroupByFields()) {
                            if (!combinedGroupByFields.contains(field))
                                combinedGroupByFields.add(field);
                        }
                    }
                } else if (val instanceof Map) {
                    allGroupKeys.addAll(((Map<String, ?>) val).keySet());
                } else {
                    throw new IllegalArgumentException(
                            "Dữ liệu METRIC_RESULT từ step '" + inputId + "' không tương thích");
                }
            }
        }

        // ── Tính scopeLabel một lần — dùng chung cho cả 2 nhánh ────────────────────
        // Nếu không có groupBy → "Toàn hệ thống"
        // Nếu có groupBy → "Theo service, environment" (join tên các trường)
        String scopeLabel = combinedGroupByFields.isEmpty()
                ? "Toàn hệ thống"
                : "Theo " + String.join(", ", combinedGroupByFields);

        // ── Nhánh có METRIC_RESULT (có hoặc không có groupBy đều qua đây) ────────────
        if (hasMetricResult) {
            if (allGroupKeys.isEmpty())
                allGroupKeys.add("DEFAULT");

            Map<String, Object> groupEvaluations = new HashMap<>();
            boolean isBooleanResult = false;

            for (String groupKey : allGroupKeys) {
                StandardEvaluationContext evalContext = new StandardEvaluationContext();
                for (String inputId : inputs) {
                    ExpressionResult inputResult = context.get(inputId);
                    if (inputResult.getType() == ResultType.METRIC_RESULT) {
                        evalContext.setVariable(inputId, extractMetricValue(inputResult, groupKey, inputId));
                    } else {
                        evalContext.setVariable(inputId, inputResult.getValue());
                    }
                }
                Object resultObj = expression.getValue(evalContext);
                groupEvaluations.put(groupKey, resultObj);
                if (resultObj instanceof Boolean)
                    isBooleanResult = true;
            }

            if (isBooleanResult) {
                return buildBooleanResult(
                        expressionStr, inputs, context, groupEvaluations,
                        combinedGroupByFields, scopeLabel);
            } else {
                return buildNumericResult(groupEvaluations, combinedGroupByFields);
            }
        }

        // ── Nhánh scalar (không có METRIC_RESULT nào) ────────────────────────────────
        return evaluateScalar(expressionStr, expression, inputs, context, scopeLabel);
    }

    // ── Helpers
    // ───────────────────────────────────────────────────────────────────

    /**
     * Build ExpressionResult kiểu BOOLEAN từ kết quả evaluate theo group.
     *
     * Thay đổi chính so với phiên bản cũ:
     * computedValues: lưu giá trị số của biểu thức tại mỗi group vi phạm.
     * Vì biểu thức MATH có thể là "(#B/#A)*100 > 5", SpEL evaluate trả Boolean.
     * Để lấy giá trị % thực tế, ta re-evaluate phần numeric bằng cách bỏ
     * so sánh cuối ("> 5") và chỉ lấy "(#B/#A)*100".
     * Tuy nhiên parse lại expression phức tạp và dễ sai — thay vào đó,
     * ta tính trực tiếp từ các input: lấy tỉ lệ STEP_B/STEP_A*100 tại group đó.
     *
     * Vì MathExecutor không biết semantic của biểu thức (có thể là %, ratio, hay
     * biểu thức tùy ý), ta lưu giá trị của input đầu tiên là METRIC_RESULT
     * tại group đó vào computedValues — đây là "giá trị bị vi phạm" rõ nghĩa nhất
     * mà không cần parse biểu thức.
     *
     * Nếu cần % chính xác, rule nên thêm 1 MATH step riêng tính % trước,
     * rồi EVALUATE_THRESHOLD so sánh — khi đó computedValues sẽ là % thật.
     *
     * scopeLabel: truyền vào để frontend hiển thị thay vì "DEFAULT".
     * breachedGroups: thay "DEFAULT" bằng scopeLabel khi không có groupBy.
     */
    private ExpressionResult buildBooleanResult(
            String expressionStr,
            List<String> inputs,
            Map<String, ExpressionResult> context,
            Map<String, Object> groupEvaluations,
            List<String> groupByFields,
            String scopeLabel) {

        boolean globalTriggered = false;
        List<String> breachedGroups = new ArrayList<>();
        Map<String, Double> breachedGroupValues = new HashMap<>(); // raw input value
        Map<String, Double> computedValues = new HashMap<>(); // giá trị biểu thức đã tính

        for (Map.Entry<String, Object> entry : groupEvaluations.entrySet()) {
            if (!Boolean.TRUE.equals(entry.getValue()))
                continue;

            globalTriggered = true;
            String groupKey = entry.getKey();

            // Label hiển thị: thay "DEFAULT" bằng scopeLabel
            String displayKey = "DEFAULT".equals(groupKey) ? scopeLabel : groupKey;
            breachedGroups.add(displayKey);

            // Raw value từ input METRIC_RESULT đầu tiên
            Double rawVal = resolveRepresentativeValue(inputs, context, groupKey);
            breachedGroupValues.put(displayKey, rawVal);

            /*
             * computedValues: cố gắng lấy giá trị từ input thứ 2 nếu có (thường là
             * denominator để tính tỉ lệ), hoặc fallback về rawVal.
             *
             * Pattern phổ biến:
             * input = ["STEP_B", "STEP_A"] → (B/A)*100 > threshold
             * computedValues lưu: (B/A)*100 tại group đó
             *
             * Ta tính lại bằng cách lấy B và A từ context thay vì parse biểu thức.
             * Nếu chỉ có 1 input hoặc input thứ 2 không phải METRIC_RESULT → dùng rawVal.
             */
            Double computed = tryComputeRatio(inputs, context, groupKey);
            computedValues.put(displayKey, computed != null ? computed : rawVal);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("expression", expressionStr);

        TriggerResult triggerResult = TriggerResult.builder()
                .triggered(globalTriggered)
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

    private ExpressionResult buildNumericResult(
            Map<String, Object> groupEvaluations,
            List<String> groupByFields) {

        Map<String, Double> finalMetrics = new HashMap<>();
        for (Map.Entry<String, Object> entry : groupEvaluations.entrySet()) {
            double numVal = entry.getValue() instanceof Number
                    ? ((Number) entry.getValue()).doubleValue()
                    : 0.0;
            finalMetrics.put(entry.getKey(), numVal);
        }
        return ExpressionResult.builder()
                .type(ResultType.METRIC_RESULT)
                .value(FetchDataResult.builder()
                        .groupByFields(groupByFields)
                        .metrics(finalMetrics)
                        .build())
                .build();
    }

    private ExpressionResult evaluateScalar(
            String expressionStr,
            Expression expression,
            List<String> inputs,
            Map<String, ExpressionResult> context,
            String scopeLabel) {

        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        for (String inputId : inputs) {
            ExpressionResult inputResult = context.get(inputId);
            evalContext.setVariable(inputId, inputResult.getValue());
        }

        Object resultObj = expression.getValue(evalContext);

        if (resultObj instanceof Boolean triggered) {
            Map<String, Double> breachedGroupValues = new HashMap<>();
            Map<String, Double> computedValues = new HashMap<>();

            if (triggered) {
                Double rawVal = resolveRepresentativeValue(inputs, context, "DEFAULT");
                Double computed = tryComputeRatio(inputs, context, "DEFAULT");
                breachedGroupValues.put(scopeLabel, rawVal);
                computedValues.put(scopeLabel, computed != null ? computed : rawVal);
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("expression", expressionStr);

            TriggerResult triggerResult = TriggerResult.builder()
                    .triggered(triggered)
                    .breachedGroups(triggered ? List.of(scopeLabel) : List.of())
                    .breachedGroupValues(breachedGroupValues)
                    .computedValues(computedValues)
                    .groupByFields(List.of())
                    .scopeLabel(scopeLabel)
                    .metadata(metadata)
                    .build();

            return ExpressionResult.builder()
                    .type(ResultType.BOOLEAN)
                    .value(triggerResult)
                    .build();
        }

        // Scalar number
        Double numericVal = resultObj instanceof Number ? ((Number) resultObj).doubleValue() : 0.0;
        return ExpressionResult.builder()
                .type(ResultType.METRIC_RESULT)
                .value(FetchDataResult.builder()
                        .groupByFields(List.of())
                        .metrics(Map.of("DEFAULT", numericVal))
                        .build())
                .build();
    }

    // ── Private utilities
    // ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Double extractMetricValue(ExpressionResult inputResult, String groupKey, String inputId) {
        Object val = inputResult.getValue();
        if (val instanceof FetchDataResult fdr) {
            Map<String, Double> metrics = fdr.getMetrics();
            if (metrics == null)
                return 0.0;
            if (metrics.containsKey(groupKey))
                return metrics.get(groupKey);
            if (metrics.containsKey("DEFAULT"))
                return metrics.get("DEFAULT");
            return 0.0;
        } else if (val instanceof Map) {
            Object mVal = ((Map<String, ?>) val).get(groupKey);
            return mVal instanceof Number ? ((Number) mVal).doubleValue() : 0.0;
        }
        throw new IllegalArgumentException(
                "Dữ liệu METRIC_RESULT từ step '" + inputId + "' không tương thích");
    }

    @SuppressWarnings("unchecked")
    private Double resolveRepresentativeValue(List<String> inputs,
            Map<String, ExpressionResult> context,
            String groupKey) {
        for (String inputId : inputs) {
            ExpressionResult r = context.get(inputId);
            if (r == null || r.getType() != ResultType.METRIC_RESULT)
                continue;
            Object val = r.getValue();
            if (val instanceof FetchDataResult fdr) {
                Map<String, Double> m = fdr.getMetrics();
                if (m == null)
                    continue;
                if (m.containsKey(groupKey))
                    return m.get(groupKey);
                if (m.containsKey("DEFAULT"))
                    return m.get("DEFAULT");
            } else if (val instanceof Map) {
                Object mv = ((Map<String, ?>) val).get(groupKey);
                if (mv instanceof Number)
                    return ((Number) mv).doubleValue();
            }
        }
        return 1.0; // marker: breach nhưng không có giá trị số
    }

    /**
     * Tính (numerator / denominator) * 100 nếu có đủ 2 input METRIC_RESULT.
     * Input[0] = numerator (STEP_B = error count),
     * Input[1] = denominator (STEP_A = total count).
     *
     * Pattern này đúng với rule tỉ lệ lỗi phổ biến nhất.
     * Nếu rule không theo pattern 2-input hoặc denominator = 0 → trả null
     * (caller sẽ fallback về rawVal).
     */
    private Double tryComputeRatio(List<String> inputs,
            Map<String, ExpressionResult> context,
            String groupKey) {
        if (inputs.size() < 2)
            return null;

        ExpressionResult numResult = context.get(inputs.get(0));
        ExpressionResult denomResult = context.get(inputs.get(1));

        if (numResult == null || denomResult == null)
            return null;
        if (numResult.getType() != ResultType.METRIC_RESULT)
            return null;
        if (denomResult.getType() != ResultType.METRIC_RESULT)
            return null;

        Double numerator = extractMetricValueSafe(numResult, groupKey);
        Double denominator = extractMetricValueSafe(denomResult, groupKey);

        if (denominator == null || denominator == 0.0)
            return null;
        if (numerator == null)
            return null;

        // Làm tròn 2 chữ số thập phân
        double ratio = (numerator / denominator) * 100.0;
        return Math.round(ratio * 100.0) / 100.0;
    }

    @SuppressWarnings("unchecked")
    private Double extractMetricValueSafe(ExpressionResult result, String groupKey) {
        Object val = result.getValue();
        if (val instanceof FetchDataResult fdr) {
            Map<String, Double> m = fdr.getMetrics();
            if (m == null)
                return null;
            if (m.containsKey(groupKey))
                return m.get(groupKey);
            if (m.containsKey("DEFAULT"))
                return m.get("DEFAULT");
        } else if (val instanceof Map) {
            Object mv = ((Map<String, ?>) val).get(groupKey);
            if (mv instanceof Number)
                return ((Number) mv).doubleValue();
        }
        return null;
    }
}