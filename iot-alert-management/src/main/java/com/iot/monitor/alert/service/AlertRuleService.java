package com.iot.monitor.alert.service;

import com.iot.monitor.common.dto.AlertRuleDTO;
import com.iot.monitor.common.dto.PageResult;
import com.iot.monitor.common.entity.AlertRule;
import com.iot.monitor.common.entity.Device;
import com.iot.monitor.common.enums.AlertLevel;
import com.iot.monitor.common.enums.AlertType;
import com.iot.monitor.common.exception.BusinessException;
import com.iot.monitor.common.repository.AlertRuleRepository;
import com.iot.monitor.device.service.DeviceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final DeviceService deviceService;

    @Transactional
    public AlertRuleDTO createRule(AlertRuleDTO dto) {
        AlertRule rule = new AlertRule();
        convertToEntity(dto, rule);
        rule = alertRuleRepository.save(rule);
        log.info("Alert rule created: {}", rule.getRuleName());
        return convertToDTO(rule);
    }

    @Transactional
    public AlertRuleDTO updateRule(Long id, AlertRuleDTO dto) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警规则不存在"));
        convertToEntity(dto, rule);
        rule = alertRuleRepository.save(rule);
        log.info("Alert rule updated: {}", rule.getId());
        return convertToDTO(rule);
    }

    @Transactional
    public void deleteRule(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警规则不存在"));
        alertRuleRepository.delete(rule);
        log.info("Alert rule deleted: {}", id);
    }

    public AlertRuleDTO getRuleById(Long id) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警规则不存在"));
        return convertToDTO(rule);
    }

    public PageResult<AlertRuleDTO> listRules(int pageNum, int pageSize, String ruleName,
                                               String alertType, String alertLevel, Boolean enabled) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);

        Specification<AlertRule> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ruleName != null && !ruleName.isEmpty()) {
                predicates.add(cb.like(root.get("ruleName"), "%" + ruleName + "%"));
            }
            if (alertType != null && !alertType.isEmpty()) {
                predicates.add(cb.equal(root.get("alertType"), AlertType.valueOf(alertType)));
            }
            if (alertLevel != null && !alertLevel.isEmpty()) {
                predicates.add(cb.equal(root.get("alertLevel"), AlertLevel.valueOf(alertLevel)));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AlertRule> page = alertRuleRepository.findAll(spec, pageable);
        List<AlertRuleDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        return PageResult.of(dtoList, page.getTotalElements(), pageNum, pageSize);
    }

    @Transactional
    public void toggleRule(Long id, boolean enabled) {
        AlertRule rule = alertRuleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("告警规则不存在"));
        rule.setEnabled(enabled);
        alertRuleRepository.save(rule);
        log.info("Alert rule {} toggled to {}", id, enabled);
    }

    public List<AlertRule> getApplicableRules(String deviceCode, String metric) {
        List<AlertRule> rules = new ArrayList<>();

        Device device = deviceService.getDeviceEntityByCode(deviceCode);
        if (device == null) {
            return rules;
        }

        rules.addAll(alertRuleRepository.findByDeviceCodeAndEnabledTrue(deviceCode));

        if (device.getDeviceType() != null) {
            rules.addAll(alertRuleRepository.findByDeviceTypeAndEnabledTrue(device.getDeviceType()));
        }

        if (metric != null) {
            rules.addAll(alertRuleRepository.findByMetricAndEnabledTrue(metric));
        }

        return rules.stream().distinct().toList();
    }

    private void convertToEntity(AlertRuleDTO dto, AlertRule entity) {
        entity.setRuleName(dto.getRuleName());
        entity.setDescription(dto.getDescription());
        entity.setAlertType(dto.getAlertType() != null ? AlertType.valueOf(dto.getAlertType()) : AlertType.THRESHOLD_EXCEEDED);
        entity.setAlertLevel(dto.getAlertLevel() != null ? dto.getAlertLevel() : AlertLevel.WARNING);
        entity.setDeviceCode(dto.getDeviceCode());
        entity.setDeviceType(dto.getDeviceType());
        entity.setMetric(dto.getMetric());
        entity.setComparisonOperator(dto.getComparisonOperator());
        entity.setThresholdValue(dto.getThresholdValue());
        entity.setMinThreshold(dto.getMinThreshold());
        entity.setMaxThreshold(dto.getMaxThreshold());
        entity.setConsecutiveTimes(dto.getConsecutiveTimes() != null ? dto.getConsecutiveTimes() : 1);
        entity.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);

        if (dto.getNotificationChannels() != null) {
            entity.setNotificationChannels(String.join(",", dto.getNotificationChannels()));
        }
        if (dto.getNotificationTargets() != null) {
            entity.setNotificationTargets(String.join(",", dto.getNotificationTargets()));
        }

        entity.setMessageTemplate(dto.getMessageTemplate());
        entity.setRecoverable(dto.getRecoverable() != null ? dto.getRecoverable() : true);
        entity.setSuppressDuration(dto.getSuppressDuration());
    }

    private AlertRuleDTO convertToDTO(AlertRule entity) {
        AlertRuleDTO dto = new AlertRuleDTO();
        dto.setId(entity.getId());
        dto.setRuleName(entity.getRuleName());
        dto.setDescription(entity.getDescription());
        dto.setAlertType(entity.getAlertType().name());
        dto.setAlertLevel(entity.getAlertLevel());
        dto.setDeviceCode(entity.getDeviceCode());
        dto.setDeviceType(entity.getDeviceType());
        dto.setMetric(entity.getMetric());
        dto.setComparisonOperator(entity.getComparisonOperator());
        dto.setThresholdValue(entity.getThresholdValue());
        dto.setMinThreshold(entity.getMinThreshold());
        dto.setMaxThreshold(entity.getMaxThreshold());
        dto.setConsecutiveTimes(entity.getConsecutiveTimes());
        dto.setEnabled(entity.getEnabled());

        if (entity.getNotificationChannels() != null) {
            dto.setNotificationChannels(Arrays.asList(entity.getNotificationChannels().split(",")));
        }
        if (entity.getNotificationTargets() != null) {
            dto.setNotificationTargets(Arrays.asList(entity.getNotificationTargets().split(",")));
        }

        dto.setMessageTemplate(entity.getMessageTemplate());
        dto.setRecoverable(entity.getRecoverable());
        dto.setSuppressDuration(entity.getSuppressDuration());
        return dto;
    }
}
