package com.example.adminquiz;

import static com.example.adminquiz.TestData.rec;
import static com.example.adminquiz.TestData.ts;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.analytics.LeaderboardRanker;
import com.example.adminquiz.analytics.LeaderboardRanker.Mode;
import com.example.adminquiz.analytics.LeaderboardRanker.Period;
import com.example.adminquiz.analytics.LeaderboardRanker.RankedEntry;
import com.example.adminquiz.analytics.LeaderboardRanker.Result;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class LeaderboardRankerTest {
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Kolkata");
    private final long now = ts(TZ, 2026, 10, 9, 12, 0);

    private long day(int d) {
        return ts(TZ, 2026, 10, d, 10, 0);
    }

    @Test
    public void onlyBestAttemptPerQuizCounts() {
        List<AttemptRecord> l = new ArrayList<>();
        l.add(rec(1, "A", 10, "Java", 3, 10, 50000, day(9)));
        l.add(rec(1, "A", 10, "Java", 9, 10, 40000, day(9)));
        l.add(rec(1, "A", 10, "Java", 5, 10, 30000, day(9)));
        l.add(rec(2, "B", 10, "Java", 8, 10, 30000, day(9)));
        Result r = LeaderboardRanker.rank(l, Period.ALL_TIME, Mode.POINTS, 2, now, TZ);
        assertEquals("A", r.entries.get(0).username);
        assertEquals(9, r.entries.get(0).points);
        assertEquals(1, r.entries.get(0).quizzes);
        assertEquals(2, r.me.rank);
    }

    @Test
    public void tieBreakersAreAccuracyThenFewerQuizzesThenTime() {
        List<AttemptRecord> l = new ArrayList<>();
        // A and B both 10 points. A: one quiz of 10 (100%), B: one quiz of 20 (50%).
        l.add(rec(1, "A", 1, "Java", 10, 10, 90000, day(9)));
        l.add(rec(2, "B", 2, "Java", 10, 20, 10000, day(9)));
        // C: 10 points over two 10-question quizzes, 50% -> same accuracy as B but more quizzes.
        l.add(rec(3, "C", 3, "Java", 5, 10, 10000, day(9)));
        l.add(rec(3, "C", 4, "Java", 5, 10, 10000, day(9)));
        Result r = LeaderboardRanker.rank(l, Period.ALL_TIME, Mode.POINTS, 1, now, TZ);
        assertEquals("A", r.entries.get(0).username);
        assertEquals("B", r.entries.get(1).username);
        assertEquals("C", r.entries.get(2).username);
        assertEquals(1, r.entries.get(0).rank);
        assertEquals(2, r.entries.get(1).rank);
        assertEquals(3, r.entries.get(2).rank);
    }

    @Test
    public void studentsEqualOnEveryKeyShareARank() {
        List<AttemptRecord> l = new ArrayList<>();
        l.add(rec(1, "A", 1, "Java", 8, 10, 40000, day(9)));
        l.add(rec(2, "B", 1, "Java", 8, 10, 40000, day(9)));
        l.add(rec(3, "C", 1, "Java", 5, 10, 40000, day(9)));
        Result r = LeaderboardRanker.rank(l, Period.ALL_TIME, Mode.POINTS, 3, now, TZ);
        assertEquals(1, r.entries.get(0).rank);
        assertEquals(1, r.entries.get(1).rank);
        assertEquals(3, r.entries.get(2).rank);
    }

    @Test
    public void periodFiltersApplyLocalDays() {
        List<AttemptRecord> l = new ArrayList<>();
        l.add(rec(1, "A", 1, "Java", 9, 10, 1, day(9)));              // today
        l.add(rec(2, "B", 1, "Java", 9, 10, 1, day(3)));              // 6 days ago -> in weekly
        l.add(rec(3, "C", 1, "Java", 9, 10, 1, day(2)));              // 7 days ago -> out of weekly
        l.add(rec(4, "D", 1, "Java", 9, 10, 1, ts(TZ, 2026, 9, 15, 10, 0))); // in monthly? 24 days ago
        l.add(rec(5, "E", 1, "Java", 9, 10, 1, ts(TZ, 2026, 8, 1, 10, 0)));  // all-time only
        assertEquals(1, LeaderboardRanker.rank(l, Period.DAILY, Mode.POINTS, 1, now, TZ).participants);
        assertEquals(2, LeaderboardRanker.rank(l, Period.WEEKLY, Mode.POINTS, 1, now, TZ).participants);
        assertEquals(4, LeaderboardRanker.rank(l, Period.MONTHLY, Mode.POINTS, 1, now, TZ).participants);
        assertEquals(5, LeaderboardRanker.rank(l, Period.ALL_TIME, Mode.POINTS, 1, now, TZ).participants);
    }

    @Test
    public void accuracyModeNeedsMinimumQuizzes() {
        List<AttemptRecord> l = new ArrayList<>();
        l.add(rec(1, "Lucky", 1, "Java", 10, 10, 1, day(9)));          // 100% but only 1 quiz
        for (int q = 1; q <= 3; q++) {
            l.add(rec(2, "Steady", q, "Java", 8, 10, 1, day(9)));       // 80% over 3 quizzes
        }
        Result r = LeaderboardRanker.rank(l, Period.ALL_TIME, Mode.ACCURACY, 1, now, TZ);
        assertEquals("Steady", r.entries.get(0).username);
        assertEquals(1, r.entries.get(0).rank);
        assertEquals(1, r.rankedCount);
        assertEquals(0, r.me.rank);
        assertTrue(!r.me.ranked);
        // Daily needs only one quiz, so Lucky is ranked there.
        Result d = LeaderboardRanker.rank(l, Period.DAILY, Mode.ACCURACY, 1, now, TZ);
        assertEquals("Lucky", d.entries.get(0).username);
    }

    @Test
    public void currentStudentIsFoundEvenOutsideTheVisibleTop() {
        List<AttemptRecord> l = new ArrayList<>();
        for (int u = 1; u <= 80; u++) {
            l.add(rec(u, "S" + u, 1, "Java", u, 100, u * 1000L, day(9))); // student u scores u/100
        }
        Result r = LeaderboardRanker.rank(l, Period.ALL_TIME, Mode.POINTS, 5, now, TZ);
        assertNotNull(r.me);
        assertEquals(76, r.me.rank);                 // 75 students scored higher
        assertEquals(80, r.participants);
        assertEquals(80, r.entries.size());
        assertTrue(r.entries.indexOf(r.me) >= 25);   // beyond a 25-row visible list
    }

    @Test
    public void emptyClassIsHandled() {
        Result r = LeaderboardRanker.rank(new ArrayList<AttemptRecord>(), Period.WEEKLY, Mode.POINTS, 1, now, TZ);
        assertEquals(0, r.participants);
        assertNull(r.me);
        Result n = LeaderboardRanker.rank(null, Period.WEEKLY, Mode.ACCURACY, 1, now, TZ);
        assertTrue(n.entries.isEmpty());
    }

    @Test
    public void futureAndDuplicateAttemptsAreIgnored() {
        List<AttemptRecord> l = new ArrayList<>();
        AttemptRecord a = rec(1, "A", 1, "Java", 5, 10, 1, day(9));
        l.add(a);
        l.add(AttemptRecord.of(a.attemptId, 1, "A", 2, "Quiz 2", "Java", 10, 10, 1, day(9)));
        l.add(rec(1, "A", 3, "Java", 10, 10, 1, ts(TZ, 2026, 12, 25, 10, 0)));
        Result r = LeaderboardRanker.rank(l, Period.ALL_TIME, Mode.POINTS, 1, now, TZ);
        RankedEntry e = r.entries.get(0);
        assertEquals(5, e.points);
        assertEquals(1, e.quizzes);
    }
}
