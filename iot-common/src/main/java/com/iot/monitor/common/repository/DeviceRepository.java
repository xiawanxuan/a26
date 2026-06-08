package com.iot.monitor.common.repository;

import com.iot.monitor.common.entity.Device;
import com.iot.monitor.common.enums.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    Optional<Device> findByDeviceCode(String deviceCode);

    boolean existsByDeviceCode(String deviceCode);

    List<Device> findByStatus(DeviceStatus status);

    List<Device> findByDeviceType(String deviceType);

    long countByStatus(DeviceStatus status);
}
