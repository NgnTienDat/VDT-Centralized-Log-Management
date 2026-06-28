package com.vdt.log_monitor.alert;

import com.vdt.log_monitor.alert.model.RuleConfig;
import com.vdt.log_monitor.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts/rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RuleConfig>> createRule(@RequestBody @Valid RuleConfig ruleConfig) {
        // Ủy quyền xử lý logic nghiệp vụ cho Service tầng dưới
        RuleConfig savedRule = alertRuleService.createRule(ruleConfig);

        return ResponseEntity.ok(ApiResponse.success(savedRule));
    }
}