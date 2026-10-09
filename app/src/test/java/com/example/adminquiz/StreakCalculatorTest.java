package com.example.adminquiz;

import static com.example.adminquiz.TestData.rec;
import static com.example.adminquiz.TestData.ts;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.analytics.StreakCalculator;
import com.example.adminquiz.analytics.StreakCalculator.StreakStats;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class StreakCalculatorTest {
    private static final TimeZone IST = TimeZone.getTimeZone("Asia/Kolkata");
    private static final TimeZone NY = TimeZone.getTimeZone("America/New_York");

    private AttemptRecord at(TimeZone tz, int y, int m, int d, int h) {
        return rec(1, "a", 1, "Java", 5, 10, 30000, ts(tz, y, m, d, h, 0));
    }

    @Test
    public void emptyHistoryDoesNotCrash() {
        StreakStats s = StreakCalculator.compute(new ArrayList<AttemptRecord>(), ts(IST, 2026, 10, 9, 8, 0), IST);
        assertEquals(0, s.currentStreak);
        assertEquals(0, s.maxStreak);
        assertEquals(0, s.activeDays);
        StreakStats n = StreakCalculator.compute(null, ts(IST, 2026, 10, 9, 8, 0), IST);
        assertEquals(0, n.totalQuizzes);
    }

    @Test
    public void manyQuizzesOnOneDayAreOneActiveDay() {
        long now = ts(IST, 2026, 10, 9, 22, 0);
        List<AttemptRecord> l = Arrays.asList(at(IST, 2026, 10, 9, 8), at(IST, 2026, 10, 9, 12), at(IST, 2026, 10, 9, 20));
        StreakStats s = StreakCalculator.compute(l, now, IST);
        assertEquals(1, s.currentStreak);
        assertEquals(1, s.maxStreak);
        assertEquals(1, s.activeDays);
        assertEquals(3, s.totalQuizzes);
    }

    @Test
    public void missingADayBreaksCurrentButKeepsMax() {
        long now = ts(IST, 2026, 10, 9, 9, 0);
        List<AttemptRecord> l = Arrays.asList(
                at(IST, 2026, 10, 1, 10), at(IST, 2026, 10, 2, 10), at(IST, 2026, 10, 3, 10), at(IST, 2026, 10, 4, 10),
                at(IST, 2026, 10, 8, 10), at(IST, 2026, 10, 9, 8));
        StreakStats s = StreakCalculator.compute(l, now, IST);
        assertEquals(2, s.currentStreak);
        assertEquals(4, s.maxStreak);
    }

    @Test
    public void streakEndingYesterdayStillCountsButNotOlder() {
        long now = ts(IST, 2026, 10, 9, 9, 0);
        List<AttemptRecord> yesterday = Arrays.asList(at(IST, 2026, 10, 7, 10), at(IST, 2026, 10, 8, 10));
        assertEquals(2, StreakCalculator.compute(yesterday, now, IST).currentStreak);
        List<AttemptRecord> stale = Arrays.asList(at(IST, 2026, 10, 6, 10), at(IST, 2026, 10, 7, 10));
        StreakStats s = StreakCalculator.compute(stale, now, IST);
        assertEquals(0, s.currentStreak);
        assertEquals(2, s.maxStreak);
    }

    @Test
    public void leapDayMonthAndYearBoundariesAreConsecutive() {
        long now = ts(IST, 2024, 3, 1, 23, 0);
        List<AttemptRecord> leap = Arrays.asList(at(IST, 2024, 2, 28, 10), at(IST, 2024, 2, 29, 10), at(IST, 2024, 3, 1, 10));
        assertEquals(3, StreakCalculator.compute(leap, now, IST).currentStreak);

        long nowYear = ts(IST, 2024, 1, 1, 23, 0);
        List<AttemptRecord> year = Arrays.asList(at(IST, 2023, 12, 30, 10), at(IST, 2023, 12, 31, 10), at(IST, 2024, 1, 1, 10));
        assertEquals(3, StreakCalculator.compute(year, nowYear, IST).maxStreak);

        long nonLeap = ts(IST, 2023, 3, 1, 23, 0);
        List<AttemptRecord> noLeapDay = Arrays.asList(at(IST, 2023, 2, 28, 10), at(IST, 2023, 3, 1, 10));
        assertEquals(2, StreakCalculator.compute(noLeapDay, nonLeap, IST).currentStreak);
    }

    @Test
    public void midnightSplitsDaysByLocalTime() {
        long now = ts(IST, 2026, 10, 9, 12, 0);
        AttemptRecord late = rec(1, "a", 1, "Java", 5, 10, 1, ts(IST, 2026, 10, 8, 23, 59));
        AttemptRecord early = rec(1, "a", 1, "Java", 5, 10, 1, ts(IST, 2026, 10, 9, 0, 1));
        StreakStats s = StreakCalculator.compute(Arrays.asList(late, early), now, IST);
        assertEquals(2, s.activeDays);
        assertEquals(2, s.currentStreak);
    }

    @Test
    public void sameInstantIsDifferentDayInDifferentTimezones() {
        long t = ts(IST, 2026, 10, 9, 1, 0); // 01:00 in India = previous evening in New York
        long now = ts(NY, 2026, 10, 9, 12, 0);
        AttemptRecord r = rec(1, "a", 1, "Java", 5, 10, 1, t);
        long ist = StreakCalculator.dayOf(t, IST);
        long ny = StreakCalculator.dayOf(t, NY);
        assertEquals(ist - 1, ny);
        assertEquals(1, StreakCalculator.compute(Arrays.asList(r), now, NY).currentStreak);
    }

    @Test
    public void daylightSavingTransitionDoesNotBreakStreak() {
        long now = ts(NY, 2024, 3, 11, 9, 0);
        List<AttemptRecord> l = Arrays.asList(at(NY, 2024, 3, 9, 23), at(NY, 2024, 3, 10, 12), at(NY, 2024, 3, 11, 8));
        assertEquals(3, StreakCalculator.compute(l, now, NY).currentStreak);
    }

    @Test
    public void futureDatedAndDuplicateAttemptsCannotInflate() {
        long now = ts(IST, 2026, 10, 9, 9, 0);
        AttemptRecord real = at(IST, 2026, 10, 9, 8);
        AttemptRecord future1 = at(IST, 2026, 10, 10, 8);
        AttemptRecord future2 = at(IST, 2026, 10, 11, 8);
        AttemptRecord dup = AttemptRecord.of(real.attemptId, 1, "a", 1, "Quiz 1", "Java", 5, 10, 1, real.finishedAt);
        StreakStats s = StreakCalculator.compute(Arrays.asList(real, future1, future2, dup), now, IST);
        assertEquals(1, s.currentStreak);
        assertEquals(1, s.maxStreak);
        assertEquals(1, s.totalQuizzes);
    }

    @Test
    public void milestonesAndLevels() {
        assertEquals(Arrays.asList(3, 7), StreakCalculator.earnedMilestones(10));
        assertTrue(StreakCalculator.earnedMilestones(2).isEmpty());
        assertEquals(6, StreakCalculator.earnedMilestones(400).size());
        assertEquals(7, StreakCalculator.nextMilestone(3));
        assertEquals(0, StreakCalculator.nextMilestone(365));
        assertEquals(0, StreakCalculator.level(0));
        assertEquals(1, StreakCalculator.level(1));
        assertEquals(3, StreakCalculator.level(4));
        assertEquals(4, StreakCalculator.level(9));
    }

    @Test
    public void dayOfWeekMatchesCalendar() {
        long sunday = StreakCalculator.dayOf(ts(TimeZone.getTimeZone("UTC"), 2024, 1, 7, 12, 0), TimeZone.getTimeZone("UTC"));
        assertEquals(0, StreakCalculator.dayOfWeek(sunday));
        assertEquals(1, StreakCalculator.dayOfWeek(sunday + 1));
    }
}
