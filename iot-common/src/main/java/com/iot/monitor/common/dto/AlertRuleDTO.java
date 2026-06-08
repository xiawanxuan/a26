package com.iot.monitor.common.dto;

import com.iot.monitor.common.enums.AlertLevel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlertRuleDTO implements Serializable {

    private Long id;
    private String ruleName;
    private String description;
    private String alertType;
    private AlertLevel alertLevel;
    private String deviceCode;
    private String deviceType;
    private String metric;
    private String comparisonOperator;
    private BigDecimal thresholdValue;
    private BigDecimal minThreshold;
    private BigDecimal maxThreshold;
    private Integer consecutiveTimes;
    private Boolean enabled;
    private List<String> notificationChannels;
    private List<String> notificationTargets;
    private String messageTemplate;
    private Boolean recoverable;
    private Integer suppressDuration;
    private String baselinePeriod;
    private Integer baselineWindowSize;
    private BigDecimal stdDevMultiplier;
    private BigDecimal percentileValue;
    private String comparisonMode;
    private Integer comparisonWindowSize;
    private BigDecimal momThreshold;
    private BigDecimal yoyThreshold;
    private BigDecimal rateOfChangeThreshold;
    private String rateOfChangeUnit;
    private Integer trendWindowSize;
    private String trendDirection;
    private Boolean useAbsoluteValue;
}
