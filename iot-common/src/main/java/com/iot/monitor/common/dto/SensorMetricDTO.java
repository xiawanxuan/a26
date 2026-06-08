package com.iot.monitor.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class SensorMetricDTO implements Serializable {

    private String metric;
    private BigDecimal value;
    private String unit;
    private String dataTime;
}
