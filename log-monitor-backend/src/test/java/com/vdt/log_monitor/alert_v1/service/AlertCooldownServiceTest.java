//package com.vdt.log_monitor.alert.service;
//import com.vdt.log_monitor.alert.dto.AlertRule;
//import com.vdt.log_monitor.alert.dto.AlertRuleType;
//import com.vdt.log_monitor.alert.dto.AlertSeverity;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
///**
// * Unit tests for {@link AlertCooldownService}.
// * Verifies cooldown logic: first trigger, within cooldown, and after cooldown expiry.
// */
//class AlertCooldownServiceTest {
//    private AlertCooldownService cooldownService;
//    @BeforeEach
//    void setUp() {
//        cooldownService = new AlertCooldownService();
//    }
//    private AlertRule createRule(String id, long cooldownMinutes) {
//        return AlertRule.builder()
//                .id(id)
//                .type(AlertRuleType.COUNT_THRESHOLD)
//                .enabled(true)
//                .environment("*")
//                .application("*")
//                .level("ERROR")
//                .threshold(10)
//                .windowMinutes(5)
//                .cooldownMinutes(cooldownMinutes)
//                .severity(AlertSeverity.HIGH)
//                .build();
//    }
//    @Test
//    @DisplayName("Should allow trigger when rule has never been triggered")
//    void shouldAllowFirstTrigger() {
//        AlertRule rule = createRule("TEST_RULE", 10);
//        assertTrue(cooldownService.canTrigger(rule));
//    }
//    @Test
//    @DisplayName("Should block trigger immediately after being triggered (within cooldown)")
//    void shouldBlockDuringCooldown() {
//        AlertRule rule = createRule("TEST_RULE", 10);
//        // First trigger
//        cooldownService.markTriggered(rule);
//        // Should be blocked — cooldown is 10 minutes and we just triggered
//        assertFalse(cooldownService.canTrigger(rule));
//    }
//    @Test
//    @DisplayName("Should allow trigger when cooldown is 0 minutes")
//    void shouldAllowWithZeroCooldown() {
//        AlertRule rule = createRule("TEST_RULE", 0);
//        cooldownService.markTriggered(rule);
//        // Cooldown is 0 minutes, so it should be immediately re-triggerable
//        assertTrue(cooldownService.canTrigger(rule));
//    }
//    @Test
//    @DisplayName("Should track cooldowns independently per rule ID")
//    void shouldTrackCooldownsPerRule() {
//        AlertRule rule1 = createRule("RULE_1", 10);
//        AlertRule rule2 = createRule("RULE_2", 10);
//        cooldownService.markTriggered(rule1);
//        // Rule 1 is in cooldown, but Rule 2 has never been triggered
//        assertFalse(cooldownService.canTrigger(rule1));
//        assertTrue(cooldownService.canTrigger(rule2));
//    }
//}