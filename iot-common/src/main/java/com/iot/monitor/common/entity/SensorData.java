package com.iot.monitor.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_sensor_data", indexes = {
        @Index(name = "idx_device_code", columnList = "deviceCode"),
        @Index(name = "idx_metric", columnList = "metric"),
        @Index(name = "idx_create_time", columnList = "createTime")
})
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String deviceCode;

    @Column(nullable = false, length = 64)
    private String metric;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal value;

    @Column(length = 32)
    private String unit;

    @Column
    private LocalDateTime dataTime;

    @Column(length = 256)
    private String extraInfo;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createTime;
}
