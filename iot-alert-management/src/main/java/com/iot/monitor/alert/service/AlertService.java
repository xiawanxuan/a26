package com.iot.monitor.alert.service;

import com.iot.monitor.common.dto.AlertRecordDTO;
import com.iot.monitor.common.dto.PageResult;
import com.iot.monitor.common.entity.AlertRecord;
import com.iot.monitor.common.enums.AlertLevel;
import com.iot.monitor.common.enums.AlertStatus;
import com.iot.monitor.common.exception.BusinessException;
import com.iot.monitor.common.repository.AlertRecordRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRecordRepository alertRecordRepository;

    public AlertRecordDTO getAlertById(Long id) {
        AlertRecord record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警记录不存在"));
        return convertToDTO(record);
    }

    public AlertRecordDTO getAlertByCode(String alertCode) {
        AlertRecord record = alertRecordRepository.findByAlertCode(alertCode)
                .orElseThrow(() -> new BusinessException("告警记录不存在"));
        return convertToDTO(record);
    }

    public PageResult<AlertRecordDTO> listAlerts(int pageNum, int pageSize, String deviceCode,
                                                  String status, String alertLevel, String alertType,
                                                  LocalDateTime startTime, LocalDateTime endTime) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);

        Specification<AlertRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (deviceCode != null && !deviceCode.isEmpty()) {
                predicates.add(cb.equal(root.get("deviceCode"), deviceCode));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), AlertStatus.valueOf(status)));
            }
            if (alertLevel != null && !alertLevel.isEmpty()) {
                predicates.add(cb.equal(root.get("alertLevel"), AlertLevel.valueOf(alertLevel)));
            }
            if (alertType != null && !alertType.isEmpty()) {
                predicates.add(cb.equal(root.get("alertType"), alertType));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("alertTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("alertTime"), endTime));
            }
            query.orderBy(cb.desc(root.get("alertTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AlertRecord> page = alertRecordRepository.findAll(spec, pageable);
        List<AlertRecordDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        return PageResult.of(dtoList, page.getTotalElements(), pageNum, pageSize);
    }

    @Transactional
    public AlertRecordDTO acknowledgeAlert(Long id, String operator) {
        AlertRecord record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警记录不存在"));

        if (record.getStatus() == AlertStatus.ACTIVE || record.getStatus() == AlertStatus.PENDING) {
            record.setStatus(AlertStatus.ACKNOWLEDGED);
            record.setAcknowledgedBy(operator);
            record.setAcknowledgeTime(LocalDateTime.now());
            record = alertRecordRepository.save(record);
            log.info("Alert {} acknowledged by {}", record.getAlertCode(), operator);
        }

        return convertToDTO(record);
    }

    @Transactional
    public AlertRecordDTO resolveAlert(Long id, String resolveNote, String operator) {
        AlertRecord record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警记录不存在"));

        record.setStatus(AlertStatus.RESOLVED);
        record.setRecoverTime(LocalDateTime.now());
        record.setResolveNote(resolveNote);
        record.setAcknowledgedBy(operator);
        record = alertRecordRepository.save(record);
        log.info("Alert {} resolved by {}", record.getAlertCode(), operator);

        return convertToDTO(record);
    }

    @Transactional
    public AlertRecordDTO closeAlert(Long id, String closeNote, String operator) {
        AlertRecord record = alertRecordRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警记录不存在"));

        record.setStatus(AlertStatus.CLOSED);
        record.setResolveNote(closeNote);
        record = alertRecordRepository.save(record);
        log.info("Alert {} closed by {}", record.getAlertCode(), operator);

        return convertToDTO(record);
    }

    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCount", alertRecordRepository.countByStatus(AlertStatus.ACTIVE));
        stats.put("pendingCount", alertRecordRepository.countByStatus(AlertStatus.PENDING));
        stats.put("acknowledgedCount", alertRecordRepository.countByStatus(AlertStatus.ACKNOWLEDGED));
        stats.put("resolvedCount", alertRecordRepository.countByStatus(AlertStatus.RESOLVED));

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        stats.put("todayCriticalCount", alertRecordRepository.countByLevelAndTimeAfter(AlertLevel.CRITICAL, todayStart));
        stats.put("todayErrorCount", alertRecordRepository.countByLevelAndTimeAfter(AlertLevel.ERROR, todayStart));
        stats.put("todayWarningCount", alertRecordRepository.countByLevelAndTimeAfter(AlertLevel.WARNING, todayStart));

        return stats;
    }

    private AlertRecordDTO convertToDTO(AlertRecord entity) {
        AlertRecordDTO dto = new AlertRecordDTO();
        dto.setId(entity.getId());
        dto.setAlertCode(entity.getAlertCode());
        dto.setDeviceCode(entity.getDeviceCode());
        dto.setMetric(entity.getMetric());
        dto.setRuleId(entity.getRuleId());
        dto.setAlertType(entity.getAlertType().name());
        dto.setAlertLevel(entity.getAlertLevel().name());
        dto.setStatus(entity.getStatus().name());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setCurrentValue(entity.getCurrentValue());
        dto.setThresholdValue(entity.getThresholdValue());
        dto.setAlertTime(entity.getAlertTime());
        dto.setRecoverTime(entity.getRecoverTime());
        return dto;
    }
}
