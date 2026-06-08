package com.iot.monitor.alert.controller;

import com.iot.monitor.alert.service.AlertService;
import com.iot.monitor.common.dto.AlertRecordDTO;
import com.iot.monitor.common.dto.ApiResponse;
import com.iot.monitor.common.dto.PageResult;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts/records")
public class AlertRecordController {

    private final AlertService alertService;

    public AlertRecordController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/{id}")
    public ApiResponse<AlertRecordDTO> getAlertById(@PathVariable Long id) {
        return ApiResponse.success(alertService.getAlertById(id));
    }

    @GetMapping("/code/{alertCode}")
    public ApiResponse<AlertRecordDTO> getAlertByCode(@PathVariable String alertCode) {
        return ApiResponse.success(alertService.getAlertByCode(alertCode));
    }

    @GetMapping
    public ApiResponse<PageResult<AlertRecordDTO>> listAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alertLevel,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return ApiResponse.success(alertService.listAlerts(pageNum, pageSize, deviceCode, status, alertLevel, alertType, startTime, endTime));
    }

    @PutMapping("/{id}/acknowledge")
    public ApiResponse<AlertRecordDTO> acknowledgeAlert(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success(alertService.acknowledgeAlert(id, operator));
    }

    @PutMapping("/{id}/resolve")
    public ApiResponse<AlertRecordDTO> resolveAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String resolveNote,
            @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success(alertService.resolveAlert(id, resolveNote, operator));
    }

    @PutMapping("/{id}/close")
    public ApiResponse<AlertRecordDTO> closeAlert(
            @PathVariable Long id,
            @RequestParam(required = false) String closeNote,
            @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success(alertService.closeAlert(id, closeNote, operator));
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getAlertStatistics() {
        return ApiResponse.success(alertService.getAlertStatistics());
    }
}
