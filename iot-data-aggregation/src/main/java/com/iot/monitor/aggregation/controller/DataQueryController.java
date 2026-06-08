package com.iot.monitor.aggregation.controller;

import com.iot.monitor.common.dto.ApiResponse;
import com.iot.monitor.common.entity.AggregatedData;
import com.iot.monitor.common.entity.SensorData;
import com.iot.monitor.common.enums.AggregationPeriod;
import com.iot.monitor.common.repository.SensorDataRepository;
import com.iot.monitor.aggregation.service.DataAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataQueryController {

    private final DataAggregationService dataAggregationService;
    private final SensorDataRepository sensorDataRepository;

    @GetMapping("/history/{deviceCode}")
    public ApiResponse<Page<SensorData>> getHistoryData(
            @PathVariable String deviceCode,
            @RequestParam(required = false) String metric,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize) {

        Page<SensorData> result;
        if (metric != null && !metric.isEmpty()) {
            result = sensorDataRepository.findByDeviceCodeAndMetricOrderByDataTimeDesc(
                    deviceCode, metric, PageRequest.of(pageNum - 1, pageSize));
        } else {
            result = sensorDataRepository.findByDeviceCodeOrderByDataTimeDesc(
                    deviceCode, PageRequest.of(pageNum - 1, pageSize));
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/aggregated")
    public ApiResponse<List<AggregatedData>> getAggregatedData(
            @RequestParam String deviceCode,
            @RequestParam String metric,
            @RequestParam AggregationPeriod period,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        List<AggregatedData> result = dataAggregationService.getAggregatedData(
                deviceCode, metric, period, startTime, endTime);
        return ApiResponse.success(result);
    }

    @GetMapping("/realtime/{deviceCode}/{metric}")
    public ApiResponse<Map<String, Object>> getRealtimeStatistics(
            @PathVariable String deviceCode,
            @PathVariable String metric,
            @RequestParam(defaultValue = "60") int minutes) {

        Map<String, Object> result = dataAggregationService.getRealtimeStatistics(
                deviceCode, metric, minutes);
        return ApiResponse.success(result);
    }

    @GetMapping("/latest/{deviceCode}")
    public ApiResponse<List<SensorData>> getLatestData(
            @PathVariable String deviceCode,
            @RequestParam(defaultValue = "10") int limit) {

        Page<SensorData> page = sensorDataRepository.findByDeviceCodeOrderByDataTimeDesc(
                deviceCode, PageRequest.of(0, limit));
        return ApiResponse.success(page.getContent());
    }
}
