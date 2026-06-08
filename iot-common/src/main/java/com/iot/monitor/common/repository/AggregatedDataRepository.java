package com.iot.monitor.common.repository;

import com.iot.monitor.common.entity.AggregatedData;
import com.iot.monitor.common.enums.AggregationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AggregatedDataRepository extends JpaRepository<AggregatedData, Long>, JpaSpecificationExecutor<AggregatedData> {

    List<AggregatedData> findByDeviceCodeAndMetricAndPeriodAndPeriodStartBetweenOrderByPeriodStartAsc(
            String deviceCode, String metric, AggregationPeriod period,
            LocalDateTime startTime, LocalDateTime endTime);

    Optional<AggregatedData> findByDeviceCodeAndMetricAndPeriodAndPeriodStart(
            String deviceCode, String metric, AggregationPeriod period, LocalDateTime periodStart);

    List<AggregatedData> findByDeviceCodeAndPeriodAndPeriodStartBetweenOrderByPeriodStartAsc(
            String deviceCode, AggregationPeriod period, LocalDateTime startTime, LocalDateTime endTime);

    void deleteByDeviceCodeAndPeriodAndPeriodStartBefore(String deviceCode, AggregationPeriod period, LocalDateTime beforeTime);
}
