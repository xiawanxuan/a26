package com.iot.monitor.device.controller;

import com.iot.monitor.common.dto.ApiResponse;
import com.iot.monitor.common.dto.DeviceDTO;
import com.iot.monitor.common.dto.PageResult;
import com.iot.monitor.common.enums.DeviceStatus;
import com.iot.monitor.device.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public ApiResponse<DeviceDTO> registerDevice(@RequestBody DeviceDTO deviceDTO) {
        return ApiResponse.success(deviceService.registerDevice(deviceDTO));
    }

    @PutMapping("/{id}")
    public ApiResponse<DeviceDTO> updateDevice(@PathVariable Long id, @RequestBody DeviceDTO deviceDTO) {
        return ApiResponse.success(deviceService.updateDevice(id, deviceDTO));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceDTO> getDeviceById(@PathVariable Long id) {
        return ApiResponse.success(deviceService.getDeviceById(id));
    }

    @GetMapping("/code/{deviceCode}")
    public ApiResponse<DeviceDTO> getDeviceByCode(@PathVariable String deviceCode) {
        return ApiResponse.success(deviceService.getDeviceByCode(deviceCode));
    }

    @GetMapping
    public ApiResponse<PageResult<DeviceDTO>> listDevices(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String deviceCode,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String deviceType) {
        return ApiResponse.success(deviceService.listDevices(pageNum, pageSize, deviceCode, deviceName, status, deviceType));
    }

    @PutMapping("/{deviceCode}/status")
    public ApiResponse<Void> updateDeviceStatus(@PathVariable String deviceCode,
                                                 @RequestParam String status) {
        deviceService.updateDeviceStatus(deviceCode, DeviceStatus.valueOf(status));
        return ApiResponse.success();
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Long>> getDeviceStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", deviceService.countTotalDevices());
        stats.put("online", deviceService.countOnlineDevices());
        stats.put("offline", deviceService.countOfflineDevices());
        return ApiResponse.success(stats);
    }
}
