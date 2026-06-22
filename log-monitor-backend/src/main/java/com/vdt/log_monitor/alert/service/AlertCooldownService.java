package com.vdt.log_monitor.alert.service;

import com.vdt.log_monitor.alert.dto.AlertRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents alert spam (Alert Fatigue) by enforcing a cooldown period per rule.
 * <p>
 * Uses a thread-safe in-memory cache ({@link ConcurrentHashMap}) to track
 * the last trigger time for each rule ID.
 */
@Slf4j
@Service
public class AlertCooldownService {

    private final ConcurrentHashMap<String, Instant> lastTriggerMap = new ConcurrentHashMap<>();

    /**
     * Checks whether the given rule is eligible to trigger an alert.
     * Returns {@code true} if the rule has never been triggered, or if
     * the cooldown period has elapsed since the last trigger.
     *
     * @param rule the alert rule to check
     * @return {@code true} if the rule can trigger, {@code false} if still in cooldown
     */
    public boolean canTrigger(AlertRule rule) {
        Instant lastTriggered = lastTriggerMap.get(rule.getId());
        if (lastTriggered == null) return true;

        Instant now = Instant.now(); // gọi 1 lần duy nhất
        Instant cooldownEnd = lastTriggered.plus(rule.getCooldownMinutes(), ChronoUnit.MINUTES);
        return !now.isBefore(cooldownEnd); // isAfter || isEqual, đúng chuẩn
    }

    /**
     * Records the current time as the last trigger time for the given rule.
     *
     * @param rule the alert rule that was triggered
     */
    public void markTriggered(AlertRule rule) {
        lastTriggerMap.put(rule.getId(), Instant.now());
        log.debug("Cooldown updated for rule [{}]: next trigger allowed after {} minutes",
                rule.getId(), rule.getCooldownMinutes());
    }

    public boolean tryAcquire(AlertRule rule) {
        Instant now = Instant.now();
        boolean[] allowed = {false};

        lastTriggerMap.compute(rule.getId(), (key, lastTriggered) -> {
            if (lastTriggered == null) {
                allowed[0] = true;
                return now;
            }
            Instant cooldownEnd = lastTriggered.plus(rule.getCooldownMinutes(), ChronoUnit.MINUTES);
            if (!now.isBefore(cooldownEnd)) {
                allowed[0] = true;
                return now; // reset cooldown timer atomically
            }
            return lastTriggered; // còn cooldown, giữ nguyên
        });

        return allowed[0];
    }

    // Thêm hàm này vào class AlertCooldownService
    public Instant getLastTriggeredTime(String ruleId) {
        return lastTriggerMap.get(ruleId);
    }
}
