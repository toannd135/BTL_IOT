package com.ptit.iot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses the timestamps the ESP8266 firmware produces ("2026-08-26T10:33:34" or
 * "2026-08-26 10:33:34"). Also accepts ISO-8601 with an offset/Z, converting it to
 * server-local time.
 *
 * <p>The board builds its timestamp from NTP, and until the first successful sync it
 * reports epoch time (1970-01-01). Storing that would corrupt the chart and the
 * "latest reading" ordering, so any implausible value falls back to server time.
 */
public final class TimeParser {
    private static final Logger log = LoggerFactory.getLogger(TimeParser.class);
    private static final DateTimeFormatter SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Readings older than this came from a device whose clock is not set yet. */
    private static final LocalDateTime MIN_PLAUSIBLE = LocalDateTime.of(2020, 1, 1, 0, 0);
    /** Tolerance for a device clock running slightly ahead of the server. */
    private static final long MAX_FUTURE_HOURS = 24;

    private TimeParser() {}

    public static LocalDateTime parseOrNow(String raw) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime parsed = parse(raw);
        if (parsed == null) return now;

        if (parsed.isBefore(MIN_PLAUSIBLE)) {
            log.warn("Device clock not synchronised (timestamp '{}'), using server time instead", raw);
            return now;
        }
        if (parsed.isAfter(now.plusHours(MAX_FUTURE_HOURS))) {
            log.warn("Device timestamp '{}' is too far in the future, using server time instead", raw);
            return now;
        }
        return parsed;
    }

    private static LocalDateTime parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDateTime.parse(s, SPACE);
        } catch (DateTimeParseException ignored) { }
        try {
            return OffsetDateTime.parse(s).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) { }
        log.debug("Unparseable timestamp '{}'", raw);
        return null;
    }
}
