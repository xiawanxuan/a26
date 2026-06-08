package com.iot.monitor.device.service;

import com.iot.monitor.common.entity.Device;
import com.iot.monitor.common.enums.DeviceStatus;
import com.iot.monitor.common.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceHeartbeatService {

    private final DeviceRepository deviceRepository;

    private static final int OFFLINE_THRESHOLD_MINUTES = 5;

    @Scheduled(fixedRate = 60000)
    public void checkOfflineDevices() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusMinutes(OFFLINE_THRESHOLD_MINUTES);
        List<Device> onlineDevices = deviceRepository.findByStatus(DeviceStatus.ONLINE);

        int offlineCount = 0;
        for (Device device : onlineDevices) {
            if (device.getLastDataTime() == null || device.getLastDataTime().isBefore(thresholdTime)) {
                device.setStatus(DeviceStatus.OFFLINE);
                deviceRepository.save(device);
                offlineCount++;
                log.warn("Device {} is offline due to no data for {} minutes",
                        device.getDeviceCode(), OFFLINE_THRESHOLD_MINUTES);
            }
        }

        if (offlineCount > 0) {
            log.info("Checked {} online devices, {} marked as offline", onlineDevices.size(), offlineCount);
        }
    }
}
