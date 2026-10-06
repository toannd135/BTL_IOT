package com.ptit.iot.common;

public class BadRequestException extends RuntimeException {
    private final String detail;

    public BadRequestException(String message, String detail) {
        super(message);
        this.detail = detail;
    }

    public String getDetail() { return detail; }
}
