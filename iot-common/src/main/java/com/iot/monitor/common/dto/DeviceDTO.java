package com.iot.monitor.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeviceDTO implements Serializable {

    private Long id;
    private String deviceCode;
    private String deviceName;
    private String description;
    private String deviceType;
    private String status;
    private String protocolType;
    private String mqttTopic;
    private String location;
    private String firmwareVersion;
}
