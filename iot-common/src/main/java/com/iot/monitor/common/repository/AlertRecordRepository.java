package com.iot.monitor.common.repository;

import com.iot.monitor.common.entity.AlertRecord;
import com.iot.monitor.common.enums.AlertLevel;
import com.iot.monitor.common.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long>, JpaSpecificationExecutor<AlertRecord> {

    Optional<AlertRecord> findByAlertCode(String alertCode);

    Page<AlertRecord> findByDeviceCodeOrderByAlertTimeDesc(String deviceCode, Pageable pageable);

    Page<AlertRecord> findByStatusOrderByAlertTimeDesc(AlertStatus status, Pageable pageable);

    Page<AlertRecord> findByAlertLevelOrderByAlertTimeDesc(AlertLevel alertLevel, Pageable pageable);

    List<AlertRecord> findByDeviceCodeAndStatusAndMetricAndRuleId(
            String deviceCode, AlertStatus status, String metric, Long ruleId);

    @Query("SELECT COUNT(a) FROM AlertRecord a WHERE a.status = :status")
    long countByStatus(@Param("status") AlertStatus status);

    @Query("SELECT COUNT(a) FROM AlertRecord a WHERE a.alertLevel = :level AND a.alertTime >= :startTime")
    long countByLevelAndTimeAfter(@Param("level") AlertLevel level, @Param("startTime") LocalDateTime startTime);

    List<AlertRecord> findByDeviceCodeAndStatusInAndMetric(
            String deviceCode, List<AlertStatus> statuses, String metric);
}
