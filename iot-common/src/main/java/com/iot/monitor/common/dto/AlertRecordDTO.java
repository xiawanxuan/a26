package com.iot.monitor.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AlertRecordDTO implements Serializable {

    private Long id;
    private String alertCode;
    private String deviceCode;
    private String metric;
    private Long ruleId;
    private String alertType;
    private String alertLevel;
    private String status;
    private String title;
    private String content;
    private BigDecimal currentValue;
    private BigDecimal thresholdValue;
    private LocalDateTime alertTime;
    private LocalDateTime recoverTime;
}
