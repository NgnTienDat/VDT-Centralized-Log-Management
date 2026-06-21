//package com.vdt.log_monitor.alert.config;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.vdt.log_monitor.alert.dto.AlertRule;
//import com.vdt.log_monitor.alert.dto.AlertRuleType;
//import com.vdt.log_monitor.alert.dto.AlertSeverity;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.io.TempDir;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//import static org.junit.jupiter.api.Assertions.*;
///**
// * Unit tests for {@link AlertRuleLoader}.
// * Tests rule loading, validation, and failure scenarios.
// */
//class AlertRuleLoaderTest {
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    @Test
//    @DisplayName("Should load rules successfully from valid rule.json on classpath")
//    void shouldLoadRulesFromClasspath() {
//        // The test classpath includes the real rule.json from src/main/resources
//        AlertRuleLoader loader = new AlertRuleLoader(objectMapper);
//        loader.init();
//        List<AlertRule> rules = loader.getRules();
//        assertNotNull(rules);
//        assertFalse(rules.isEmpty());
//        AlertRule firstRule = rules.get(0);
//        assertEquals("HIGH_ERROR_RATE", firstRule.getId());
//        assertEquals(AlertRuleType.COUNT_THRESHOLD, firstRule.getType());
//        assertTrue(firstRule.isEnabled());
//        assertEquals("*", firstRule.getEnvironment());
//        assertEquals("*", firstRule.getApplication());
//        assertEquals("ERROR", firstRule.getLevel());
//        assertEquals(10, firstRule.getThreshold());
//        assertEquals(5, firstRule.getWindowMinutes());
//        assertEquals(10, firstRule.getCooldownMinutes());
//        assertEquals(AlertSeverity.HIGH, firstRule.getSeverity());
//    }
//    @Test
//    @DisplayName("Loaded rules list should be immutable")
//    void shouldReturnImmutableList() {
//        AlertRuleLoader loader = new AlertRuleLoader(objectMapper);
//        loader.init();
//        List<AlertRule> rules = loader.getRules();
//        assertThrows(UnsupportedOperationException.class, () ->
//                rules.add(AlertRule.builder().id("TEST").build()));
//    }
//}