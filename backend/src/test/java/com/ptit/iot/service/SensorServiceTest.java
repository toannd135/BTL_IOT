package com.ptit.iot.service;

import com.ptit.iot.common.BadRequestException;
import com.ptit.iot.config.AppProperties;
import com.ptit.iot.dto.ChartPoint;
import com.ptit.iot.dto.LatestSensorResponse;
import com.ptit.iot.entity.DataSensor;
import com.ptit.iot.entity.Sensor;
import com.ptit.iot.mqtt.SensorDataPayload;
import com.ptit.iot.repository.DataSensorRepository;
import com.ptit.iot.repository.SensorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorServiceTest {
    @Mock SensorRepository sensorRepository;
    @Mock DataSensorRepository dataSensorRepository;
    @Mock RealtimePublisher realtime;

    SensorService service;
    Sensor temp, hum, light;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(new AppProperties.Cors(List.of("*")), 1,
                new AppProperties.Device(10), new AppProperties.Chart(20));
        service = new SensorService(sensorRepository, dataSensorRepository, realtime, props);
        temp = sensor(1, "temperature");
        hum = sensor(2, "humidity");
        light = sensor(3, "light");
    }

    private static Sensor sensor(int id, String name) {
        Sensor s = new Sensor();
        s.setId(id);
        s.setName(name);
        return s;
    }

    @Test
    void ingestStoresThreeRowsAndPushesWebSocket() {
        when(sensorRepository.findAll()).thenReturn(List.of(temp, hum, light));
        when(dataSensorRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        LatestSensorResponse out = service.ingest(new SensorDataPayload.Reading(29.7, 73.1, 610.0, "2026-08-18T10:00:05"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DataSensor>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataSensorRepository).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());
        assertEquals(LocalDateTime.of(2026, 8, 18, 10, 0, 5), captor.getValue().get(0).getTime());
        verify(realtime).publishSensorData(out);
        assertEquals(29.7, out.temperature());
        assertEquals(610.0, out.light());
    }

    @Test
    void latestPicksNewestTimestampAcrossSensors() {
        when(sensorRepository.findAll()).thenReturn(List.of(temp, hum, light));
        LocalDateTime t1 = LocalDateTime.of(2026, 8, 22, 13, 45, 0);
        LocalDateTime t2 = t1.plusSeconds(3);
        when(dataSensorRepository.findTopBySensorIdOrderByTimeDescIdDesc(1)).thenReturn(Optional.of(new DataSensor(temp, 28.5, t1)));
        when(dataSensorRepository.findTopBySensorIdOrderByTimeDescIdDesc(2)).thenReturn(Optional.of(new DataSensor(hum, 65.0, t2)));
        when(dataSensorRepository.findTopBySensorIdOrderByTimeDescIdDesc(3)).thenReturn(Optional.empty());

        LatestSensorResponse latest = service.latest();

        assertEquals(28.5, latest.temperature());
        assertEquals(65.0, latest.humidity());
        assertNull(latest.light());
        assertEquals(t2, latest.timestamp());
    }

    @Test
    void chartGroupsRowsByTimestampAscending() {
        LocalDateTime t1 = LocalDateTime.of(2026, 8, 22, 13, 0, 0);
        LocalDateTime t2 = t1.plusMinutes(5);
        when(dataSensorRepository.findLatest(any(Pageable.class))).thenReturn(List.of(
                new DataSensor(light, 435.0, t2), new DataSensor(hum, 65.5, t2), new DataSensor(temp, 28.1, t2),
                new DataSensor(light, 420.0, t1), new DataSensor(hum, 66.2, t1), new DataSensor(temp, 27.8, t1)));

        List<ChartPoint> points = service.chart(null, 20);

        assertEquals(2, points.size());
        assertEquals(t1, points.get(0).timestamp());
        assertEquals(27.8, points.get(0).temperature());
        assertEquals(66.2, points.get(0).humidity());
        assertEquals(420.0, points.get(0).light());
        assertEquals(t2, points.get(1).timestamp());
    }

    @Test
    void chartFilteredByNameReturnsOnlyThatField() {
        when(sensorRepository.findByNameIgnoreCase("temperature")).thenReturn(Optional.of(temp));
        LocalDateTime t1 = LocalDateTime.of(2026, 8, 22, 13, 0, 0);
        when(dataSensorRepository.findLatestBySensor(eq(1), any(Pageable.class)))
                .thenReturn(List.of(new DataSensor(temp, 27.8, t1)));

        List<ChartPoint> points = service.chart("temperature", 20);

        assertEquals(1, points.size());
        assertEquals(27.8, points.get(0).temperature());
        assertNull(points.get(0).humidity());
        assertNull(points.get(0).light());
    }

    @Test
    void chartRejectsUnknownType() {
        when(sensorRepository.findByNameIgnoreCase("pressure")).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> service.chart("pressure", 20));
    }
}
