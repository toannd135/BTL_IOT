package com.ptit.iot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.iot.mqtt.DeviceResponsePayload;
import com.ptit.iot.mqtt.MqttMessageListener;
import com.ptit.iot.mqtt.SensorDataPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Decodes inbound MQTT JSON and dispatches to the domain services. */
@Service
public class MqttIngestService implements MqttMessageListener {
    private static final Logger log = LoggerFactory.getLogger(MqttIngestService.class);

    private final ObjectMapper objectMapper;
    private final SensorService sensorService;
    private final DeviceService deviceService;

    public MqttIngestService(ObjectMapper objectMapper, SensorService sensorService, DeviceService deviceService) {
        this.objectMapper = objectMapper;
        this.sensorService = sensorService;
        this.deviceService = deviceService;
    }

    @Override
    public void onSensorData(String payload) {
        try {
            SensorDataPayload parsed = objectMapper.readValue(payload, SensorDataPayload.class);
            if (parsed.data() == null || parsed.data().isEmpty()) {
                // tolerate a flat object { temperature, humidity, light, timestamp }
                SensorDataPayload.Reading flat = objectMapper.readValue(payload, SensorDataPayload.Reading.class);
                sensorService.ingest(flat);
                return;
            }
            for (SensorDataPayload.Reading r : parsed.data()) sensorService.ingest(r);
        } catch (Exception e) {
            log.error("Invalid sensor-data payload '{}': {}", payload, e.getMessage());
        }
    }

    @Override
    public void onDeviceResponse(String payload) {
        try {
            DeviceResponsePayload parsed = objectMapper.readValue(payload, DeviceResponsePayload.class);
            deviceService.handleDeviceResponse(parsed);
        } catch (Exception e) {
            log.error("Invalid device-response payload '{}': {}", payload, e.getMessage());
        }
    }
}
