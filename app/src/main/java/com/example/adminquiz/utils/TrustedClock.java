package com.example.adminquiz.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import com.example.adminquiz.analytics.ClockGuard;

/**
 * Timestamp source for saving quiz results. Wraps {@link ClockGuard} with the last trusted time
 * and the elapsed-realtime reading stored in private preferences.
 */
public final class TrustedClock {
    private static final String PREFS = "clock_guard";
    private static final String KEY_TRUSTED = "last_trusted_ms";
    private static final String KEY_ELAPSED = "last_elapsed_ms";

    private TrustedClock() {
    }

    public static synchronized long now(Context context) {
        SharedPreferences p = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long wall = System.currentTimeMillis();
        long elapsed = SystemClock.elapsedRealtime();
        long trusted = ClockGuard.resolve(wall, elapsed, p.contains(KEY_TRUSTED),
                p.getLong(KEY_TRUSTED, 0L), p.getLong(KEY_ELAPSED, 0L));
        p.edit().putLong(KEY_TRUSTED, trusted).putLong(KEY_ELAPSED, elapsed).apply();
        return trusted;
    }
}
