package com.iot.monitor.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SensorDataDTO implements Serializable {

    private String deviceCode;
    private String metric;
    private BigDecimal value;
    private String unit;
    private LocalDateTime dataTime;
    private Map<String, Object> extra;
}
