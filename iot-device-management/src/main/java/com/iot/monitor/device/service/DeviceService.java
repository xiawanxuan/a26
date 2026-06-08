package com.iot.monitor.device.service;

import com.iot.monitor.common.dto.DeviceDTO;
import com.iot.monitor.common.dto.PageResult;
import com.iot.monitor.common.entity.Device;
import com.iot.monitor.common.enums.DeviceStatus;
import com.iot.monitor.common.enums.ProtocolType;
import com.iot.monitor.common.exception.BusinessException;
import com.iot.monitor.common.repository.DeviceRepository;
import com.iot.monitor.common.util.IdGenerator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional
    public DeviceDTO registerDevice(DeviceDTO deviceDTO) {
        if (deviceRepository.existsByDeviceCode(deviceDTO.getDeviceCode())) {
            throw new BusinessException("设备编号已存在");
        }

        Device device = new Device();
        device.setDeviceCode(deviceDTO.getDeviceCode() != null ? 
                deviceDTO.getDeviceCode() : IdGenerator.generateDeviceCode());
        device.setDeviceName(deviceDTO.getDeviceName());
        device.setDescription(deviceDTO.getDescription());
        device.setDeviceType(deviceDTO.getDeviceType());
        device.setStatus(deviceDTO.getStatus() != null ? 
                DeviceStatus.valueOf(deviceDTO.getStatus()) : DeviceStatus.INACTIVE);
        device.setProtocolType(deviceDTO.getProtocolType() != null ? 
                ProtocolType.valueOf(deviceDTO.getProtocolType()) : ProtocolType.MQTT);
        device.setMqttTopic(deviceDTO.getMqttTopic());
        device.setLocation(deviceDTO.getLocation());
        device.setFirmwareVersion(deviceDTO.getFirmwareVersion());

        device = deviceRepository.save(device);
        log.info("Device registered: {}", device.getDeviceCode());
        return convertToDTO(device);
    }

    @Transactional
    public DeviceDTO updateDevice(Long id, DeviceDTO deviceDTO) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("设备不存在"));

        if (deviceDTO.getDeviceName() != null) {
            device.setDeviceName(deviceDTO.getDeviceName());
        }
        if (deviceDTO.getDescription() != null) {
            device.setDescription(deviceDTO.getDescription());
        }
        if (deviceDTO.getDeviceType() != null) {
            device.setDeviceType(deviceDTO.getDeviceType());
        }
        if (deviceDTO.getStatus() != null) {
            device.setStatus(DeviceStatus.valueOf(deviceDTO.getStatus()));
        }
        if (deviceDTO.getProtocolType() != null) {
            device.setProtocolType(ProtocolType.valueOf(deviceDTO.getProtocolType()));
        }
        if (deviceDTO.getMqttTopic() != null) {
            device.setMqttTopic(deviceDTO.getMqttTopic());
        }
        if (deviceDTO.getLocation() != null) {
            device.setLocation(deviceDTO.getLocation());
        }
        if (deviceDTO.getFirmwareVersion() != null) {
            device.setFirmwareVersion(deviceDTO.getFirmwareVersion());
        }

        device = deviceRepository.save(device);
        log.info("Device updated: {}", device.getDeviceCode());
        return convertToDTO(device);
    }

    @Transactional
    public void deleteDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("设备不存在"));
        deviceRepository.delete(device);
        log.info("Device deleted: {}", device.getDeviceCode());
    }

    public DeviceDTO getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("设备不存在"));
        return convertToDTO(device);
    }

    public DeviceDTO getDeviceByCode(String deviceCode) {
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new BusinessException("设备不存在"));
        return convertToDTO(device);
    }

    public Device getDeviceEntityByCode(String deviceCode) {
        return deviceRepository.findByDeviceCode(deviceCode).orElse(null);
    }

    public PageResult<DeviceDTO> listDevices(int pageNum, int pageSize, String deviceCode,
                                             String deviceName, String status, String deviceType) {
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);

        Specification<Device> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (deviceCode != null && !deviceCode.isEmpty()) {
                predicates.add(cb.like(root.get("deviceCode"), "%" + deviceCode + "%"));
            }
            if (deviceName != null && !deviceName.isEmpty()) {
                predicates.add(cb.like(root.get("deviceName"), "%" + deviceName + "%"));
            }
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), DeviceStatus.valueOf(status)));
            }
            if (deviceType != null && !deviceType.isEmpty()) {
                predicates.add(cb.equal(root.get("deviceType"), deviceType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Device> page = deviceRepository.findAll(spec, pageable);
        List<DeviceDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        return PageResult.of(dtoList, page.getTotalElements(), pageNum, pageSize);
    }

    @Transactional
    public void updateDeviceStatus(String deviceCode, DeviceStatus status) {
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new BusinessException("设备不存在"));
        device.setStatus(status);
        if (status == DeviceStatus.ONLINE) {
            device.setLastOnlineTime(LocalDateTime.now());
        }
        deviceRepository.save(device);
        log.info("Device {} status updated to {}", deviceCode, status);
    }

    @Transactional
    public void updateDeviceLastDataTime(String deviceCode) {
        Device device = deviceRepository.findByDeviceCode(deviceCode).orElse(null);
        if (device != null) {
            device.setLastDataTime(LocalDateTime.now());
            if (device.getStatus() != DeviceStatus.ONLINE) {
                device.setStatus(DeviceStatus.ONLINE);
                device.setLastOnlineTime(LocalDateTime.now());
            }
            deviceRepository.save(device);
        }
    }

    public long countOnlineDevices() {
        return deviceRepository.countByStatus(DeviceStatus.ONLINE);
    }

    public long countOfflineDevices() {
        return deviceRepository.countByStatus(DeviceStatus.OFFLINE);
    }

    public long countTotalDevices() {
        return deviceRepository.count();
    }

    private DeviceDTO convertToDTO(Device device) {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(device.getId());
        dto.setDeviceCode(device.getDeviceCode());
        dto.setDeviceName(device.getDeviceName());
        dto.setDescription(device.getDescription());
        dto.setDeviceType(device.getDeviceType());
        dto.setStatus(device.getStatus().name());
        dto.setProtocolType(device.getProtocolType().name());
        dto.setMqttTopic(device.getMqttTopic());
        dto.setLocation(device.getLocation());
        dto.setFirmwareVersion(device.getFirmwareVersion());
        return dto;
    }
}
