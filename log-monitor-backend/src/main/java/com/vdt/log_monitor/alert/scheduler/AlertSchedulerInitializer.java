package com.vdt.log_monitor.alert.scheduler;

import com.vdt.log_monitor.alert.model.RuleConfig;
import com.vdt.log_monitor.alert.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertSchedulerInitializer implements CommandLineRunner {

    private final AlertRuleRepository ruleRepository;
    private final AlertSchedulerManager schedulerManager;

    @Override
    public void run(String... args) {
        log.info("⚙️ Đang bốc các Rule hoạt động từ Elasticsearch để cài đặt đồng hồ hẹn giờ...");
        List<RuleConfig> activeRules = ruleRepository.findByIsActiveTrue();

        for (RuleConfig rule : activeRules) {
            schedulerManager.scheduleRule(rule);
        }
        log.info("✅ Đã kích hoạt thành công {} đồng hồ hẹn giờ.", activeRules.size());
    }
}