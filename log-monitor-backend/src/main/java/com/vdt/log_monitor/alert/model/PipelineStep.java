package com.vdt.log_monitor.alert.model;

import lombok.Data;
import java.util.Map;

@Data
public class PipelineStep {
    private String id;
    private String type; // FETCH_ES_DATA, EVALUATE_THRESHOLD
    private Map<String, Object> params; // Map linh hoạt nhận param động theo từng executor
}