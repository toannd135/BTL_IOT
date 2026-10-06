package com.ptit.iot.mqtt;

/** Implemented by the service layer; MqttGateway routes inbound messages here. */
public interface MqttMessageListener {
    void onSensorData(String payload);
    void onDeviceResponse(String payload);
}
