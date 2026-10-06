package com.ptit.iot.common;

/** Raised when a control command cannot be delivered to the MQTT broker. Maps to HTTP 500 as in the design. */
public class DeviceControlException extends RuntimeException {
    public DeviceControlException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
