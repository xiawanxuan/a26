package com.iot.monitor.receiver.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.monitor.common.dto.SensorDataDTO;
import com.iot.monitor.receiver.service.DataReceiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = false)
public class MqttDataReceiver {

    private final DataReceiverService dataReceiverService;
    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<String> message) {
        String topic = message.getHeaders().get("mqtt_receivedTopic", String.class);
        String payload = message.getPayload();

        log.debug("Received MQTT message - topic: {}, payload: {}", topic, payload);

        try {
            if (topic != null && topic.startsWith("iot/data/")) {
                String deviceCode = extractDeviceCodeFromTopic(topic);
                if (deviceCode != null) {
                    processDeviceData(deviceCode, payload);
                }
            } else {
                processGenericData(payload);
            }
        } catch (Exception e) {
            log.error("Failed to process MQTT message - topic: {}, payload: {}", topic, payload, e);
        }
    }

    private String extractDeviceCodeFromTopic(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }

    private void processDeviceData(String deviceCode, String payload) throws Exception {
        Map<String, Object> dataMap = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});

        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
            String metric = entry.getKey();
            Object value = entry.getValue();

            if (isNumericValue(value)) {
                SensorDataDTO sensorDataDTO = new SensorDataDTO();
                sensorDataDTO.setDeviceCode(deviceCode);
                sensorDataDTO.setMetric(metric);
                sensorDataDTO.setValue(new BigDecimal(value.toString()));

                Object timestamp = dataMap.get("timestamp");
                if (timestamp != null) {
                    try {
                        sensorDataDTO.setDataTime(LocalDateTime.parse(timestamp.toString(), DATE_TIME_FORMATTER));
                    } catch (Exception e) {
                        log.debug("Failed to parse timestamp: {}", timestamp);
                    }
                }

                dataReceiverService.receiveData(sensorDataDTO);
            }
        }
    }

    private void processGenericData(String payload) throws Exception {
        SensorDataDTO sensorDataDTO = objectMapper.readValue(payload, SensorDataDTO.class);
        dataReceiverService.receiveData(sensorDataDTO);
    }

    private boolean isNumericValue(Object value) {
        if (value == null) return false;
        try {
            new BigDecimal(value.toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
