package com.example.adminquiz;

import static com.example.adminquiz.TestData.rec;
import static com.example.adminquiz.TestData.ts;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.analytics.ClassAnalytics;
import com.example.adminquiz.analytics.ClassAnalytics.Filter;
import com.example.adminquiz.analytics.ClassAnalytics.Report;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ClassAnalyticsTest {
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Kolkata");
    private final long now = ts(TZ, 2026, 10, 9, 12, 0);

    private long day(int d) {
        return ts(TZ, 2026, 10, d, 10, 0);
    }

    private List<AttemptRecord> sample() {
        List<AttemptRecord> l = new ArrayList<>();
        l.add(rec(1, "Asha", 1, "Java", 8, 10, 60000, day(9)));
        l.add(rec(1, "Asha", 2, "DSA", 4, 10, 120000, day(8)));
        l.add(rec(2, "Ben", 1, "Java", 6, 10, 90000, day(9)));
        l.add(rec(2, "Ben", 1, "Java", 10, 10, 30000, day(5)));
        return l;
    }

    @Test
    public void overviewSeparatesEnrolledParticipantsAndCompleted() {
        Report r = ClassAnalytics.build(sample(), 10, 1, Filter.all(), now, TZ);
        assertEquals(10, r.overview.enrolled);
        assertEquals(2, r.overview.participants);
        assertEquals(4, r.overview.completed);
        assertEquals(20.0, r.overview.participationPct, 0.001);
        assertEquals(7.0, r.overview.avgPoints, 0.001);
        assertEquals(70.0, r.overview.avgAccuracyPct, 0.001);
        assertEquals(100.0, r.overview.highestPct, 0.001);
        assertEquals("Ben", r.overview.highestName);
        assertEquals(75000, r.overview.avgTimeMs); // (60+120+90+30) s / 4 attempts
        assertEquals("Asha", r.overview.topStudent); // best attempts: Asha 8+4 = 12 points, Ben 10
        assertEquals(12, r.overview.topStudentPoints);
    }

    @Test
    public void bucketBoundariesMatchTheLabels() {
        int[] expected = {0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4};
        double[] pcts = {0, 10, 20, 21, 40, 41, 60, 61, 80, 81, 100};
        for (int i = 0; i < pcts.length; i++) {
            assertEquals("pct " + pcts[i], expected[i], ClassAnalytics.bucketOf(pcts[i]));
        }
    }

    @Test
    public void emptyClassAndNoEnrolmentDoNotCrash() {
        Report r = ClassAnalytics.build(new ArrayList<AttemptRecord>(), 0, 1, Filter.all(), now, TZ);
        assertFalse(r.hasData);
        assertEquals(0, r.overview.participationPct, 0.0);
        assertEquals(-1, r.overview.avgTimeMs);
        assertEquals(0, r.student.attempts);
        assertEquals(0, r.timeLabels.size());
        Report n = ClassAnalytics.build(null, 3, 1, Filter.all(), now, TZ);
        assertFalse(n.hasData);
    }

    @Test
    public void filtersNarrowTheSelectionButNotTheOptionLists() {
        Report bySubject = ClassAnalytics.build(sample(), 5, 1, Filter.all().withCategory("DSA"), now, TZ);
        assertEquals(1, bySubject.overview.completed);
        assertEquals(2, bySubject.categories.size());
        Report range = ClassAnalytics.build(sample(), 5, 1, Filter.all().withRange(2), now, TZ);
        assertEquals(3, range.overview.completed); // days 8 and 9 only
        Report quiz = ClassAnalytics.build(sample(), 5, 1, Filter.all().withQuiz(2), now, TZ);
        assertEquals(1, quiz.overview.completed);
        assertEquals(2, quiz.quizIds.size());
    }

    @Test
    public void scoreDistributionPutsPerfectScoreInTopBucket() {
        Report r = ClassAnalytics.build(sample(), 5, 1, Filter.all(), now, TZ);
        assertEquals(1, r.distribution[4]); // 100%
        assertEquals(1, r.distribution[3]); // 80% belongs to 61-80
        assertEquals(1, r.distribution[2]); // 60%
        assertEquals(1, r.distribution[1]); // 40%
        int sum = 0;
        for (int c : r.distribution) {
            sum += c;
        }
        assertEquals(4, sum);
    }

    @Test
    public void studentComparisonRankAndSubjects() {
        Report r = ClassAnalytics.build(sample(), 5, 1, Filter.all(), now, TZ);
        assertEquals(2, r.student.attempts);
        assertEquals(60.0, r.student.avgAccuracyPct, 0.001);
        assertEquals(-10.0, r.student.deltaVsClass, 0.001);
        assertEquals(1, r.student.rank); // Asha has 12 points, Ben 10
        assertEquals("Java", r.student.strongest);
        assertEquals("DSA", r.student.weakest);
        assertEquals(80.0, r.student.bestPct, 0.001);
    }

    @Test
    public void strongestAndWeakestNeedAtLeastTwoSubjects() {
        List<AttemptRecord> l = new ArrayList<>();
        l.add(rec(1, "Asha", 1, "Java", 8, 10, 1, day(9)));
        l.add(rec(1, "Asha", 2, "Java", 6, 10, 1, day(8)));
        Report r = ClassAnalytics.build(l, 1, 1, Filter.all(), now, TZ);
        assertNull(r.student.strongest);
        assertNull(r.student.weakest);
    }

    @Test
    public void trendDetectsImprovementAndSteadiness() {
        List<AttemptRecord> up = new ArrayList<>();
        int[] scores = {3, 3, 3, 8, 8, 8};
        for (int i = 0; i < scores.length; i++) {
            up.add(rec(1, "A", i + 1, "Java", scores[i], 10, 1, day(3 + i)));
        }
        Report r = ClassAnalytics.build(up, 1, 1, Filter.all(), now, TZ);
        assertEquals("Improving", r.student.trend);
        assertEquals(50.0, r.student.trendDelta, 0.001);

        List<AttemptRecord> flat = new ArrayList<>();
        flat.add(rec(1, "A", 1, "Java", 7, 10, 1, day(8)));
        flat.add(rec(1, "A", 2, "Java", 7, 10, 1, day(9)));
        assertEquals("Steady", ClassAnalytics.build(flat, 1, 1, Filter.all(), now, TZ).student.trend);

        List<AttemptRecord> one = new ArrayList<>();
        one.add(rec(1, "A", 1, "Java", 7, 10, 1, day(9)));
        assertEquals("Not enough data yet", ClassAnalytics.build(one, 1, 1, Filter.all(), now, TZ).student.trend);
    }

    @Test
    public void timelineBucketsAndParticipationCountAttempts() {
        Report r = ClassAnalytics.build(sample(), 5, 1, Filter.all().withRange(7), now, TZ);
        assertEquals(7, r.timeLabels.size());
        assertFalse(r.weeklyBuckets);
        double total = 0;
        for (double v : r.participation) {
            total += v;
        }
        assertEquals(4.0, total, 0.0);
        assertEquals(70.0, r.timeClass[6], 0.001);
        assertTrue(Double.isNaN(r.timeStudent[0]));
    }

    @Test
    public void longHistoryFallsBackToWeeklyBuckets() {
        List<AttemptRecord> l = new ArrayList<>();
        l.add(rec(1, "A", 1, "Java", 5, 10, 1, ts(TZ, 2026, 6, 1, 10, 0)));
        l.add(rec(1, "A", 2, "Java", 5, 10, 1, day(9)));
        Report r = ClassAnalytics.build(l, 1, 1, Filter.all(), now, TZ);
        assertTrue(r.weeklyBuckets);
        assertTrue(r.timeLabels.size() <= 26);
        double total = 0;
        for (double v : r.participation) {
            total += v;
        }
        assertEquals(2.0, total, 0.0);
    }

    @Test
    public void improvementKeepsOnlyLastTwentyAttempts() {
        List<AttemptRecord> l = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            l.add(rec(1, "A", i + 1, "Java", i % 11, 10, 1, now - (25 - i) * 60000L));
        }
        Report r = ClassAnalytics.build(l, 1, 1, Filter.all(), now, TZ);
        assertEquals(20, r.improvement.length);
        assertEquals("#6", r.improvementLabels.get(0));
        assertEquals("#25", r.improvementLabels.get(19));
    }
}
