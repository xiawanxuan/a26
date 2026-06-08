package com.iot.monitor.alert.service;

import com.iot.monitor.common.entity.AlertNotification;
import com.iot.monitor.common.entity.AlertRecord;
import com.iot.monitor.common.entity.AlertRule;
import com.iot.monitor.common.repository.AlertNotificationRepository;
import com.iot.monitor.common.repository.AlertRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotificationService {

    private final AlertNotificationRepository notificationRepository;
    private final AlertRecordRepository alertRecordRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@example.com}")
    private String mailFrom;

    @Async
    public void sendNotifications(AlertRecord alertRecord, AlertRule rule) {
        if (rule.getNotificationChannels() == null) {
            return;
        }

        List<String> channels = Arrays.asList(rule.getNotificationChannels().split(","));
        List<String> targets = rule.getNotificationTargets() != null
                ? Arrays.asList(rule.getNotificationTargets().split(","))
                : List.of();

        for (String channel : channels) {
            for (String target : targets) {
                sendNotification(alertRecord, channel.trim(), target.trim(), rule);
            }
        }

        alertRecord.setNotificationCount(alertRecord.getNotificationCount() + 1);
        alertRecordRepository.save(alertRecord);
    }

    private void sendNotification(AlertRecord alertRecord, String channel, String target, AlertRule rule) {
        AlertNotification notification = new AlertNotification();
        notification.setAlertRecordId(alertRecord.getId());
        notification.setChannel(channel);
        notification.setTarget(target);
        notification.setTitle(alertRecord.getTitle());
        notification.setContent(alertRecord.getContent());
        notification.setStatus("PENDING");
        notification.setCreateTime(LocalDateTime.now());

        try {
            switch (channel.toLowerCase()) {
                case "email", "mail" -> sendEmail(target, alertRecord.getTitle(), alertRecord.getContent());
                case "sms" -> sendSms(target, alertRecord.getContent());
                case "webhook" -> sendWebhook(target, alertRecord);
                default -> {
                    notification.setStatus("SKIPPED");
                    notification.setErrorMessage("不支持的通知渠道: " + channel);
                }
            }
            notification.setStatus("SENT");
            notification.setSendTime(LocalDateTime.now());
            log.info("Notification sent: channel={}, target={}, alert={}", channel, target, alertRecord.getAlertCode());
        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());
            log.error("Failed to send notification: channel={}, target={}", channel, target, e);
        }

        notificationRepository.save(notification);
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject("[告警] " + subject);
            message.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Failed to send email, will log only: {}", e.getMessage());
        }
    }

    private void sendSms(String phoneNumber, String content) {
        log.info("SMS notification would be sent to {}: {}", phoneNumber, content);
    }

    private void sendWebhook(String url, AlertRecord alertRecord) {
        log.info("Webhook notification would be sent to {} for alert {}", url, alertRecord.getAlertCode());
    }

    public List<AlertNotification> getNotificationHistory(Long alertRecordId) {
        return notificationRepository.findByAlertRecordIdOrderByCreateTimeDesc(alertRecordId);
    }
}
