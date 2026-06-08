package com.iot.monitor.aggregation.model;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class SlidingWindowStats {

    private final int windowSizeSeconds;
    private final Deque<DataPoint> dataPoints;
    private BigDecimal sum = BigDecimal.ZERO;
    private BigDecimal min = null;
    private BigDecimal max = null;
    private BigDecimal sumOfSquares = BigDecimal.ZERO;
    private AtomicLong count = new AtomicLong(0);
    private BigDecimal firstValue = null;
    private BigDecimal lastValue = null;
    private LocalDateTime firstTime = null;
    private LocalDateTime lastTime = null;

    public SlidingWindowStats(int windowSizeSeconds) {
        this.windowSizeSeconds = windowSizeSeconds;
        this.dataPoints = new ArrayDeque<>();
    }

    public synchronized void addDataPoint(BigDecimal value, LocalDateTime timestamp) {
        DataPoint point = new DataPoint(value, timestamp);
        dataPoints.addLast(point);

        sum = sum.add(value);
        sumOfSquares = sumOfSquares.add(value.multiply(value));
        count.incrementAndGet();

        if (min == null || value.compareTo(min) < 0) {
            min = value;
        }
        if (max == null || value.compareTo(max) > 0) {
            max = value;
        }
        if (firstValue == null) {
            firstValue = value;
            firstTime = timestamp;
        }
        lastValue = value;
        lastTime = timestamp;

        evictExpired(LocalDateTime.now());
    }

    public synchronized void evictExpired(LocalDateTime now) {
        LocalDateTime cutoff = now.minusSeconds(windowSizeSeconds);

        while (!dataPoints.isEmpty() && dataPoints.peekFirst().getTimestamp().isBefore(cutoff)) {
            DataPoint expired = dataPoints.pollFirst();
            BigDecimal value = expired.getValue();

            sum = sum.subtract(value);
            sumOfSquares = sumOfSquares.subtract(value.multiply(value));
            count.decrementAndGet();

            if (value.compareTo(min) == 0 || value.compareTo(max) == 0) {
                recalculateMinMax();
            }
            if (value.compareTo(firstValue) == 0) {
                firstValue = dataPoints.peekFirst() != null ? dataPoints.peekFirst().getValue() : null;
                firstTime = dataPoints.peekFirst() != null ? dataPoints.peekFirst().getTimestamp() : null;
            }
        }
    }

    private void recalculateMinMax() {
        min = null;
        max = null;
        for (DataPoint point : dataPoints) {
            BigDecimal v = point.getValue();
            if (min == null || v.compareTo(min) < 0) min = v;
            if (max == null || v.compareTo(max) > 0) max = v;
        }
    }

    public synchronized BigDecimal getAvg() {
        long c = count.get();
        if (c == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(c), 6, RoundingMode.HALF_UP);
    }

    public synchronized BigDecimal getStdDev() {
        long c = count.get();
        if (c < 2) return BigDecimal.ZERO;
        BigDecimal avg = getAvg();
        BigDecimal variance = sumOfSquares.divide(BigDecimal.valueOf(c), 10, RoundingMode.HALF_UP)
                .subtract(avg.multiply(avg));
        if (variance.compareTo(BigDecimal.ZERO) < 0) {
            variance = BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue()));
    }

    public synchronized boolean isEmpty() {
        return count.get() == 0;
    }

    public synchronized long getCount() {
        return count.get();
    }

    @Data
    public static class DataPoint {
        private final BigDecimal value;
        private final LocalDateTime timestamp;
    }
}
