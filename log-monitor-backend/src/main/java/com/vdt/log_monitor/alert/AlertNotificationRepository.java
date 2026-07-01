package com.vdt.log_monitor.alert;

import com.vdt.log_monitor.alert.model.AlertNotificationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertNotificationRepository extends ElasticsearchRepository<AlertNotificationDocument, String> {
    List<AlertNotificationDocument> findByRuleIdOrderByTimestampDesc(String ruleId);
}