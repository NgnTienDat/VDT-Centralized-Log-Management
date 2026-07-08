package com.vdt.log_monitor.alert;

import com.vdt.log_monitor.alert.dto.UpdateRuleRequest;
import com.vdt.log_monitor.alert.enums.AlertState;
import com.vdt.log_monitor.alert.model.PipelineStep;
import com.vdt.log_monitor.alert.model.RuleConfig;
import com.vdt.log_monitor.alert.scheduler.AlertSchedulerManager;
import com.vdt.log_monitor.common.exception.AppException;
import com.vdt.log_monitor.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository ruleRepository;
    private final AlertSchedulerManager schedulerManager;

    public List<RuleConfig> getAllRules() {
        return ruleRepository.findAll();
    }

    public RuleConfig getRuleById(String ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new AppException(ErrorCode.ALERT_RULE_NOT_FOUND));
    }

    public RuleConfig createRule(RuleConfig ruleConfig) {

        List<PipelineStep> steps = ruleConfig.getPipelineSteps();

        if (steps == null
                || steps.isEmpty()
                || !"FETCH_ES_DATA".equals(steps.get(0).getType())) {
            throw new AppException(ErrorCode.ALERT_RULE_INVALID_FIRST_STEP);
        }

        ruleConfig.setRuleId(UidGenerator.generateUid());

        if (ruleConfig.getIsActive() == null) {
            ruleConfig.setIsActive(true);
        }

        ruleConfig.setLastRunTime(0L);
        ruleConfig.setAlertState(AlertState.OK);
        ruleConfig.setLastNotifiedTime(0L);

        RuleConfig savedRule = ruleRepository.save(ruleConfig);
        schedulerManager.scheduleRule(savedRule);

        return savedRule;
    }

    public RuleConfig patchRule(String ruleId, UpdateRuleRequest request) {
        RuleConfig existing = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new AppException(ErrorCode.ALERT_RULE_NOT_FOUND));

        // Chỉ ghi đè field nào được gửi lên (khác null)
        if (request.getName() != null) {
            existing.setName(request.getName().strip());
        }
        if (request.getIntervalMinutes() != null) {
            existing.setIntervalMinutes(request.getIntervalMinutes());
        }
        if (request.getIsActive() != null) {
            existing.setIsActive(request.getIsActive());
        }
        if (request.getRepeatIntervalMinutes() != null) {
            existing.setRepeatIntervalMinutes(request.getRepeatIntervalMinutes());
        }
        if (request.getTriggerStepId() != null) {
            existing.setTriggerStepId(request.getTriggerStepId());
        }
        if (request.getPipelineSteps() != null) {
            existing.setPipelineSteps(request.getPipelineSteps());
        }
        if (request.getNotificationTemplate() != null) {
            existing.setNotificationTemplate(request.getNotificationTemplate());
        }

        RuleConfig savedRule = ruleRepository.save(existing);
        schedulerManager.scheduleRule(savedRule);

        log.info("Cập nhật thành công Rule ID: {}", ruleId);
        return savedRule;
    }

    public void deleteRule(String ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new AppException(ErrorCode.ALERT_RULE_NOT_FOUND);
        }

        schedulerManager.cancelRule(ruleId);
        ruleRepository.deleteById(ruleId);

        log.info("Xóa thành công Rule ID: {} khỏi hệ thống.", ruleId);
    }

    private void validatePipeline(List<PipelineStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new AppException(ErrorCode.ALERT_RULE_INVALID_PIPELINE);
        }

        if (!"FETCH_ES_DATA".equals(steps.get(0).getType())) {
            throw new AppException(ErrorCode.ALERT_RULE_INVALID_FIRST_STEP);
        }
    }
}