package com.ptit.iot.service;

import com.ptit.iot.common.BadRequestException;
import com.ptit.iot.common.DeviceControlException;
import com.ptit.iot.config.AppProperties;
import com.ptit.iot.dto.DeviceControlRequest;
import com.ptit.iot.dto.DeviceControlResponse;
import com.ptit.iot.dto.DeviceStatusMessage;
import com.ptit.iot.entity.Action;
import com.ptit.iot.entity.ActionStatus;
import com.ptit.iot.entity.Device;
import com.ptit.iot.entity.User;
import com.ptit.iot.mqtt.DeviceControlPayload;
import com.ptit.iot.mqtt.DeviceResponsePayload;
import com.ptit.iot.mqtt.MqttGateway;
import com.ptit.iot.repository.ActionRepository;
import com.ptit.iot.repository.DeviceRepository;
import com.ptit.iot.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {
    @Mock DeviceRepository deviceRepository;
    @Mock UserRepository userRepository;
    @Mock ActionRepository actionRepository;
    @Mock MqttGateway mqtt;
    @Mock RealtimePublisher realtime;

    DeviceService service;
    Device led1;
    User admin;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(new AppProperties.Cors(List.of("*")), 1,
                new AppProperties.Device(10), new AppProperties.Chart(20));
        service = new DeviceService(deviceRepository, userRepository, actionRepository, mqtt, realtime, props);
        led1 = new Device();
        led1.setId(1);
        led1.setName("LED 1");
        admin = new User();
        admin.setId(1);
        admin.setUsername("admin");
    }

    @Test
    void controlRejectsInvalidAction() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.control(new DeviceControlRequest(1, "BLINK", null)));
        assertEquals("The 'action' field must be either 'ON' or 'OFF'", e.getDetail());
        verifyNoInteractions(mqtt);
    }

    @Test
    void controlSavesPendingAndPublishes() {
        when(deviceRepository.findById(1)).thenReturn(Optional.of(led1));
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceControlResponse res = service.control(new DeviceControlRequest(1, "on", null));

        assertEquals("ON", res.state());
        assertEquals("PENDING", res.status());
        verify(mqtt).publishDeviceControl(new DeviceControlPayload(1, "ON", 1));
    }

    @Test
    void controlMarksFailedWhenBrokerDown() {
        when(deviceRepository.findById(1)).thenReturn(Optional.of(led1));
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new DeviceControlException("broker down", null)).when(mqtt).publishDeviceControl(any());

        assertThrows(DeviceControlException.class, () -> service.control(new DeviceControlRequest(1, "OFF", null)));

        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(actionRepository, times(2)).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
        verify(realtime).publishDeviceStatus(any(DeviceStatusMessage.class));
    }

    @Test
    void deviceResponseResolvesPendingActionAndPushesStatus() {
        Action pending = new Action(led1, admin, "ON", ActionStatus.PENDING, LocalDateTime.now());
        when(deviceRepository.findById(1)).thenReturn(Optional.of(led1));
        when(actionRepository.findByDeviceAndStatus(eq(1), eq("PENDING"), any(Pageable.class))).thenReturn(List.of(pending));
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleDeviceResponse(new DeviceResponsePayload(1, 1, "ON", "SUCCESS", "2026-08-23 10:10:00"));

        assertEquals("SUCCESS", pending.getStatus());
        assertEquals(LocalDateTime.of(2026, 8, 23, 10, 10, 0), pending.getTime());
        ArgumentCaptor<DeviceStatusMessage> captor = ArgumentCaptor.forClass(DeviceStatusMessage.class);
        verify(realtime).publishDeviceStatus(captor.capture());
        assertEquals("SUCCESS", captor.getValue().status());
        assertEquals(1, captor.getValue().deviceId());
    }

    @Test
    void deviceResponseWithoutPendingCreatesNewRow() {
        when(deviceRepository.findById(1)).thenReturn(Optional.of(led1));
        when(actionRepository.findByDeviceAndStatus(eq(1), eq("PENDING"), any(Pageable.class))).thenReturn(List.of());
        when(userRepository.findById(2)).thenReturn(Optional.of(admin));
        when(actionRepository.save(any(Action.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handleDeviceResponse(new DeviceResponsePayload(1, 2, "OFF", "SUCCESS", null));

        ArgumentCaptor<Action> captor = ArgumentCaptor.forClass(Action.class);
        verify(actionRepository).save(captor.capture());
        assertEquals("OFF", captor.getValue().getAction());
        assertEquals("SUCCESS", captor.getValue().getStatus());
    }
}
