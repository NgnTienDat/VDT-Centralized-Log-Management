package com.vdt.log_monitor.alert.engine;

import com.vdt.log_monitor.alert.model.ExpressionResult;
import com.vdt.log_monitor.alert.model.PipelineStep;
import java.util.Map;

public interface ExpressionExecutor {
    String getType();
    ExpressionResult execute(PipelineStep step, Map<String, ExpressionResult> context);
}