package com.iot.monitor.alert.service;

import com.iot.monitor.common.dto.SensorDataDTO;
import com.iot.monitor.common.entity.AlertRecord;
import com.iot.monitor.common.entity.AlertRule;
import com.iot.monitor.common.entity.SensorData;
import com.iot.monitor.common.enums.AlertStatus;
import com.iot.monitor.common.enums.AlertType;
import com.iot.monitor.common.event.AlertTriggeredEvent;
import com.iot.monitor.common.event.SensorDataReceivedEvent;
import com.iot.monitor.common.repository.AlertRecordRepository;
import com.iot.monitor.common.repository.SensorDataRepository;
import com.iot.monitor.common.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertDetectionService {

    private final AlertRuleService alertRuleService;
    private final AlertRecordRepository alertRecordRepository;
    private final SensorDataRepository sensorDataRepository;
    private final AlertNotificationService alertNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, Integer> consecutiveCountMap = new ConcurrentHashMap<>();

    @EventListener
    @Async
    public void onSensorDataReceived(SensorDataReceivedEvent event) {
        SensorDataDTO sensorData = event.getSensorData();
        detectAlerts(sensorData);
    }

    @Transactional
    public void detectAlerts(SensorDataDTO sensorData) {
        List<AlertRule> rules = alertRuleService.getApplicableRules(
                sensorData.getDeviceCode(), sensorData.getMetric());

        for (AlertRule rule : rules) {
            try {
                checkRule(rule, sensorData);
            } catch (Exception e) {
                log.error("Error checking alert rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }
    }

    private void checkRule(AlertRule rule, SensorDataDTO sensorData) {
        if (rule.getAlertType() == AlertType.THRESHOLD_EXCEEDED) {
            checkThresholdRule(rule, sensorData);
        } else if (rule.getAlertType() == AlertType.DATA_ABNORMAL) {
            checkDataAbnormalRule(rule, sensorData);
        }
    }

    private void checkThresholdRule(AlertRule rule, SensorDataDTO sensorData) {
        BigDecimal value = sensorData.getValue();
        boolean isAlert = false;
        String reason = "";

        String operator = rule.getComparisonOperator();
        BigDecimal threshold = rule.getThresholdValue();

        if (operator != null && threshold != null) {
            switch (operator) {
                case ">" -> {
                    isAlert = value.compareTo(threshold) > 0;
                    reason = "超过上限阈值";
                }
                case ">=" -> {
                    isAlert = value.compareTo(threshold) >= 0;
                    reason = "超过或等于上限阈值";
                }
                case "<" -> {
                    isAlert = value.compareTo(threshold) < 0;
                    reason = "低于下限阈值";
                }
                case "<=" -> {
                    isAlert = value.compareTo(threshold) <= 0;
                    reason = "低于或等于下限阈值";
                }
                case "=" -> {
                    isAlert = value.compareTo(threshold) == 0;
                    reason = "等于阈值";
                }
                case "!=" -> {
                    isAlert = value.compareTo(threshold) != 0;
                    reason = "不等于阈值";
                }
            }
        }

        if (rule.getMinThreshold() != null && rule.getMaxThreshold() != null) {
            isAlert = value.compareTo(rule.getMinThreshold()) < 0
                    || value.compareTo(rule.getMaxThreshold()) > 0;
            reason = "超出正常范围";
        }

        String countKey = rule.getId() + ":" + sensorData.getDeviceCode() + ":" + sensorData.getMetric();

        if (isAlert) {
            int count = consecutiveCountMap.getOrDefault(countKey, 0) + 1;
            consecutiveCountMap.put(countKey, count);

            int requiredCount = rule.getConsecutiveTimes() != null ? rule.getConsecutiveTimes() : 1;

            if (count >= requiredCount) {
                triggerAlert(rule, sensorData, reason);
            }
        } else {
            consecutiveCountMap.remove(countKey);
            checkRecovery(rule, sensorData);
        }
    }

    private void checkDataAbnormalRule(AlertRule rule, SensorDataDTO sensorData) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(30);
        List<SensorData> recentData = sensorDataRepository.findLatestByDeviceAndMetric(
                sensorData.getDeviceCode(), sensorData.getMetric(), startTime, PageRequest.of(0, 100));

        if (recentData.size() < 10) {
            return;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (SensorData data : recentData) {
            sum = sum.add(data.getValue());
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(recentData.size()), 6, java.math.RoundingMode.HALF_UP);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (SensorData data : recentData) {
            BigDecimal diff = data.getValue().subtract(avg);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(recentData.size()), 6, java.math.RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        BigDecimal deviation = sensorData.getValue().subtract(avg).abs();

        if (stdDev.compareTo(BigDecimal.ZERO) > 0
                && deviation.divide(stdDev, 2, java.math.RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(3)) > 0) {
            triggerAlert(rule, sensorData, "数据异常波动");
        }
    }

    private void triggerAlert(AlertRule rule, SensorDataDTO sensorData, String reason) {
        List<AlertRecord> existingRecords = alertRecordRepository
                .findByDeviceCodeAndStatusInAndMetric(
                        sensorData.getDeviceCode(),
                        List.of(AlertStatus.PENDING, AlertStatus.ACTIVE),
                        sensorData.getMetric());

        boolean hasActiveAlert = existingRecords.stream()
                .anyMatch(r -> r.getRuleId() != null && r.getRuleId().equals(rule.getId()));

        if (hasActiveAlert) {
            return;
        }

        AlertRecord alertRecord = new AlertRecord();
        alertRecord.setAlertCode(IdGenerator.generateAlertCode());
        alertRecord.setDeviceCode(sensorData.getDeviceCode());
        alertRecord.setMetric(sensorData.getMetric());
        alertRecord.setRuleId(rule.getId());
        alertRecord.setAlertType(rule.getAlertType());
        alertRecord.setAlertLevel(rule.getAlertLevel());
        alertRecord.setStatus(AlertStatus.ACTIVE);
        alertRecord.setTitle(rule.getRuleName());

        String content = buildAlertContent(rule, sensorData, reason);
        alertRecord.setContent(content);
        alertRecord.setCurrentValue(sensorData.getValue());
        alertRecord.setThresholdValue(rule.getThresholdValue());
        alertRecord.setAlertTime(LocalDateTime.now());
        alertRecord.setNotificationCount(0);

        alertRecord = alertRecordRepository.save(alertRecord);

        alertNotificationService.sendNotifications(alertRecord, rule);

        eventPublisher.publishEvent(new AlertTriggeredEvent(this, alertRecord));

        log.warn("Alert triggered: {} - {} - {}", alertRecord.getAlertCode(),
                sensorData.getDeviceCode(), reason);
    }

    private String buildAlertContent(AlertRule rule, SensorDataDTO sensorData, String reason) {
        String template = rule.getMessageTemplate();
        if (template == null || template.isEmpty()) {
            return String.format("设备[%s]的指标[%s]发生告警：%s。当前值：%s，阈值：%s",
                    sensorData.getDeviceCode(), sensorData.getMetric(), reason,
                    sensorData.getValue(), rule.getThresholdValue());
        }
        return template
                .replace("{deviceCode}", sensorData.getDeviceCode())
                .replace("{metric}", sensorData.getMetric())
                .replace("{value}", sensorData.getValue().toString())
                .replace("{threshold}", rule.getThresholdValue() != null ? rule.getThresholdValue().toString() : "")
                .replace("{reason}", reason);
    }

    private void checkRecovery(AlertRule rule, SensorDataDTO sensorData) {
        if (!Boolean.TRUE.equals(rule.getRecoverable())) {
            return;
        }

        List<AlertRecord> activeRecords = alertRecordRepository
                .findByDeviceCodeAndStatusInAndMetric(
                        sensorData.getDeviceCode(),
                        List.of(AlertStatus.ACTIVE),
                        sensorData.getMetric());

        for (AlertRecord record : activeRecords) {
            if (record.getRuleId() != null && record.getRuleId().equals(rule.getId())) {
                record.setStatus(AlertStatus.RESOLVED);
                record.setRecoverTime(LocalDateTime.now());
                alertRecordRepository.save(record);
                log.info("Alert recovered: {}", record.getAlertCode());
            }
        }
    }
}
