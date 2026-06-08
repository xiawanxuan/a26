package com.iot.monitor.aggregation.service;

import com.iot.monitor.common.entity.AggregatedData;
import com.iot.monitor.common.entity.SensorData;
import com.iot.monitor.common.enums.AggregationPeriod;
import com.iot.monitor.common.event.SensorDataReceivedEvent;
import com.iot.monitor.common.repository.AggregatedDataRepository;
import com.iot.monitor.common.repository.SensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataAggregationService {

    private final AggregatedDataRepository aggregatedDataRepository;
    private final SensorDataRepository sensorDataRepository;

    private final Map<String, List<BigDecimal>> minuteCache = new ConcurrentHashMap<>();

    @EventListener
    public void onSensorDataReceived(SensorDataReceivedEvent event) {
        String key = event.getSensorData().getDeviceCode() + ":" + event.getSensorData().getMetric();
        minuteCache.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                  .add(event.getSensorData().getValue());
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void aggregateMinuteData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).minusMinutes(1);
        LocalDateTime periodEnd = periodStart.plusMinutes(1);

        int aggregatedCount = 0;
        Set<String> processedKeys = new HashSet<>(minuteCache.keySet());

        for (String key : processedKeys) {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                String deviceCode = parts[0];
                String metric = parts[1];

                List<BigDecimal> values = minuteCache.remove(key);
                if (values != null && !values.isEmpty()) {
                    aggregateAndSave(deviceCode, metric, AggregationPeriod.MINUTE,
                            periodStart, periodEnd, values);
                    aggregatedCount++;
                }
            }
        }

        if (aggregatedCount > 0) {
            log.info("Minute aggregation completed: {} metrics aggregated", aggregatedCount);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void aggregateHourData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.truncatedTo(ChronoUnit.HOURS).minusHours(1);
        LocalDateTime periodEnd = periodStart.plusHours(1);

        aggregateFromDb(AggregationPeriod.HOUR, periodStart, periodEnd);
        log.info("Hour aggregation completed");
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void aggregateDayData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.truncatedTo(ChronoUnit.DAYS).minusDays(1);
        LocalDateTime periodEnd = periodStart.plusDays(1);

        aggregateFromDb(AggregationPeriod.DAY, periodStart, periodEnd);
        log.info("Day aggregation completed");
    }

    private void aggregateFromDb(AggregationPeriod period, LocalDateTime periodStart, LocalDateTime periodEnd) {
    }

    private AggregatedData aggregateAndSave(String deviceCode, String metric, AggregationPeriod period,
                                            LocalDateTime periodStart, LocalDateTime periodEnd,
                                            List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }

        BigDecimal min = values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal max = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);

        AggregatedData existing = aggregatedDataRepository
                .findByDeviceCodeAndMetricAndPeriodAndPeriodStart(deviceCode, metric, period, periodStart)
                .orElse(null);

        AggregatedData aggregatedData;
        if (existing != null) {
            aggregatedData = existing;
        } else {
            aggregatedData = new AggregatedData();
            aggregatedData.setDeviceCode(deviceCode);
            aggregatedData.setMetric(metric);
            aggregatedData.setPeriod(period);
            aggregatedData.setPeriodStart(periodStart);
        }

        aggregatedData.setPeriodEnd(periodEnd);
        aggregatedData.setAvgValue(avg);
        aggregatedData.setMinValue(min);
        aggregatedData.setMaxValue(max);
        aggregatedData.setSumValue(sum);
        aggregatedData.setDataCount((long) values.size());
        aggregatedData.setFirstValue(values.get(0));
        aggregatedData.setLastValue(values.get(values.size() - 1));

        return aggregatedDataRepository.save(aggregatedData);
    }

    public List<AggregatedData> getAggregatedData(String deviceCode, String metric, AggregationPeriod period,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        return aggregatedDataRepository
                .findByDeviceCodeAndMetricAndPeriodAndPeriodStartBetweenOrderByPeriodStartAsc(
                        deviceCode, metric, period, startTime, endTime);
    }

    public Map<String, Object> getRealtimeStatistics(String deviceCode, String metric, int minutes) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(minutes);
        List<SensorData> dataList = sensorDataRepository
                .findByDeviceCodeAndMetricAndDataTimeBetweenOrderByDataTimeAsc(
                        deviceCode, metric, startTime, LocalDateTime.now());

        Map<String, Object> result = new HashMap<>();
        result.put("deviceCode", deviceCode);
        result.put("metric", metric);
        result.put("dataCount", dataList.size());
        result.put("timeRangeMinutes", minutes);

        if (!dataList.isEmpty()) {
            BigDecimal min = BigDecimal.valueOf(Double.MAX_VALUE);
            BigDecimal max = BigDecimal.valueOf(Double.MIN_VALUE);
            BigDecimal sum = BigDecimal.ZERO;

            for (SensorData data : dataList) {
                BigDecimal value = data.getValue();
                if (value.compareTo(min) < 0) min = value;
                if (value.compareTo(max) > 0) max = value;
                sum = sum.add(value);
            }

            BigDecimal avg = sum.divide(BigDecimal.valueOf(dataList.size()), 6, RoundingMode.HALF_UP);

            result.put("minValue", min);
            result.put("maxValue", max);
            result.put("avgValue", avg);
            result.put("currentValue", dataList.get(dataList.size() - 1).getValue());
            result.put("firstValue", dataList.get(0).getValue());
            result.put("lastUpdateTime", dataList.get(dataList.size() - 1).getDataTime());
        }

        return result;
    }
}
