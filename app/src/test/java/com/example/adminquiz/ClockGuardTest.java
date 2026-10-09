package com.example.adminquiz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.adminquiz.analytics.ClockGuard;
import com.example.adminquiz.utils.ClassCode;

import org.junit.Test;

public class ClockGuardTest {
    private static final long MIN = 60_000L;
    private static final long DAY = 24 * 60 * MIN;
    private static final long T0 = 1_800_000_000_000L;

    @Test
    public void firstResultUsesTheWallClock() {
        assertEquals(T0, ClockGuard.resolve(T0, 5_000, false, 0, 0));
    }

    @Test
    public void normalPlayAcceptsTheWallClock() {
        long wall = T0 + 30 * MIN;
        assertEquals(wall, ClockGuard.resolve(wall, 100_000 + 30 * MIN, true, T0, 100_000));
    }

    @Test
    public void rollingTheClockBackCannotBackdateAResult() {
        // 30 minutes of real time passed, but the wall clock was set back two days.
        long wall = T0 - 2 * DAY;
        long got = ClockGuard.resolve(wall, 100_000 + 30 * MIN, true, T0, 100_000);
        assertEquals(T0 + 30 * MIN, got);
    }

    @Test
    public void jumpingTheClockForwardCannotAddAFakeDay() {
        long wall = T0 + 3 * DAY;
        long got = ClockGuard.resolve(wall, 100_000 + 10 * MIN, true, T0, 100_000);
        assertEquals(T0 + 10 * MIN, got);
    }

    @Test
    public void smallDriftIsTolerated() {
        long wall = T0 + 30 * MIN + 2 * MIN;
        assertEquals(wall, ClockGuard.resolve(wall, 100_000 + 30 * MIN, true, T0, 100_000));
    }

    @Test
    public void afterARebootTimeStillNeverGoesBackwards() {
        // elapsedRealtime restarted (smaller than the stored value).
        assertEquals(T0, ClockGuard.resolve(T0 - DAY, 3_000, true, T0, 900_000));
        assertEquals(T0 + DAY, ClockGuard.resolve(T0 + DAY, 3_000, true, T0, 900_000));
    }

    @Test
    public void classCodesAreNormalisedAndValidated() {
        assertEquals("GENERAL", ClassCode.normalize(null));
        assertEquals("GENERAL", ClassCode.normalize("   "));
        assertEquals("CS-2A", ClassCode.normalize(" cs-2a "));
        assertNull(ClassCode.normalize("a"));
        assertNull(ClassCode.normalize("has space"));
        assertNull(ClassCode.normalize("this-code-is-way-too-long-123"));
        assertNull(ClassCode.normalize("bad!"));
        assertEquals("GENERAL", ClassCode.of(null));
        assertEquals("XY1", ClassCode.of("XY1"));
    }
}
