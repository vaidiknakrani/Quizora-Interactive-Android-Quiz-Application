package com.example.adminquiz.analytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Transparent class ranking. Pure Java, unit-tested.
 *
 * Scoring: for each student and each quiz only the BEST attempt inside the period counts, so
 * replaying one quiz many times cannot farm points.
 *   Points   = sum of best scores            Quizzes  = number of different quizzes
 *   Accuracy = correct answers / questions asked over those best attempts
 *
 * Points mode  : Points desc, Accuracy desc, fewer Quizzes, less Time.
 * Accuracy mode: Accuracy desc, more Quizzes, Points desc, less Time. Only students with at
 *                least {@link #minQuizzes} quizzes are ranked, so one lucky quiz cannot top the list.
 * Students equal on every key share a rank (1, 2, 2, 4 ...).
 */
public final class LeaderboardRanker {
    public static final int MIN_QUIZZES_ACCURACY = 3;

    public enum Period { DAILY, WEEKLY, MONTHLY, ALL_TIME }

    public enum Mode { POINTS, ACCURACY }

    private LeaderboardRanker() {
    }

    public static final class RankedEntry {
        public long userId;
        public String username;
        public int rank;
        public boolean ranked;
        public boolean me;
        public int points;
        public int quizzes;
        public int correct;
        public int questions;
        public long totalTimeMs;

        public double accuracy() {
            return questions == 0 ? 0 : (double) correct / questions;
        }

        public int accuracyPct() {
            return (int) Math.round(accuracy() * 100);
        }
    }

    public static final class Result {
        public final List<RankedEntry> entries = new ArrayList<>();
        public RankedEntry me;
        public int participants;
        public int rankedCount;
        public int minQuizzes;
        public Mode mode;
        public Period period;
        public long computedAt;
    }

    public static int minQuizzes(Period p) {
        return p == Period.DAILY ? 1 : MIN_QUIZZES_ACCURACY;
    }

    /** Start of the period in local time; 0 means all-time. Weekly = 7 calendar days incl. today. */
    public static long sinceFor(Period p, long now, TimeZone tz) {
        switch (p) {
            case DAILY: return StreakCalculator.startOfDay(now, tz, 0);
            case WEEKLY: return StreakCalculator.startOfDay(now, tz, 6);
            case MONTHLY: return StreakCalculator.startOfDay(now, tz, 29);
            default: return 0;
        }
    }

    public static Result rank(List<AttemptRecord> records, Period period, Mode mode,
                              long meId, long now, TimeZone tz) {
        long since = sinceFor(period, now, tz);
        List<AttemptRecord> filtered = new ArrayList<>();
        for (AttemptRecord r : StreakCalculator.validRecords(records, now)) {
            if (r.finishedAt >= since) {
                filtered.add(r);
            }
        }
        return build(filtered, period, mode, minQuizzes(period), meId, now);
    }

    /** Points ranking over attempts the caller already validated and filtered. */
    public static Result rankPrefiltered(List<AttemptRecord> filtered, long meId, long now) {
        return build(filtered, Period.ALL_TIME, Mode.POINTS, 1, meId, now);
    }

    private static boolean better(AttemptRecord a, AttemptRecord b) {
        if (a.pct() != b.pct()) {
            return a.pct() > b.pct();
        }
        if (a.score != b.score) {
            return a.score > b.score;
        }
        return a.totalTimeMs < b.totalTimeMs;
    }

    private static Result build(List<AttemptRecord> filtered, Period period, Mode mode,
                                int minQuizzes, long meId, long now) {
        Map<Long, Map<Long, AttemptRecord>> best = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (AttemptRecord r : filtered) {
            names.put(r.userId, r.nameLabel());
            Map<Long, AttemptRecord> perQuiz = best.get(r.userId);
            if (perQuiz == null) {
                perQuiz = new HashMap<>();
                best.put(r.userId, perQuiz);
            }
            AttemptRecord cur = perQuiz.get(r.quizId);
            if (cur == null || better(r, cur)) {
                perQuiz.put(r.quizId, r);
            }
        }

        List<RankedEntry> all = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, AttemptRecord>> e : best.entrySet()) {
            RankedEntry re = new RankedEntry();
            re.userId = e.getKey();
            re.username = names.get(re.userId);
            re.me = re.userId == meId;
            for (AttemptRecord r : e.getValue().values()) {
                re.points += r.score;
                re.correct += r.score;
                re.questions += r.total;
                re.totalTimeMs += r.totalTimeMs;
                re.quizzes++;
            }
            re.ranked = mode == Mode.POINTS || re.quizzes >= minQuizzes;
            all.add(re);
        }

        final Comparator<RankedEntry> keys = mode == Mode.POINTS ? POINTS_KEYS : ACCURACY_KEYS;
        List<RankedEntry> ranked = new ArrayList<>();
        List<RankedEntry> unranked = new ArrayList<>();
        for (RankedEntry re : all) {
            (re.ranked ? ranked : unranked).add(re);
        }
        Collections.sort(ranked, (x, y) -> {
            int c = keys.compare(x, y);
            return c != 0 ? c : Long.compare(x.userId, y.userId);
        });
        for (int i = 0; i < ranked.size(); i++) {
            RankedEntry cur = ranked.get(i);
            cur.rank = (i > 0 && keys.compare(ranked.get(i - 1), cur) == 0) ? ranked.get(i - 1).rank : i + 1;
        }
        Collections.sort(unranked, (x, y) -> {
            int c = Integer.compare(y.points, x.points);
            return c != 0 ? c : Long.compare(x.userId, y.userId);
        });

        Result res = new Result();
        res.entries.addAll(ranked);
        res.entries.addAll(unranked);
        res.participants = all.size();
        res.rankedCount = ranked.size();
        res.minQuizzes = minQuizzes;
        res.mode = mode;
        res.period = period;
        res.computedAt = now;
        for (RankedEntry re : res.entries) {
            if (re.me) {
                res.me = re;
            }
        }
        return res;
    }

    private static final Comparator<RankedEntry> POINTS_KEYS = (a, b) -> {
        int c = Integer.compare(b.points, a.points);
        if (c != 0) {
            return c;
        }
        c = Double.compare(b.accuracy(), a.accuracy());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(a.quizzes, b.quizzes);
        return c != 0 ? c : Long.compare(a.totalTimeMs, b.totalTimeMs);
    };

    private static final Comparator<RankedEntry> ACCURACY_KEYS = (a, b) -> {
        int c = Double.compare(b.accuracy(), a.accuracy());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(b.quizzes, a.quizzes);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(b.points, a.points);
        return c != 0 ? c : Long.compare(a.totalTimeMs, b.totalTimeMs);
    };

    public static String explain(Mode mode, Period period) {
        String base = "Only your best attempt of each quiz counts in the period, so replaying one quiz "
                + "cannot add points.\n\nPoints = sum of those best scores.\nAccuracy = correct answers "
                + "÷ questions over the same attempts.\n\n";
        if (mode == Mode.POINTS) {
            return base + "Ranked by: points, then accuracy, then fewer quizzes, then less total time. "
                    + "Students equal on all four share a rank.";
        }
        return base + "Ranked by: accuracy, then more quizzes, then points, then less total time. "
                + "You need at least " + minQuizzes(period) + " quiz(zes) in this period to be ranked, so "
                + "one lucky quiz cannot take first place.";
    }
}
