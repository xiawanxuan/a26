package com.iot.monitor.receiver.controller;

import com.iot.monitor.common.dto.ApiResponse;
import com.iot.monitor.common.dto.BatchSensorDataDTO;
import com.iot.monitor.common.dto.SensorDataDTO;
import com.iot.monitor.common.dto.SensorMetricDTO;
import com.iot.monitor.receiver.service.DataReceiverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class HttpDataReceiverController {

    private final DataReceiverService dataReceiverService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostMapping("/ingest")
    public ApiResponse<String> ingestData(@RequestBody SensorDataDTO sensorDataDTO,
                                           @RequestHeader(value = "X-Device-Token", required = false) String deviceToken) {
        dataReceiverService.receiveData(sensorDataDTO);
        return ApiResponse.success("数据接收成功");
    }

    @PostMapping("/ingest/batch")
    public ApiResponse<Map<String, Object>> ingestBatchData(@RequestBody BatchSensorDataDTO batchDTO) {
        List<String> successMetrics = new ArrayList<>();
        List<String> failedMetrics = new ArrayList<>();

        if (batchDTO.getMetrics() != null) {
            for (SensorMetricDTO metricDTO : batchDTO.getMetrics()) {
                try {
                    SensorDataDTO sensorDataDTO = new SensorDataDTO();
                    sensorDataDTO.setDeviceCode(batchDTO.getDeviceCode());
                    sensorDataDTO.setMetric(metricDTO.getMetric());
                    sensorDataDTO.setValue(metricDTO.getValue());
                    sensorDataDTO.setUnit(metricDTO.getUnit());

                    if (metricDTO.getDataTime() != null) {
                        sensorDataDTO.setDataTime(LocalDateTime.parse(metricDTO.getDataTime(), DATE_TIME_FORMATTER));
                    }

                    dataReceiverService.receiveData(sensorDataDTO);
                    successMetrics.add(metricDTO.getMetric());
                } catch (Exception e) {
                    log.error("Failed to ingest metric {}: {}", metricDTO.getMetric(), e.getMessage());
                    failedMetrics.add(metricDTO.getMetric() + ": " + e.getMessage());
                }
            }
        }

        return ApiResponse.success(Map.of(
                "successCount", successMetrics.size(),
                "failedCount", failedMetrics.size(),
                "failedMetrics", failedMetrics
        ));
    }

    @PostMapping("/ingest/{deviceCode}/{metric}")
    public ApiResponse<String> ingestSimpleData(@PathVariable String deviceCode,
                                                 @PathVariable String metric,
                                                 @RequestParam String value,
                                                 @RequestParam(required = false) String unit) {
        SensorDataDTO sensorDataDTO = new SensorDataDTO();
        sensorDataDTO.setDeviceCode(deviceCode);
        sensorDataDTO.setMetric(metric);
        sensorDataDTO.setValue(new java.math.BigDecimal(value));
        sensorDataDTO.setUnit(unit);

        dataReceiverService.receiveData(sensorDataDTO);
        return ApiResponse.success("数据接收成功");
    }
}
