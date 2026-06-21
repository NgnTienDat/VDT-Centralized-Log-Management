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
        if (lastTriggered == null) {
            return true;
        }
        Instant cooldownEnd = lastTriggered.plus(rule.getCooldownMinutes(), ChronoUnit.MINUTES);
        return Instant.now().isAfter(cooldownEnd) || Instant.now().equals(cooldownEnd);
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
}
