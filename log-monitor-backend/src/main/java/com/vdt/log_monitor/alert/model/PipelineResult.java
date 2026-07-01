package com.vdt.log_monitor.alert.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class PipelineResult {
    private boolean isTriggered;
    private Map<String, ExpressionResult> context; // Giữ lại kết quả của các STEP_1, STEP_2...
}