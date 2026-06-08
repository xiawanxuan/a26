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

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
