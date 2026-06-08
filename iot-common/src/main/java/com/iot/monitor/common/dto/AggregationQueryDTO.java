package com.iot.monitor.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AggregationQueryDTO implements Serializable {

    private String deviceCode;
    private String metric;
    private String period;
    private String startTime;
    private String endTime;
}
