package com.iot.monitor.common.entity;

import com.iot.monitor.common.enums.AlertLevel;
import com.iot.monitor.common.enums.AlertStatus;
import com.iot.monitor.common.enums.AlertType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_alert_record", indexes = {
        @Index(name = "idx_record_device_code", columnList = "deviceCode"),
        @Index(name = "idx_record_status", columnList = "status"),
        @Index(name = "idx_record_level", columnList = "alertLevel"),
        @Index(name = "idx_record_create_time", columnList = "createTime")
})
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String alertCode;

    @Column(length = 64)
    private String deviceCode;

    @Column(length = 64)
    private String metric;

    @Column
    private Long ruleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertLevel alertLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status;

    @Column(length = 512)
    private String title;

    @Column(length = 2048)
    private String content;

    @Column(precision = 18, scale = 6)
    private BigDecimal currentValue;

    @Column(precision = 18, scale = 6)
    private BigDecimal thresholdValue;

    @Column
    private LocalDateTime alertTime;

    @Column
    private LocalDateTime recoverTime;

    @Column
    private LocalDateTime acknowledgeTime;

    @Column(length = 128)
    private String acknowledgedBy;

    @Column(length = 2048)
    private String resolveNote;

    @Column
    private Integer notificationCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
