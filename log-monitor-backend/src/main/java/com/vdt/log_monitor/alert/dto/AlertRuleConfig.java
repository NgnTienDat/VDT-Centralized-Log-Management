package com.vdt.log_monitor.alert.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
/**
 * Root configuration object deserialized from {@code rule.json}.
 * Contains the list of all alert rules.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleConfig {
    private List<AlertRule> rules;
}
