package com.iot.monitor.common.event;

import com.iot.monitor.common.dto.SensorDataDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class SensorDataReceivedEvent extends ApplicationEvent {

    private final SensorDataDTO sensorData;
    private final LocalDateTime receivedTime;

    public SensorDataReceivedEvent(Object source, SensorDataDTO sensorData) {
        super(source);
        this.sensorData = sensorData;
        this.receivedTime = LocalDateTime.now();
    }
}
