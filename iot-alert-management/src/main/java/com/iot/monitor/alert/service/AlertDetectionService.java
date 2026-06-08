package com.iot.monitor.alert.service;

import com.iot.monitor.aggregation.service.DataAggregationService;
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
import java.math.RoundingMode;
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
    private final DataAggregationService dataAggregationService;

    private final Map<String, Integer> consecutiveCountMap = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lastValueMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastTimeMap = new ConcurrentHashMap<>();

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

        String key = sensorData.getDeviceCode() + ":" + sensorData.getMetric();
        BigDecimal previousValue = lastValueMap.get(key);
        LocalDateTime previousTime = lastTimeMap.get(key);

        for (AlertRule rule : rules) {
            try {
                checkRule(rule, sensorData, previousValue, previousTime);
            } catch (Exception e) {
                log.error("Error checking alert rule {}: {}", rule.getId(), e.getMessage(), e);
            }
        }

        lastValueMap.put(key, sensorData.getValue());
        lastTimeMap.put(key, LocalDateTime.now());
    }

    private void checkRule(AlertRule rule, SensorDataDTO sensorData,
                           BigDecimal previousValue, LocalDateTime previousTime) {
        AlertType type = rule.getAlertType();

        switch (type) {
            case THRESHOLD_EXCEEDED -> checkThresholdRule(rule, sensorData);
            case DYNAMIC_THRESHOLD -> checkDynamicThresholdRule(rule, sensorData);
            case MOM_COMPARISON -> checkMomRule(rule, sensorData, previousValue);
            case YOY_COMPARISON -> checkYoyRule(rule, sensorData);
            case RATE_OF_CHANGE -> checkRateOfChangeRule(rule, sensorData, previousValue, previousTime);
            case TREND_ANOMALY -> checkTrendAnomalyRule(rule, sensorData);
            case DATA_ABNORMAL -> checkDataAbnormalRule(rule, sensorData);
            default -> log.debug("Unsupported alert type: {}", type);
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
            reason = "超出正常范围 [" + rule.getMinThreshold() + ", " + rule.getMaxThreshold() + "]";
        }

        handleAlertCheck(rule, sensorData, isAlert, reason, threshold);
    }

    private void checkDynamicThresholdRule(AlertRule rule, SensorDataDTO sensorData) {
        String deviceCode = sensorData.getDeviceCode();
        String metric = sensorData.getMetric();
        BigDecimal value = sensorData.getValue();

        int windowSeconds = rule.getBaselineWindowSize() != null ? rule.getBaselineWindowSize() : 300;
        double multiplier = rule.getStdDevMultiplier() != null ? rule.getStdDevMultiplier().doubleValue() : 3.0;

        BigDecimal dynamicThreshold = dataAggregationService.getDynamicThreshold(
                deviceCode, metric, windowSeconds, multiplier);

        if (dynamicThreshold == null) {
            return;
        }

        boolean isAlert;
        String reason;
        String operator = rule.getComparisonOperator();

        if ("<".equals(operator) || "<=".equals(operator)) {
            BigDecimal lowerThreshold = dataAggregationService.getDynamicThreshold(
                    deviceCode, metric, windowSeconds, -multiplier);
            isAlert = lowerThreshold != null && value.compareTo(lowerThreshold) < 0;
            reason = String.format("低于动态下限阈值(%.3fσ)", multiplier);
        } else {
            isAlert = value.compareTo(dynamicThreshold) > 0;
            reason = String.format("超过动态上限阈值(%.3fσ)", multiplier);
        }

        handleAlertCheck(rule, sensorData, isAlert, reason, dynamicThreshold);
    }

    private void checkMomRule(AlertRule rule, SensorDataDTO sensorData, BigDecimal previousValue) {
        if (previousValue == null) {
            return;
        }

        BigDecimal value = sensorData.getValue();
        BigDecimal momThreshold = rule.getMomThreshold() != null ? rule.getMomThreshold() : new BigDecimal("10");

        BigDecimal change = value.subtract(previousValue);
        BigDecimal changePercent = previousValue.compareTo(BigDecimal.ZERO) != 0
                ? change.divide(previousValue.abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        boolean isAlert;
        String reason;
        String direction = rule.getTrendDirection();

        if ("rising".equals(direction)) {
            isAlert = changePercent.compareTo(momThreshold) > 0;
            reason = String.format("环比上升 %.2f%%，超过阈值 %.2f%%", changePercent, momThreshold);
        } else if ("falling".equals(direction)) {
            isAlert = changePercent.compareTo(momThreshold.negate()) < 0;
            reason = String.format("环比下降 %.2f%%，超过阈值 %.2f%%", changePercent.abs(), momThreshold);
        } else {
            boolean useAbs = Boolean.TRUE.equals(rule.getUseAbsoluteValue());
            BigDecimal compareValue = useAbs ? changePercent.abs() : changePercent;
            BigDecimal threshold = useAbs ? momThreshold : momThreshold.abs();
            isAlert = compareValue.compareTo(threshold) > 0;
            reason = String.format("环比变化 %.2f%%，超过阈值 %.2f%%", changePercent, momThreshold);
        }

        handleAlertCheck(rule, sensorData, isAlert, reason, previousValue);
    }

    private void checkYoyRule(AlertRule rule, SensorDataDTO sensorData) {
        String key = sensorData.getDeviceCode() + ":" + sensorData.getMetric();
        BigDecimal yoyValue = dataAggregationService.getYoyValue(sensorData.getDeviceCode(), sensorData.getMetric());

        if (yoyValue == null) {
            return;
        }

        BigDecimal value = sensorData.getValue();
        BigDecimal yoyThreshold = rule.getYoyThreshold() != null ? rule.getYoyThreshold() : new BigDecimal("20");

        BigDecimal change = value.subtract(yoyValue);
        BigDecimal changePercent = yoyValue.compareTo(BigDecimal.ZERO) != 0
                ? change.divide(yoyValue.abs(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        boolean isAlert;
        String reason;
        String direction = rule.getTrendDirection();

        if ("rising".equals(direction)) {
            isAlert = changePercent.compareTo(yoyThreshold) > 0;
            reason = String.format("同比上升 %.2f%%，超过阈值 %.2f%%", changePercent, yoyThreshold);
        } else if ("falling".equals(direction)) {
            isAlert = changePercent.compareTo(yoyThreshold.negate()) < 0;
            reason = String.format("同比下降 %.2f%%，超过阈值 %.2f%%", changePercent.abs(), yoyThreshold);
        } else {
            boolean useAbs = Boolean.TRUE.equals(rule.getUseAbsoluteValue());
            BigDecimal compareValue = useAbs ? changePercent.abs() : changePercent;
            BigDecimal threshold = useAbs ? yoyThreshold : yoyThreshold.abs();
            isAlert = compareValue.compareTo(threshold) > 0;
            reason = String.format("同比变化 %.2f%%，超过阈值 %.2f%%", changePercent, yoyThreshold);
        }

        handleAlertCheck(rule, sensorData, isAlert, reason, yoyValue);
    }

    private void checkRateOfChangeRule(AlertRule rule, SensorDataDTO sensorData,
                                        BigDecimal previousValue, LocalDateTime previousTime) {
        if (previousValue == null || previousTime == null) {
            return;
        }

        BigDecimal value = sensorData.getValue();
        BigDecimal rateThreshold = rule.getRateOfChangeThreshold() != null
                ? rule.getRateOfChangeThreshold()
                : new BigDecimal("10");
        String unit = rule.getRateOfChangeUnit() != null ? rule.getRateOfChangeUnit() : "per_second";

        BigDecimal change = value.subtract(previousValue);
        long timeDiffSeconds = java.time.Duration.between(previousTime, LocalDateTime.now()).getSeconds();

        if (timeDiffSeconds <= 0) {
            return;
        }

        BigDecimal ratePerSecond = change.divide(BigDecimal.valueOf(timeDiffSeconds), 6, RoundingMode.HALF_UP);

        BigDecimal rateToCompare;
        switch (unit) {
            case "per_minute" -> rateToCompare = ratePerSecond.multiply(BigDecimal.valueOf(60));
            case "per_hour" -> rateToCompare = ratePerSecond.multiply(BigDecimal.valueOf(3600));
            default -> rateToCompare = ratePerSecond;
        }

        boolean isAlert;
        String reason;
        String direction = rule.getTrendDirection();

        if ("rising".equals(direction)) {
            isAlert = rateToCompare.compareTo(rateThreshold) > 0;
            reason = String.format("变化率上升 %.4f/%s，超过阈值 %.4f", rateToCompare, unit, rateThreshold);
        } else if ("falling".equals(direction)) {
            isAlert = rateToCompare.compareTo(rateThreshold.negate()) < 0;
            reason = String.format("变化率下降 %.4f/%s，超过阈值 %.4f", rateToCompare.abs(), unit, rateThreshold);
        } else {
            boolean useAbs = Boolean.TRUE.equals(rule.getUseAbsoluteValue());
            BigDecimal compareValue = useAbs ? rateToCompare.abs() : rateToCompare;
            BigDecimal threshold = useAbs ? rateThreshold : rateThreshold.abs();
            isAlert = compareValue.compareTo(threshold) > 0;
            reason = String.format("变化率 %.4f/%s，超过阈值 %.4f", rateToCompare, unit, rateThreshold);
        }

        handleAlertCheck(rule, sensorData, isAlert, reason, rateThreshold);
    }

    private void checkTrendAnomalyRule(AlertRule rule, SensorDataDTO sensorData) {
        int windowSize = rule.getTrendWindowSize() != null ? rule.getTrendWindowSize() : 20;
        List<BigDecimal> trendValues = dataAggregationService.getTrendValues(
                sensorData.getDeviceCode(), sensorData.getMetric(), windowSize);

        if (trendValues.size() < windowSize / 2) {
            return;
        }

        BigDecimal currentValue = sensorData.getValue();

        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : trendValues) {
            sum = sum.add(v);
        }
        BigDecimal avg = sum.divide(BigDecimal.valueOf(trendValues.size()), 6, RoundingMode.HALF_UP);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (BigDecimal v : trendValues) {
            BigDecimal diff = v.subtract(avg);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(trendValues.size()), 6, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        double multiplier = rule.getStdDevMultiplier() != null ? rule.getStdDevMultiplier().doubleValue() : 2.0;

        BigDecimal upperBound = avg.add(stdDev.multiply(BigDecimal.valueOf(multiplier)));
        BigDecimal lowerBound = avg.subtract(stdDev.multiply(BigDecimal.valueOf(multiplier)));

        boolean isAlert = currentValue.compareTo(upperBound) > 0
                || currentValue.compareTo(lowerBound) < 0;

        String reason = String.format("趋势异常偏离(%.1fσ)，均值: %.4f, 标准差: %.4f",
                multiplier, avg, stdDev);

        handleAlertCheck(rule, sensorData, isAlert, reason, upperBound);
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
        BigDecimal avg = sum.divide(BigDecimal.valueOf(recentData.size()), 6, RoundingMode.HALF_UP);

        BigDecimal varianceSum = BigDecimal.ZERO;
        for (SensorData data : recentData) {
            BigDecimal diff = data.getValue().subtract(avg);
            varianceSum = varianceSum.add(diff.multiply(diff));
        }
        BigDecimal variance = varianceSum.divide(BigDecimal.valueOf(recentData.size()), 6, RoundingMode.HALF_UP);
        BigDecimal stdDev = BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));

        BigDecimal deviation = sensorData.getValue().subtract(avg).abs();

        if (stdDev.compareTo(BigDecimal.ZERO) > 0
                && deviation.divide(stdDev, 2, RoundingMode.HALF_UP).compareTo(BigDecimal.valueOf(3)) > 0) {
            triggerAlert(rule, sensorData, "数据异常波动(3σ原则)", avg);
        }
    }

    private void handleAlertCheck(AlertRule rule, SensorDataDTO sensorData,
                                   boolean isAlert, String reason, BigDecimal thresholdValue) {
        String countKey = rule.getId() + ":" + sensorData.getDeviceCode() + ":" + sensorData.getMetric();

        if (isAlert) {
            int count = consecutiveCountMap.getOrDefault(countKey, 0) + 1;
            consecutiveCountMap.put(countKey, count);

            int requiredCount = rule.getConsecutiveTimes() != null ? rule.getConsecutiveTimes() : 1;

            if (count >= requiredCount) {
                triggerAlert(rule, sensorData, reason, thresholdValue);
            }
        } else {
            consecutiveCountMap.remove(countKey);
            checkRecovery(rule, sensorData);
        }
    }

    private void triggerAlert(AlertRule rule, SensorDataDTO sensorData, String reason, BigDecimal thresholdValue) {
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
        alertRecord.setThresholdValue(thresholdValue);
        alertRecord.setAlertTime(LocalDateTime.now());
        alertRecord.setNotificationCount(0);

        alertRecord = alertRecordRepository.save(alertRecord);

        alertNotificationService.sendNotifications(alertRecord, rule);

        eventPublisher.publishEvent(new AlertTriggeredEvent(this, alertRecord));

        log.warn("Alert triggered: {} - {} - {} - {}", alertRecord.getAlertCode(),
                rule.getAlertType(), sensorData.getDeviceCode(), reason);
    }

    private String buildAlertContent(AlertRule rule, SensorDataDTO sensorData, String reason) {
        String template = rule.getMessageTemplate();
        if (template == null || template.isEmpty()) {
            return String.format("设备[%s]的指标[%s]发生告警：%s。当前值：%s",
                    sensorData.getDeviceCode(), sensorData.getMetric(), reason,
                    sensorData.getValue());
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
