package com.example.adminquiz.analytics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Daily activity and streak maths. Pure Java (no Android classes) so it is unit-tested.
 *
 * Rules:
 * - a "day" is the local calendar date of the attempt's finish time;
 * - several quizzes on one day make ONE active day;
 * - current streak = consecutive active days ending today, or yesterday if today is still empty;
 * - max streak = longest run in the whole history and is never reduced;
 * - attempts dated in the future (clock set forward), without a question, or duplicated by id
 *   are ignored, so they cannot inflate anything.
 */
public final class StreakCalculator {
    public static final long DAY_MS = 24L * 60 * 60 * 1000;
    public static final long FUTURE_TOLERANCE_MS = 5L * 60 * 1000;
    public static final int[] MILESTONES = {3, 7, 14, 30, 100, 365};

    private StreakCalculator() {
    }

    public static final class DayActivity {
        public final long epochDay;
        public int quizzes;
        public int points;
        public int correct;
        public int questions;

        DayActivity(long epochDay) {
            this.epochDay = epochDay;
        }

        public int accuracyPct() {
            return questions == 0 ? 0 : (int) Math.round(100.0 * correct / questions);
        }
    }

    public static final class StreakStats {
        public int currentStreak;
        public int maxStreak;
        public int activeDays;
        public int totalQuizzes;
        public long today;
        public final TreeMap<Long, DayActivity> days = new TreeMap<>();

        public boolean activeToday() {
            return days.containsKey(today);
        }
    }

    /** Local calendar day number (days since 1970-01-01) for an instant, honouring DST. */
    public static long dayOf(long timeMs, TimeZone tz) {
        return Math.floorDiv(timeMs + tz.getOffset(timeMs), DAY_MS);
    }

    /** Local midnight, {@code daysBack} days before the day containing {@code now}. */
    public static long startOfDay(long now, TimeZone tz, int daysBack) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(now);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.DAY_OF_YEAR, -daysBack);
        return c.getTimeInMillis();
    }

    public static boolean isValid(AttemptRecord r, long now) {
        return r != null && r.total > 0 && r.finishedAt > 0 && r.finishedAt <= now + FUTURE_TOLERANCE_MS;
    }

    /** Valid attempts only, with duplicate attempt ids removed. */
    public static List<AttemptRecord> validRecords(List<AttemptRecord> input, long now) {
        List<AttemptRecord> out = new ArrayList<>();
        if (input == null) {
            return out;
        }
        Set<Long> seen = new HashSet<>();
        for (AttemptRecord r : input) {
            if (!isValid(r, now)) {
                continue;
            }
            if (r.attemptId > 0 && !seen.add(r.attemptId)) {
                continue;
            }
            out.add(r);
        }
        return out;
    }

    public static StreakStats compute(List<AttemptRecord> records, long now, TimeZone tz) {
        StreakStats s = new StreakStats();
        s.today = dayOf(now, tz);
        for (AttemptRecord r : validRecords(records, now)) {
            long d = dayOf(r.finishedAt, tz);
            DayActivity a = s.days.get(d);
            if (a == null) {
                a = new DayActivity(d);
                s.days.put(d, a);
            }
            a.quizzes++;
            a.points += r.score;
            a.correct += r.score;
            a.questions += r.total;
            s.totalQuizzes++;
        }
        s.activeDays = s.days.size();

        long d = s.days.containsKey(s.today) ? s.today : s.today - 1;
        while (s.days.containsKey(d)) {
            s.currentStreak++;
            d--;
        }

        int run = 0;
        long prev = Long.MIN_VALUE;
        for (long day : s.days.keySet()) {
            run = (prev != Long.MIN_VALUE && day == prev + 1) ? run + 1 : 1;
            s.maxStreak = Math.max(s.maxStreak, run);
            prev = day;
        }
        return s;
    }

    /** Heat level 0..4 from the number of quizzes completed that day. */
    public static int level(int quizzes) {
        if (quizzes <= 0) {
            return 0;
        }
        if (quizzes == 1) {
            return 1;
        }
        if (quizzes == 2) {
            return 2;
        }
        return quizzes <= 4 ? 3 : 4;
    }

    /** Milestones (3, 7, 14, 30, 100, 365) reached by the longest streak ever achieved. */
    public static List<Integer> earnedMilestones(int maxStreak) {
        List<Integer> out = new ArrayList<>();
        for (int m : MILESTONES) {
            if (maxStreak >= m) {
                out.add(m);
            }
        }
        return out;
    }

    /** Next milestone above the current streak, or 0 when every milestone is already passed. */
    public static int nextMilestone(int currentStreak) {
        for (int m : MILESTONES) {
            if (currentStreak < m) {
                return m;
            }
        }
        return 0;
    }

    /** Day of week for an epoch day: Sunday = 0 ... Saturday = 6 (1970-01-01 was a Thursday). */
    public static int dayOfWeek(long epochDay) {
        return (int) Math.floorMod(epochDay + 4, 7L);
    }

    public static String formatDay(long epochDay) {
        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(epochDay * DAY_MS));
    }

    public static String formatShort(long epochDay) {
        SimpleDateFormat f = new SimpleDateFormat("d MMM", Locale.getDefault());
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(epochDay * DAY_MS));
    }
}
