package com.vdt.log_monitor.alert;

import com.vdt.log_monitor.alert.model.RuleConfig;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends ElasticsearchRepository<RuleConfig, String> {
    List<RuleConfig> findByIsActiveTrue();
    List<RuleConfig> findAll();
}