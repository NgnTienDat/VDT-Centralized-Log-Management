package com.vdt.log_monitor.alert;

import com.vdt.log_monitor.alert.dto.UpdateRuleRequest;
import com.vdt.log_monitor.alert.model.AlertNotificationDocument;
import com.vdt.log_monitor.alert.model.RuleConfig;
import com.vdt.log_monitor.common.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts/rules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;
    private final AlertNotificationRepository notificationRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<RuleConfig>> createRule(
            @RequestBody @Valid RuleConfig ruleConfig) {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.createRule(ruleConfig)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RuleConfig>>> getAllRules() {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.getAllRules()));
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RuleConfig>> getRuleById(@PathVariable String ruleId) {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.getRuleById(ruleId)));
    }

    @PatchMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<RuleConfig>> patchRule(
            @PathVariable String ruleId,
            @RequestBody @Valid UpdateRuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alertRuleService.patchRule(ruleId, request)));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable String ruleId) {
        alertRuleService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{ruleId}/notifications")
    public ResponseEntity<ApiResponse<List<AlertNotificationDocument>>> getNotificationsByRule(
            @PathVariable String ruleId) {

        List<AlertNotificationDocument> notifications =
                notificationRepository.findByRuleIdOrderByTimestampDesc(ruleId);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }
}