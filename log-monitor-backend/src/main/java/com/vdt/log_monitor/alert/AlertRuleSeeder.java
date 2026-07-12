// package com.vdt.log_monitor.alert;

// import com.vdt.log_monitor.alert.model.PipelineStep;
// import com.vdt.log_monitor.alert.model.RuleConfig;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.CommandLineRunner;
// import org.springframework.core.annotation.Order;
// import org.springframework.stereotype.Component;

// import java.util.List;
// import java.util.Map;

// @Slf4j
// @Component
// @RequiredArgsConstructor
// @Order(100) // chạy sau các initializer khác nếu có
// public class AlertRuleSeeder implements CommandLineRunner {

//     private static final int RULE_COUNT = 30;
//     private static final String NAME_PREFIX = "Error Spike ";

//     private final AlertRuleService alertRuleService;
//     private final AlertRuleRepository ruleRepository;

//     @Override
//     public void run(String... args) {
//         long existing = ruleRepository.findAll().stream()
//                 .filter(r -> r.getName() != null && r.getName().startsWith(NAME_PREFIX))
//                 .count();

//         if (existing >= RULE_COUNT) {
//             log.info("⏭️ Đã có {} rule '{}*' trong hệ thống -> bỏ qua seeding.", existing, NAME_PREFIX);
//             return;
//         }

//         log.info("🌱 Bắt đầu seed {} rule test tải cho Alert Engine...", RULE_COUNT);

//         for (int i = 1; i <= RULE_COUNT; i++) {
//             try {
//                 alertRuleService.createRule(buildRule(i));
//                 log.info("✅ Đã tạo rule seed #{}", i);
//             } catch (Exception e) {
//                 log.error("❌ Lỗi khi tạo rule seed #{}: {}", i, e.getMessage(), e);
//             }
//         }

//         log.info("🏁 Hoàn tất seed {} rule.", RULE_COUNT);
//     }

//     private RuleConfig buildRule(int index) {
//         PipelineStep fetchStep = new PipelineStep();
//         fetchStep.setId("fetch_errors");
//         fetchStep.setType("FETCH_ES_DATA");
//         fetchStep.setParams(Map.of(
//                 "index", "sys-logs-*",
//                 "query", "level:(ERROR OR FATAL)",
//                 "metricType", "COUNT",
//                 "lookBackMinutes", 2,
//                 "timeField", "@timestamp"));

//         PipelineStep thresholdStep = new PipelineStep();
//         thresholdStep.setId("check_spike");
//         thresholdStep.setType("EVALUATE_THRESHOLD");
//         thresholdStep.setParams(Map.of(
//                 "input", "fetch_errors",
//                 "operator", "GREATER_THAN",
//                 "value", 1));

//         RuleConfig.NotificationTemplate template = new RuleConfig.NotificationTemplate();
//         template.setTitle("Error Spike " + index);
//         template.setMessage("Số lượng ERROR/FATAL vượt quá 3 trong 1 phút. (rule #" + index + ")");

//         RuleConfig rule = new RuleConfig();
//         rule.setName(NAME_PREFIX + index);
//         rule.setIntervalMinutes(1);
//         rule.setIsActive(true);
//         rule.setRepeatIntervalMinutes(3);
//         rule.setTriggerStepId("check_spike");
//         rule.setPipelineSteps(List.of(fetchStep, thresholdStep));
//         rule.setNotificationTemplate(template);

//         return rule;
//     }
// }