package com.iot.monitor.common.repository;

import com.iot.monitor.common.entity.SensorData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {

    Page<SensorData> findByDeviceCodeOrderByDataTimeDesc(String deviceCode, Pageable pageable);

    Page<SensorData> findByDeviceCodeAndMetricOrderByDataTimeDesc(String deviceCode, String metric, Pageable pageable);

    List<SensorData> findByDeviceCodeAndDataTimeAfterOrderByDataTimeAsc(String deviceCode, LocalDateTime startTime);

    List<SensorData> findByDeviceCodeAndMetricAndDataTimeBetweenOrderByDataTimeAsc(
            String deviceCode, String metric, LocalDateTime startTime, LocalDateTime endTime);

    @Query("SELECT s FROM SensorData s WHERE s.deviceCode = :deviceCode AND s.metric = :metric " +
           "AND s.dataTime >= :startTime ORDER BY s.dataTime DESC")
    List<SensorData> findLatestByDeviceAndMetric(
            @Param("deviceCode") String deviceCode,
            @Param("metric") String metric,
            @Param("startTime") LocalDateTime startTime,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM SensorData s WHERE s.deviceCode = :deviceCode AND s.metric = :metric " +
           "AND s.dataTime >= :startTime AND s.dataTime < :endTime")
    long countByDeviceAndMetricAndTimeRange(
            @Param("deviceCode") String deviceCode,
            @Param("metric") String metric,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
