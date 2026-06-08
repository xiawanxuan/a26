package com.iot.monitor.common.repository;

import com.iot.monitor.common.entity.AlertNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertNotificationRepository extends JpaRepository<AlertNotification, Long> {

    List<AlertNotification> findByAlertRecordIdOrderByCreateTimeDesc(Long alertRecordId);

    List<AlertNotification> findByChannelAndStatus(String channel, String status);
}
