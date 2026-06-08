package com.iot.monitor.common.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_alert_notification", indexes = {
        @Index(name = "idx_notify_record_id", columnList = "alertRecordId"),
        @Index(name = "idx_notify_channel", columnList = "channel"),
        @Index(name = "idx_notify_status", columnList = "status")
})
public class AlertNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long alertRecordId;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, length = 256)
    private String target;

    @Column(length = 512)
    private String title;

    @Column(length = 4000)
    private String content;

    @Column(length = 16)
    private String status;

    @Column(length = 1024)
    private String errorMessage;

    @Column
    private LocalDateTime sendTime;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime createTime;
}
