package com.example.adminquiz.analytics;

/**
 * Decides which timestamp a new quiz result is allowed to carry. Pure Java, unit-tested.
 *
 * A student could set the phone clock back to fill missed days, or forward to fake new ones.
 * Rules:
 * - time never goes backwards: a result is never dated before the previous one;
 * - inside one boot, SystemClock.elapsedRealtime() keeps ticking whatever the wall clock says,
 *   so when the wall clock disagrees with it by more than {@link #TOLERANCE_MS} the elapsed-time
 *   estimate is used instead of the wall clock;
 * - after a reboot elapsedRealtime restarts, so the wall clock is accepted but still not below
 *   the last trusted time.
 *
 * A phone that is fully offline cannot prove the real date: setting the clock forward and then
 * rebooting is still accepted here, which is why results dated in the future are also ignored by
 * {@link StreakCalculator}. Tamper-proof streaks need server time (see CLASS_FEATURES.md).
 */
public final class ClockGuard {
    public static final long TOLERANCE_MS = 10L * 60 * 1000;

    private ClockGuard() {
    }

    public static long resolve(long wallMs, long elapsedMs, boolean hasLast,
                               long lastTrustedMs, long lastElapsedMs) {
        if (!hasLast) {
            return wallMs;
        }
        if (elapsedMs >= lastElapsedMs) {
            long expected = lastTrustedMs + (elapsedMs - lastElapsedMs);
            if (Math.abs(wallMs - expected) > TOLERANCE_MS) {
                return expected;
            }
        }
        return Math.max(wallMs, lastTrustedMs);
    }
}
