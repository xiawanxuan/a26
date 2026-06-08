package com.iot.monitor.common.entity;

import com.iot.monitor.common.enums.DeviceStatus;
import com.iot.monitor.common.enums.ProtocolType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String deviceCode;

    @Column(nullable = false, length = 128)
    private String deviceName;

    @Column(length = 256)
    private String description;

    @Column(length = 64)
    private String deviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeviceStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProtocolType protocolType;

    @Column(length = 128)
    private String mqttTopic;

    @Column(length = 64)
    private String location;

    @Column(length = 64)
    private String firmwareVersion;

    @Column
    private LocalDateTime lastOnlineTime;

    @Column
    private LocalDateTime lastDataTime;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime;
}
