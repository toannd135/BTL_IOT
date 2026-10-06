package com.ptit.iot.repository;

import com.ptit.iot.entity.DataSensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DataSensorRepository extends JpaRepository<DataSensor, Integer>, JpaSpecificationExecutor<DataSensor> {

    Optional<DataSensor> findTopBySensorIdOrderByTimeDescIdDesc(Integer sensorId);

    @EntityGraph(attributePaths = "sensor")
    @Query("select d from DataSensor d order by d.time desc, d.id desc")
    List<DataSensor> findLatest(Pageable pageable);

    @EntityGraph(attributePaths = "sensor")
    @Query("select d from DataSensor d where d.sensor.id = :sensorId order by d.time desc, d.id desc")
    List<DataSensor> findLatestBySensor(@Param("sensorId") Integer sensorId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "sensor")
    Page<DataSensor> findAll(org.springframework.data.jpa.domain.Specification<DataSensor> spec, Pageable pageable);
}
