package com.ptit.iot.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.iot.common.DeviceControlException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around the Eclipse Paho client.
 *  - Subscribes to sensor-data and device-response topics and forwards them to {@link MqttMessageListener}.
 *  - Publishes control commands to the device-control topic.
 *  - Connects asynchronously with retry so the HTTP API still boots when the broker is down.
 */
@Component
public class MqttGateway implements MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttGateway.class);
    private static final long RETRY_SECONDS = 5;

    private final MqttProperties props;
    private final ObjectMapper objectMapper;
    private final MqttMessageListener listener;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mqtt-connector");
        t.setDaemon(true);
        return t;
    });

    private volatile MqttClient client;

    public MqttGateway(MqttProperties props, ObjectMapper objectMapper, @Lazy MqttMessageListener listener) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.listener = listener;
    }

    @PostConstruct
    void start() {
        if (!props.enabled()) {
            log.warn("MQTT is disabled (app.mqtt.enabled=false). Device control will fail and no sensor data will be ingested.");
            return;
        }
        scheduler.execute(this::connectWithRetry);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
        MqttClient c = client;
        if (c != null) {
            try {
                if (c.isConnected()) c.disconnect();
                c.close();
            } catch (MqttException e) {
                log.debug("Error closing MQTT client", e);
            }
        }
    }

    private void connectWithRetry() {
        try {
            if (client == null) {
                client = new MqttClient(props.brokerUrl(), props.clientId() + "-" + System.currentTimeMillis() % 100000, new MemoryPersistence());
                client.setCallback(this);
            }
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            if (props.username() != null && !props.username().isBlank()) {
                options.setUserName(props.username());
                options.setPassword(props.password() == null ? new char[0] : props.password().toCharArray());
            }
            log.info("Connecting to MQTT broker {} ...", props.brokerUrl());
            client.connect(options);
        } catch (MqttException e) {
            log.warn("MQTT connect failed ({}). Retrying in {}s", e.getMessage(), RETRY_SECONDS);
            if (!scheduler.isShutdown()) {
                scheduler.schedule(this::connectWithRetry, RETRY_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    public boolean isConnected() {
        MqttClient c = client;
        return c != null && c.isConnected();
    }

    /** Publishes a control command. Throws {@link DeviceControlException} when the broker is unreachable. */
    public void publishDeviceControl(DeviceControlPayload payload) {
        MqttClient c = client;
        if (c == null || !c.isConnected()) {
            throw new DeviceControlException("Device is offline or MQTT broker connection failed", null);
        }
        try {
            byte[] body = objectMapper.writeValueAsBytes(payload);
            MqttMessage message = new MqttMessage(body);
            message.setQos(props.qos());
            c.publish(props.topics().deviceControl(), message);
            log.info("[MQTT out] {} {}", props.topics().deviceControl(), new String(body, StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new DeviceControlException("Failed to serialize control payload", e);
        } catch (MqttException e) {
            throw new DeviceControlException("Device is offline or MQTT broker connection failed: " + e.getMessage(), e);
        }
    }

    // ---- MqttCallbackExtended ----

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT {} to {}", reconnect ? "reconnected" : "connected", serverURI);
        try {
            client.subscribe(props.topics().sensorData(), props.qos());
            client.subscribe(props.topics().deviceResponse(), props.qos());
            log.info("Subscribed to {} and {}", props.topics().sensorData(), props.topics().deviceResponse());
        } catch (MqttException e) {
            log.error("MQTT subscribe failed", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("[MQTT in] {} {}", topic, payload);
        try {
            if (topic.equals(props.topics().sensorData())) {
                listener.onSensorData(payload);
            } else if (topic.equals(props.topics().deviceResponse())) {
                listener.onDeviceResponse(payload);
            } else {
                log.debug("Ignoring message on unexpected topic {}", topic);
            }
        } catch (Exception e) {
            // Never let an exception propagate: Paho would drop the connection.
            log.error("Error handling MQTT message on {}: {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // no-op
    }
}
