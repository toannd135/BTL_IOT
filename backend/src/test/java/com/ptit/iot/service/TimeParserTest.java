package com.ptit.iot.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeParserTest {

    @Test
    void parsesFirmwareIsoLocal() {
        assertEquals(LocalDateTime.of(2026, 8, 26, 10, 33, 34), TimeParser.parseOrNow("2026-08-26T10:33:34"));
    }

    @Test
    void parsesSpaceSeparated() {
        assertEquals(LocalDateTime.of(2026, 8, 23, 10, 10, 0), TimeParser.parseOrNow("2026-08-23 10:10:00"));
    }

    @Test
    void parsesOffsetFormat() {
        assertNotNull(TimeParser.parseOrNow("2026-08-22T13:45:00Z"));
    }

    @Test
    void fallsBackToServerTimeOnGarbageOrNull() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(2);
        assertTrue(TimeParser.parseOrNow("not a date").isAfter(before));
        assertTrue(TimeParser.parseOrNow(null).isAfter(before));
    }

    @Test
    void rejectsEpochTimestampFromUnsyncedDeviceClock() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(2);
        // what the board sends before its first successful NTP sync
        LocalDateTime parsed = TimeParser.parseOrNow("1970-01-01T07:03:31");
        assertTrue(parsed.isAfter(before), "epoch timestamp must be replaced by server time");
    }

    @Test
    void rejectsTimestampTooFarInTheFuture() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(2);
        String farFuture = LocalDateTime.now().plusYears(5).toString();
        assertTrue(TimeParser.parseOrNow(farFuture).isAfter(before));
    }

    @Test
    void acceptsSmallClockDriftAhead() {
        LocalDateTime slightlyAhead = LocalDateTime.now().plusHours(1).withNano(0);
        assertEquals(slightlyAhead, TimeParser.parseOrNow(slightlyAhead.toString()));
    }
}
