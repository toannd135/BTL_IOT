package com.ptit.iot.entity;

public enum ActionStatus {
    PENDING, SUCCESS, FAILED;

    public static ActionStatus fromString(String raw) {
        if (raw == null) return null;
        try {
            return ActionStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
