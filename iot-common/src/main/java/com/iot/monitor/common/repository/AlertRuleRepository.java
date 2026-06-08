package com.iot.monitor.common.repository;

import com.iot.monitor.common.entity.AlertRule;
import com.iot.monitor.common.enums.AlertLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long>, JpaSpecificationExecutor<AlertRule> {

    List<AlertRule> findByEnabledTrue();

    List<AlertRule> findByDeviceCodeAndEnabledTrue(String deviceCode);

    List<AlertRule> findByDeviceTypeAndEnabledTrue(String deviceType);

    List<AlertRule> findByMetricAndEnabledTrue(String metric);

    List<AlertRule> findByAlertLevel(AlertLevel alertLevel);
}
