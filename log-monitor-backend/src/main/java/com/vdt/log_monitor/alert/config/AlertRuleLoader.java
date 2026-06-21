package com.vdt.log_monitor.alert.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.log_monitor.alert.dto.AlertRule;
import com.vdt.log_monitor.alert.dto.AlertRuleConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AlertRuleLoader {

    // Khởi tạo trực tiếp thay vì chờ Spring Inject
    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<AlertRule> rules;

    // Xóa constructor injection hiện tại đi
    // public AlertRuleLoader(ObjectMapper objectMapper) {
    //     this.objectMapper = objectMapper;
    // }

    @PostConstruct
    public void init() {
        try (InputStream inputStream = new ClassPathResource("rule.json").getInputStream()) {
            AlertRuleConfig config = objectMapper.readValue(inputStream, AlertRuleConfig.class);
            log.info("Loading alert rules from {}", config);
            if (config == null || config.getRules() == null || config.getRules().isEmpty()) {
                throw new IllegalStateException("rule.json is empty or contains no rules");
            }

            this.rules = Collections.unmodifiableList(config.getRules());
            log.info("Loaded {} alert rule(s) from rule.json", rules.size());
            // log...
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load rule.json from classpath", e);
        }
    }

    public List<AlertRule> getRules() {
        return rules;
    }
}