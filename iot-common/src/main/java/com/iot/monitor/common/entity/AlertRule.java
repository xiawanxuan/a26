package com.iot.monitor.common.entity;

import com.iot.monitor.common.enums.AlertLevel;
import com.iot.monitor.common.enums.AlertType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_alert_rule", indexes = {
        @Index(name = "idx_alert_device_code", columnList = "deviceCode"),
        @Index(name = "idx_alert_metric", columnList = "metric")
})
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String ruleName;

    @Column(length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertLevel alertLevel;

    @Column(length = 64)
    private String deviceCode;

    @Column(length = 64)
    private String deviceType;

    @Column(length = 64)
    private String metric;

    @Column(length = 16)
    private String comparisonOperator;

    @Column(precision = 18, scale = 6)
    private BigDecimal thresholdValue;

    @Column(precision = 18, scale = 6)
    private BigDecimal minThreshold;

    @Column(precision = 18, scale = 6)
    private BigDecimal maxThreshold;

    @Column
    private Integer consecutiveTimes;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(length = 256)
    private String notificationChannels;

    @Column(length = 512)
    private String notificationTargets;

    @Column(length = 512)
    private String messageTemplate;

    @Column(nullable = false)
    private Boolean recoverable;

    @Column
    private Integer suppressDuration;

    @Column(length = 32)
    private String baselinePeriod;

    @Column
    private Integer baselineWindowSize;

    @Column(precision = 10, scale = 4)
    private BigDecimal stdDevMultiplier;

    @Column(precision = 10, scale = 4)
    private BigDecimal percentileValue;

    @Column(length = 32)
    private String comparisonMode;

    @Column
    private Integer comparisonWindowSize;

    @Column(precision = 10, scale = 4)
    private BigDecimal momThreshold;

    @Column(precision = 10, scale = 4)
    private BigDecimal yoyThreshold;

    @Column(precision = 10, scale = 4)
    private BigDecimal rateOfChangeThreshold;

    @Column(length = 16)
    private String rateOfChangeUnit;

    @Column
    private Integer trendWindowSize;

    @Column(length = 16)
    private String trendDirection;

    @Column
    private Boolean useAbsoluteValue;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
