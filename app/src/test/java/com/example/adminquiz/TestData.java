package com.example.adminquiz;

import com.example.adminquiz.analytics.AttemptRecord;

import java.util.Calendar;
import java.util.TimeZone;

final class TestData {
    private TestData() {
    }

    static long ts(TimeZone tz, int y, int m, int d, int h, int min) {
        Calendar c = Calendar.getInstance(tz);
        c.clear();
        c.set(y, m - 1, d, h, min, 0);
        return c.getTimeInMillis();
    }

    static long id = 1;

    static AttemptRecord rec(long user, String name, long quiz, String cat, int score, int total,
                             long timeMs, long finishedAt) {
        return AttemptRecord.of(id++, user, name, quiz, "Quiz " + quiz, cat, score, total, timeMs, finishedAt);
    }
}
