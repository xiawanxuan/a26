package com.iot.monitor.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchSensorDataDTO implements Serializable {

    private String deviceCode;
    private List<SensorMetricDTO> metrics;
}
