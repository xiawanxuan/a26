package com.iot.monitor.common.entity;

import com.iot.monitor.common.enums.AggregationPeriod;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_aggregated_data", indexes = {
        @Index(name = "idx_agg_device_code", columnList = "deviceCode"),
        @Index(name = "idx_agg_metric", columnList = "metric"),
        @Index(name = "idx_agg_period_start", columnList = "period,periodStart"),
        @Index(name = "idx_agg_combo", columnList = "deviceCode,metric,period,periodStart")
})
public class AggregatedData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String deviceCode;

    @Column(nullable = false, length = 64)
    private String metric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AggregationPeriod period;

    @Column(nullable = false)
    private LocalDateTime periodStart;

    @Column(nullable = false)
    private LocalDateTime periodEnd;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal avgValue;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal minValue;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal maxValue;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal sumValue;

    @Column(nullable = false)
    private Long dataCount;

    @Column(precision = 18, scale = 6)
    private BigDecimal firstValue;

    @Column(precision = 18, scale = 6)
    private BigDecimal lastValue;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createTime;
}
