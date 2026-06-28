//package com.vdt.log_monitor.alert.service;
//
//import com.vdt.log_monitor.alert.config.AlertRuleLoader;
//import com.vdt.log_monitor.alert.dto.AlertEvent;
//import com.vdt.log_monitor.alert.dto.AlertRule;
//import com.vdt.log_monitor.alert.dto.AlertRuleType;
//import com.vdt.log_monitor.alert.dto.AlertSeverity;
//import com.vdt.log_monitor.alert.repository.AlertQueryRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
///**
// * Unit tests for {@link AlertEvaluationService}.
// * Mocks all dependencies to test the orchestration logic in isolation.
// */
//@ExtendWith(MockitoExtension.class)
//class AlertEvaluationServiceTest {
//    @Mock
//    private AlertRuleLoader alertRuleLoader;
//    @Mock
//    private AlertQueryRepository alertQueryRepository;
//    @Mock
//    private AlertCooldownService alertCooldownService;
//    @Mock
//    private AlertWebSocketPublisher alertWebSocketPublisher;
//    @InjectMocks
//    private AlertEvaluationService alertEvaluationService;
//    private AlertRule enabledRule;
//    private AlertRule disabledRule;
//
//    @BeforeEach
//    void setUp() {
//        enabledRule = AlertRule.builder()
//                .id("HIGH_ERROR_RATE")
//                .type(AlertRuleType.COUNT_THRESHOLD)
//                .enabled(true)
//                .environment("*")
//                .application("*")
//                .level("ERROR")
//                .threshold(10)
//                .windowMinutes(5)
//                .cooldownMinutes(10)
//                .severity(AlertSeverity.HIGH)
//                .build();
//        disabledRule = AlertRule.builder()
//                .id("DISABLED_RULE")
//                .type(AlertRuleType.COUNT_THRESHOLD)
//                .enabled(false)
//                .level("WARN")
//                .threshold(50)
//                .windowMinutes(10)
//                .cooldownMinutes(15)
//                .severity(AlertSeverity.LOW)
//                .build();
//    }
//
//    @Test
//    @DisplayName("Should skip disabled rules without querying ES")
//    void shouldSkipDisabledRules() {
//        when(alertRuleLoader.getRules()).thenReturn(List.of(disabledRule));
//        alertEvaluationService.evaluateRules();
//        verify(alertQueryRepository, never()).countMatchingLogs(any());
//        verify(alertWebSocketPublisher, never()).publish(any());
//    }
//
//    @Test
//    @DisplayName("Should not trigger alert when count is below threshold")
//    void shouldNotTriggerWhenBelowThreshold() {
//        when(alertRuleLoader.getRules()).thenReturn(List.of(enabledRule));
//        when(alertQueryRepository.countMatchingLogs(enabledRule)).thenReturn(5L);
//        alertEvaluationService.evaluateRules();
//        verify(alertCooldownService, never()).canTrigger(any());
//        verify(alertWebSocketPublisher, never()).publish(any());
//    }
//
//    @Test
//    @DisplayName("Should trigger alert when threshold exceeded and no cooldown active")
//    void shouldTriggerWhenThresholdExceededAndNoCooldown() {
//        when(alertRuleLoader.getRules()).thenReturn(List.of(enabledRule));
//        when(alertQueryRepository.countMatchingLogs(enabledRule)).thenReturn(15L);
//        when(alertCooldownService.canTrigger(enabledRule)).thenReturn(true);
//        alertEvaluationService.evaluateRules();
//        ArgumentCaptor<AlertEvent> eventCaptor = ArgumentCaptor.forClass(AlertEvent.class);
//        verify(alertWebSocketPublisher).publish(eventCaptor.capture());
//        verify(alertCooldownService).markTriggered(enabledRule);
//        AlertEvent event = eventCaptor.getValue();
//        assertEquals("HIGH_ERROR_RATE", event.getRuleId());
//        assertEquals(15L, event.getMatchedCount());
//        assertEquals("HIGH", event.getSeverity());
//        assertEquals("*", event.getEnvironment());
//        assertEquals("*", event.getApplication());
//        assertTrue(event.getMessage().contains("15"));
//        assertTrue(event.getMessage().contains("ERROR"));
//        assertTrue(event.getMessage().contains("5 minutes"));
//        assertNotNull(event.getTriggeredAt());
//    }
//
//    @Test
//    @DisplayName("Should not trigger alert when threshold exceeded but cooldown is active")
//    void shouldNotTriggerWhenCooldownActive() {
//        when(alertRuleLoader.getRules()).thenReturn(List.of(enabledRule));
//        when(alertQueryRepository.countMatchingLogs(enabledRule)).thenReturn(20L);
//        when(alertCooldownService.canTrigger(enabledRule)).thenReturn(false);
//        alertEvaluationService.evaluateRules();
//        verify(alertWebSocketPublisher, never()).publish(any());
//        verify(alertCooldownService, never()).markTriggered(any());
//    }
//
//    @Test
//    @DisplayName("Should handle ES query exceptions gracefully without stopping evaluation")
//    void shouldHandleExceptionsGracefully() {
//        AlertRule secondRule = AlertRule.builder()
//                .id("SECOND_RULE")
//                .type(AlertRuleType.COUNT_THRESHOLD)
//                .enabled(true)
//                .level("ERROR")
//                .threshold(5)
//                .windowMinutes(3)
//                .cooldownMinutes(5)
//                .severity(AlertSeverity.CRITICAL)
//                .build();
//        when(alertRuleLoader.getRules()).thenReturn(List.of(enabledRule, secondRule));
//        when(alertQueryRepository.countMatchingLogs(enabledRule))
//                .thenThrow(new RuntimeException("ES connection failed"));
//        when(alertQueryRepository.countMatchingLogs(secondRule)).thenReturn(10L);
//        when(alertCooldownService.canTrigger(secondRule)).thenReturn(true);
//        // Should not throw — the first rule's error should be caught,
//        // and the second rule should still be evaluated
//        assertDoesNotThrow(() -> alertEvaluationService.evaluateRules());
//        verify(alertWebSocketPublisher).publish(any());
//        verify(alertCooldownService).markTriggered(secondRule);
//    }
//
//    @Test
//    @DisplayName("Should trigger alert at exact threshold boundary (count == threshold)")
//    void shouldTriggerAtExactThreshold() {
//        when(alertRuleLoader.getRules()).thenReturn(List.of(enabledRule));
//        when(alertQueryRepository.countMatchingLogs(enabledRule)).thenReturn(10L); // exactly threshold
//        when(alertCooldownService.canTrigger(enabledRule)).thenReturn(true);
//        alertEvaluationService.evaluateRules();
//        // count (10) is NOT < threshold (10), so it should proceed to cooldown check and trigger
//        verify(alertWebSocketPublisher).publish(any());
//        verify(alertCooldownService).markTriggered(enabledRule);
//    }
//}