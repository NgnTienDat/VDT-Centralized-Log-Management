package com.vdt.log_monitor.alert;

import com.vdt.log_monitor.alert.enums.AlertState;
import com.vdt.log_monitor.alert.model.RuleConfig;
import com.vdt.log_monitor.alert.scheduler.AlertSchedulerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository ruleRepository;
    private final AlertSchedulerManager schedulerManager;

    public RuleConfig createRule(RuleConfig ruleConfig) {
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

    // 2. SỬA RULE (Thay đổi chu kỳ interval, bộ lọc, hoặc Bật/Tắt)
    public RuleConfig updateRule(String ruleId, RuleConfig updatedConfig) {
        RuleConfig existingRule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Rule với ID: " + ruleId));

        existingRule.setName(updatedConfig.getName());
        existingRule.setIntervalMinutes(updatedConfig.getIntervalMinutes());
        existingRule.setIsActive(updatedConfig.getIsActive());
        existingRule.setPipelineSteps(updatedConfig.getPipelineSteps());

        RuleConfig savedRule = ruleRepository.save(existingRule);

        // TỰ ĐỘNG CẬP NHẬT LẠI ĐỒNG HỒ THEO CẤU HÌNH MỚI (Hủy lịch cũ, lên lịch mới)
        schedulerManager.scheduleRule(savedRule);

        return savedRule;
    }

    // 3. XÓA RULE
    public void deleteRule(String ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new RuntimeException("Không tìm thấy Rule với ID: " + ruleId);
        }

        // Gỡ bỏ đồng hồ hẹn giờ ra khỏi Thread Pool trước khi xóa khỏi Database
        schedulerManager.cancelRule(ruleId);

        ruleRepository.deleteById(ruleId);
        log.info("Xóa thành công Rule ID: {} khỏi hệ thống.", ruleId);
    }
}