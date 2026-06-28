package com.vdt.log_monitor.alert.engine;

import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.PipelineResult;
import com.vdt.log_monitor.alert.model.PipelineStep;
import com.vdt.log_monitor.alert.enums.ResultType;
import com.vdt.log_monitor.alert.model.TriggerResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpressionEngine {

    private static final String FETCH_ES_DATA_TYPE = "FETCH_ES_DATA";

    private final Map<String, ExpressionExecutor> executorRegistry = new HashMap<>();

    public ExpressionEngine(List<ExpressionExecutor> executors) {
        for (ExpressionExecutor executor : executors) {
            executorRegistry.put(executor.getType(), executor);
        }
    }

    public PipelineResult runPipeline(List<PipelineStep> steps, String triggerStepId) {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Rule không có step nào để thực thi!");
        }
        if (!FETCH_ES_DATA_TYPE.equals(steps.get(0).getType())) {
            throw new IllegalArgumentException(
                    "Step đầu tiên của Rule buộc phải là FETCH_ES_DATA, hiện tại là: " + steps.get(0).getType());
        }

        Map<String, ExpressionResult> context = new HashMap<>();

        for (PipelineStep step : steps) {
            ExpressionExecutor executor = executorRegistry.get(step.getType());
            if (executor == null) {
                throw new UnsupportedOperationException("Unsupported type: " + step.getType());
            }

            /*
             * JSON mẫu trả về từ FetchDataResult như sau:
             *  {
             *     "groupByFields": ["service", "status"],
             *     "metrics": {
             *       "auth-service|500": 15.0,
             *       "payment-service|200": 1200.0
             *   }
             * */
            ExpressionResult stepResult = executor.execute(step, context);
            context.put(step.getId(), stepResult);
        }

        // 2. Định danh và kiểm tra Step đóng vai trò quyết định Trigger
        if (triggerStepId == null || !context.containsKey(triggerStepId)) {
            throw new IllegalArgumentException("Không tìm thấy hoặc chưa cấu hình triggerStepId hợp lệ trong Rule!");
        }

        ExpressionResult triggerResult = context.get(triggerStepId);
        if (triggerResult.getType() != ResultType.BOOLEAN) {
            throw new IllegalStateException("Step chỉ định làm Trigger quyết định (" + triggerStepId + ") bắt buộc phải trả về kiểu BOOLEAN");
        }

        // 3. Trích xuất trạng thái trigger - mọi executor trả BOOLEAN đều phải đóng gói value vào TriggerResult.
        // Đây là hợp đồng dữ liệu bắt buộc (xem TriggerResult), giúp chỗ này không phải đoán cấu trúc Map nữa.
        Object triggerValue = triggerResult.getValue();
        if (!(triggerValue instanceof TriggerResult)) {
            throw new IllegalStateException(
                    "Step Trigger (" + triggerStepId + ") trả về kiểu BOOLEAN nhưng value không phải TriggerResult");
        }
        boolean triggered = ((TriggerResult) triggerValue).isTriggered();

        return new PipelineResult(triggered, context);
    }
}


/*
 * JSON PipelineStep mẫu EVALUATE_THRESHOLD:
 * {
 *     "id": "check_threshold",
 *     "type": "EVALUATE_THRESHOLD",
 *     "params": {
 *         "input": "fetch_log_id",
 *         "operator": "GREATER_THAN",
 *         "value": 100.0
 *     }
 * }
 *
 * JSON PipelineStep mẫu EVALUATE_MATH:
 * {
 *     "id": "check_math",
 *     "type": "EVALUATE_MATH",
 *     "params": {
 *         "input": ["fetch_log_id", "math_step_id"],
 *         "expression": "${fetch_log_id_value} + ${math_step_id_value} > 100",
 *         "value": true // hoặc number tùy logic
 *     }
 * }
 *
 * JSON mẫu của context Map có thể trông như sau:
 *  {
 *     "fetch_logs_id": {
 *         "type": "METRIC_RESULT",
 *         "value": {
 *             "groupByFields": ["service", "status"],
 *             "metrics": {
 *                 "auth-service|500": 15.0,
 *                 "payment-service|200": 1200.0
 *             }
 *         }
 *     },
 *     "math_step_id": {
 *         "type": "NUMBER" // hoặc BOOLEAN,
 *         "value": 17.5 // hoặc 1(true)/0(false) tùy logic
 *     }
 * }
 * */