package com.iot.monitor.aggregation.service;

import com.iot.monitor.aggregation.model.SlidingWindowStats;
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

    private final Map<String, SlidingWindowStats> window1Min = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindowStats> window5Min = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindowStats> window15Min = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindowStats> window1Hour = new ConcurrentHashMap<>();
    private final Map<String, SlidingWindowStats> window1Day = new ConcurrentHashMap<>();

    private final Map<String, BigDecimal> previousMinuteValue = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> previousHourValue = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> sameHourYesterdayValue = new ConcurrentHashMap<>();

    private final Map<String, List<BigDecimal>> minuteWindowValues = new ConcurrentHashMap<>();
    private final Map<String, List<BigDecimal>> hourWindowValues = new ConcurrentHashMap<>();
    private final Map<String, Deque<BigDecimal>> trendWindowValues = new ConcurrentHashMap<>();

    @EventListener
    public void onSensorDataReceived(SensorDataReceivedEvent event) {
        String deviceCode = event.getSensorData().getDeviceCode();
        String metric = event.getSensorData().getMetric();
        BigDecimal value = event.getSensorData().getValue();
        LocalDateTime dataTime = event.getSensorData().getDataTime() != null
                ? event.getSensorData().getDataTime()
                : LocalDateTime.now();
        String key = deviceCode + ":" + metric;

        getOrCreateWindow(window1Min, key, 60).addDataPoint(value, dataTime);
        getOrCreateWindow(window5Min, key, 300).addDataPoint(value, dataTime);
        getOrCreateWindow(window15Min, key, 900).addDataPoint(value, dataTime);
        getOrCreateWindow(window1Hour, key, 3600).addDataPoint(value, dataTime);
        getOrCreateWindow(window1Day, key, 86400).addDataPoint(value, dataTime);

        updateTrendWindow(key, value);
    }

    private SlidingWindowStats getOrCreateWindow(Map<String, SlidingWindowStats> windowMap,
                                                  String key, int windowSizeSeconds) {
        return windowMap.computeIfAbsent(key, k -> new SlidingWindowStats(windowSizeSeconds));
    }

    private void updateTrendWindow(String key, BigDecimal value) {
        Deque<BigDecimal> deque = trendWindowValues.computeIfAbsent(key, k -> new ArrayDeque<>());
        deque.addLast(value);
        if (deque.size() > 100) {
            deque.pollFirst();
        }
    }

    @Scheduled(fixedRate = 1000)
    public void evictExpiredWindows() {
        LocalDateTime now = LocalDateTime.now();
        for (SlidingWindowStats window : window1Min.values()) {
            window.evictExpired(now);
        }
        for (SlidingWindowStats window : window5Min.values()) {
            window.evictExpired(now);
        }
        for (SlidingWindowStats window : window15Min.values()) {
            window.evictExpired(now);
        }
        for (SlidingWindowStats window : window1Hour.values()) {
            window.evictExpired(now);
        }
        for (SlidingWindowStats window : window1Day.values()) {
            window.evictExpired(now);
        }
    }

    public Map<String, Object> getRealtimeStatistics(String deviceCode, String metric) {
        String key = deviceCode + ":" + metric;
        Map<String, Object> result = new HashMap<>();
        result.put("deviceCode", deviceCode);
        result.put("metric", metric);
        result.put("timestamp", LocalDateTime.now());

        SlidingWindowStats w1min = window1Min.get(key);
        if (w1min != null && !w1min.isEmpty()) {
            result.put("window1min", buildWindowStats(w1min));
        }

        SlidingWindowStats w5min = window5Min.get(key);
        if (w5min != null && !w5min.isEmpty()) {
            result.put("window5min", buildWindowStats(w5min));
        }

        SlidingWindowStats w15min = window15Min.get(key);
        if (w15min != null && !w15min.isEmpty()) {
            result.put("window15min", buildWindowStats(w15min));
        }

        SlidingWindowStats w1hour = window1Hour.get(key);
        if (w1hour != null && !w1hour.isEmpty()) {
            result.put("window1hour", buildWindowStats(w1hour));
        }

        SlidingWindowStats w1day = window1Day.get(key);
        if (w1day != null && !w1day.isEmpty()) {
            result.put("window1day", buildWindowStats(w1day));
        }

        BigDecimal currentValue = w1min != null ? w1min.getLastValue() : null;
        result.put("currentValue", currentValue);

        BigDecimal prevMin = previousMinuteValue.get(key);
        if (prevMin != null && currentValue != null) {
            BigDecimal mom = currentValue.subtract(prevMin);
            BigDecimal momPercent = prevMin.compareTo(BigDecimal.ZERO) != 0
                    ? mom.divide(prevMin, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            result.put("momValue", mom);
            result.put("momPercent", momPercent);
        }

        Deque<BigDecimal> trend = trendWindowValues.get(key);
        if (trend != null && trend.size() >= 2) {
            List<BigDecimal> list = new ArrayList<>(trend);
            result.put("trendDirection", calculateTrendDirection(list));
            result.put("trendStrength", calculateTrendStrength(list));
        }

        return result;
    }

    private Map<String, Object> buildWindowStats(SlidingWindowStats window) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", window.getCount());
        stats.put("avg", window.getAvg());
        stats.put("min", window.getMin());
        stats.put("max", window.getMax());
        stats.put("sum", window.getSum());
        stats.put("stdDev", window.getStdDev());
        stats.put("firstValue", window.getFirstValue());
        stats.put("lastValue", window.getLastValue());
        stats.put("firstTime", window.getFirstTime());
        stats.put("lastTime", window.getLastTime());
        return stats;
    }

    private String calculateTrendDirection(List<BigDecimal> values) {
        if (values.size() < 2) return "stable";

        int increases = 0;
        int decreases = 0;

        for (int i = 1; i < values.size(); i++) {
            int cmp = values.get(i).compareTo(values.get(i - 1));
            if (cmp > 0) increases++;
            else if (cmp < 0) decreases++;
        }

        if (increases > decreases * 1.5) return "rising";
        if (decreases > increases * 1.5) return "falling";
        return "stable";
    }

    private BigDecimal calculateTrendStrength(List<BigDecimal> values) {
        if (values.size() < 2) return BigDecimal.ZERO;
        BigDecimal first = values.get(0);
        BigDecimal last = values.get(values.size() - 1);
        BigDecimal diff = last.subtract(first);
        if (first.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return diff.divide(first, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    public BigDecimal getDynamicThreshold(String deviceCode, String metric, int windowSeconds,
                                           double stdDevMultiplier) {
        String key = deviceCode + ":" + metric;
        SlidingWindowStats window;

        if (windowSeconds <= 60) {
            window = window1Min.get(key);
        } else if (windowSeconds <= 300) {
            window = window5Min.get(key);
        } else if (windowSeconds <= 900) {
            window = window15Min.get(key);
        } else if (windowSeconds <= 3600) {
            window = window1Hour.get(key);
        } else {
            window = window1Day.get(key);
        }

        if (window == null || window.isEmpty()) {
            return null;
        }

        BigDecimal avg = window.getAvg();
        BigDecimal stdDev = window.getStdDev();
        return avg.add(stdDev.multiply(BigDecimal.valueOf(stdDevMultiplier)));
    }

    public BigDecimal getMomValue(String deviceCode, String metric) {
        String key = deviceCode + ":" + metric;
        return previousMinuteValue.get(key);
    }

    public BigDecimal getYoyValue(String deviceCode, String metric) {
        String key = deviceCode + ":" + metric;
        return sameHourYesterdayValue.get(key);
    }

    public List<BigDecimal> getTrendValues(String deviceCode, String metric, int size) {
        String key = deviceCode + ":" + metric;
        Deque<BigDecimal> deque = trendWindowValues.get(key);
        if (deque == null) return Collections.emptyList();
        List<BigDecimal> result = new ArrayList<>(deque);
        if (result.size() > size) {
            result = result.subList(result.size() - size, result.size());
        }
        return result;
    }

    public SlidingWindowStats getWindowStats(String deviceCode, String metric, int windowSeconds) {
        String key = deviceCode + ":" + metric;
        if (windowSeconds <= 60) return window1Min.get(key);
        if (windowSeconds <= 300) return window5Min.get(key);
        if (windowSeconds <= 900) return window15Min.get(key);
        if (windowSeconds <= 3600) return window1Hour.get(key);
        return window1Day.get(key);
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void aggregateMinuteData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.truncatedTo(ChronoUnit.MINUTES).minusMinutes(1);
        LocalDateTime periodEnd = periodStart.plusMinutes(1);

        int count = 0;
        for (Map.Entry<String, SlidingWindowStats> entry : window1Min.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String key = entry.getKey();
                String[] parts = key.split(":", 2);
                if (parts.length == 2) {
                    SlidingWindowStats stats = entry.getValue();
                    previousMinuteValue.put(key, stats.getLastValue());

                    saveAggregated(parts[0], parts[1], AggregationPeriod.MINUTE,
                            periodStart, periodEnd, stats);
                    count++;
                }
            }
        }
        if (count > 0) {
            log.debug("Minute aggregation completed: {} metrics", count);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void aggregateHourData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.truncatedTo(ChronoUnit.HOURS).minusHours(1);
        LocalDateTime periodEnd = periodStart.plusHours(1);

        int count = 0;
        for (Map.Entry<String, SlidingWindowStats> entry : window1Hour.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String key = entry.getKey();
                String[] parts = key.split(":", 2);
                if (parts.length == 2) {
                    SlidingWindowStats stats = entry.getValue();
                    previousHourValue.put(key, stats.getLastValue());

                    saveAggregated(parts[0], parts[1], AggregationPeriod.HOUR,
                            periodStart, periodEnd, stats);
                    count++;
                }
            }
        }
        log.debug("Hour aggregation completed: {} metrics", count);
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void aggregateDayData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.truncatedTo(ChronoUnit.DAYS).minusDays(1);
        LocalDateTime periodEnd = periodStart.plusDays(1);

        int count = 0;
        for (Map.Entry<String, SlidingWindowStats> entry : window1Day.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String key = entry.getKey();
                String[] parts = key.split(":", 2);
                if (parts.length == 2) {
                    SlidingWindowStats stats = entry.getValue();
                    sameHourYesterdayValue.put(key, stats.getLastValue());

                    saveAggregated(parts[0], parts[1], AggregationPeriod.DAY,
                            periodStart, periodEnd, stats);
                    count++;
                }
            }
        }
        log.info("Day aggregation completed: {} metrics", count);
    }

    private void saveAggregated(String deviceCode, String metric, AggregationPeriod period,
                                 LocalDateTime periodStart, LocalDateTime periodEnd,
                                 SlidingWindowStats stats) {
        if (stats.isEmpty()) return;

        AggregatedData existing = aggregatedDataRepository
                .findByDeviceCodeAndMetricAndPeriodAndPeriodStart(deviceCode, metric, period, periodStart)
                .orElse(null);

        AggregatedData data;
        if (existing != null) {
            data = existing;
        } else {
            data = new AggregatedData();
            data.setDeviceCode(deviceCode);
            data.setMetric(metric);
            data.setPeriod(period);
            data.setPeriodStart(periodStart);
        }

        data.setPeriodEnd(periodEnd);
        data.setAvgValue(stats.getAvg());
        data.setMinValue(stats.getMin());
        data.setMaxValue(stats.getMax());
        data.setSumValue(stats.getSum());
        data.setDataCount(stats.getCount());
        data.setFirstValue(stats.getFirstValue());
        data.setLastValue(stats.getLastValue());

        aggregatedDataRepository.save(data);
    }

    public List<AggregatedData> getAggregatedData(String deviceCode, String metric, AggregationPeriod period,
                                                   LocalDateTime startTime, LocalDateTime endTime) {
        return aggregatedDataRepository
                .findByDeviceCodeAndMetricAndPeriodAndPeriodStartBetweenOrderByPeriodStartAsc(
                        deviceCode, metric, period, startTime, endTime);
    }

    public Map<String, Object> getRealtimeStatistics(String deviceCode, String metric, int minutes) {
        return getRealtimeStatistics(deviceCode, metric);
    }
}
