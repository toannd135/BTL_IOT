package com.ptit.iot.repository;

import com.ptit.iot.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Integer> {
    Optional<Sensor> findByNameIgnoreCase(String name);
}
