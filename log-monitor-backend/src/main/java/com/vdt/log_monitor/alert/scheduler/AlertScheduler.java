package com.vdt.log_monitor.alert.scheduler;
import com.vdt.log_monitor.alert.service.AlertEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/**
 * Periodically triggers alert rule evaluation against Elasticsearch.
 * <p>
 * Uses {@code fixedDelay = 30000} so the next execution starts 30 seconds
 * after the previous execution completes, preventing overlapping evaluations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertScheduler {
    private final AlertEvaluationService alertEvaluationService;
    @Scheduled(fixedDelay = 30000)
    public void runAlertEvaluation() {
        log.debug("Alert evaluation cycle started");
        alertEvaluationService.evaluateRules();
        log.debug("Alert evaluation cycle completed");
    }
}