package com.iot.monitor.receiver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.monitor.common.dto.SensorDataDTO;
import com.iot.monitor.common.entity.SensorData;
import com.iot.monitor.common.event.SensorDataReceivedEvent;
import com.iot.monitor.common.repository.SensorDataRepository;
import com.iot.monitor.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataReceiverService {

    private final SensorDataRepository sensorDataRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public SensorData receiveData(SensorDataDTO sensorDataDTO) {
        if (sensorDataDTO.getDeviceCode() == null || sensorDataDTO.getDeviceCode().isEmpty()) {
            throw new IllegalArgumentException("设备编号不能为空");
        }
        if (sensorDataDTO.getMetric() == null || sensorDataDTO.getMetric().isEmpty()) {
            throw new IllegalArgumentException("指标名称不能为空");
        }
        if (sensorDataDTO.getValue() == null) {
            throw new IllegalArgumentException("指标值不能为空");
        }

        SensorData sensorData = new SensorData();
        sensorData.setDeviceCode(sensorDataDTO.getDeviceCode());
        sensorData.setMetric(sensorDataDTO.getMetric());
        sensorData.setValue(sensorDataDTO.getValue());
        sensorData.setUnit(sensorDataDTO.getUnit());
        sensorData.setDataTime(sensorDataDTO.getDataTime() != null ? 
                sensorDataDTO.getDataTime() : LocalDateTime.now());

        if (sensorDataDTO.getExtra() != null && !sensorDataDTO.getExtra().isEmpty()) {
            try {
                sensorData.setExtraInfo(objectMapper.writeValueAsString(sensorDataDTO.getExtra()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize extra info", e);
            }
        }

        sensorData = sensorDataRepository.save(sensorData);

        deviceService.updateDeviceLastDataTime(sensorDataDTO.getDeviceCode());

        eventPublisher.publishEvent(new SensorDataReceivedEvent(this, sensorDataDTO));

        log.debug("Received sensor data: device={}, metric={}, value={}",
                sensorDataDTO.getDeviceCode(), sensorDataDTO.getMetric(), sensorDataDTO.getValue());

        return sensorData;
    }
}
