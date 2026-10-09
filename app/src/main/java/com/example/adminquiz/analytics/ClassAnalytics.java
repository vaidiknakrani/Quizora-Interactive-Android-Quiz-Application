package com.example.adminquiz.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Builds the whole Class Analytics report from saved attempts. Pure Java, unit-tested.
 *
 * Definitions (used consistently):
 *  - attempt / completed quiz: one saved result (abandoned quizzes are never saved), counted once each;
 *  - participant: a distinct student with at least one attempt in the selection;
 *  - accuracy and score %: correct answers / questions of each attempt, averaged per attempt.
 */
public final class ClassAnalytics {
    public static final int MAX_GROUPS = 6;
    public static final int MAX_IMPROVEMENT_POINTS = 20;
    public static final String[] DIST_LABELS = {"0-20", "21-40", "41-60", "61-80", "81-100"};

    private ClassAnalytics() {
    }

    public static final class Filter {
        public final int rangeDays;
        public final long quizId;
        public final String category;

        public Filter(int rangeDays, long quizId, String category) {
            this.rangeDays = rangeDays;
            this.quizId = quizId;
            this.category = category;
        }

        public static Filter all() {
            return new Filter(0, -1, null);
        }

        public Filter withRange(int days) {
            return new Filter(days, quizId, category);
        }

        public Filter withQuiz(long id) {
            return new Filter(rangeDays, id, category);
        }

        public Filter withCategory(String c) {
            return new Filter(rangeDays, quizId, c);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Filter)) {
                return false;
            }
            Filter f = (Filter) o;
            return rangeDays == f.rangeDays && quizId == f.quizId
                    && (category == null ? f.category == null : category.equals(f.category));
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(new Object[]{rangeDays, quizId, category});
        }
    }

    public static final class Overview {
        public int enrolled;
        public int participants;
        public int completed;
        public double avgPoints;
        public double avgAccuracyPct;
        public double highestPct = -1;
        public int highestScore;
        public int highestTotal;
        public String highestName = "";
        public String topStudent = "";
        public int topStudentPoints;
        public double participationPct;
        public long avgTimeMs = -1;
    }

    public static final class Group {
        public String label;
        public int attempts;
        public double classPct;
        public double studentPct = Double.NaN;
    }

    public static final class StudentSummary {
        public int attempts;
        public double avgPoints;
        public double avgAccuracyPct;
        public int bestScore;
        public int bestTotal;
        public double bestPct;
        public int rank;
        public int ofParticipants;
        public double classAvgPct;
        public double deltaVsClass;
        public String trend = "Not enough data yet";
        public double trendDelta;
        public String strongest;
        public double strongestPct;
        public String weakest;
        public double weakestPct;
    }

    public static final class Report {
        public Filter filter;
        public boolean hasData;
        public Overview overview = new Overview();
        public StudentSummary student;
        public int[] distribution = new int[5];
        public List<Group> bySubject = new ArrayList<>();
        public List<Group> byQuiz = new ArrayList<>();
        public List<String> timeLabels = new ArrayList<>();
        public double[] timeClass = new double[0];
        public double[] timeStudent = new double[0];
        public double[] participation = new double[0];
        public boolean weeklyBuckets;
        public List<String> improvementLabels = new ArrayList<>();
        public double[] improvement = new double[0];
        public List<Long> quizIds = new ArrayList<>();
        public List<String> quizTitles = new ArrayList<>();
        public List<String> categories = new ArrayList<>();
    }

    public static Report build(List<AttemptRecord> all, int enrolled, long meId, Filter f,
                               long now, TimeZone tz) {
        Report rep = new Report();
        rep.filter = f;
        List<AttemptRecord> valid = StreakCalculator.validRecords(all, now);

        // Filter option lists come from everything, not from the current selection.
        Map<Long, String> quizzes = new TreeMap<>();
        Set<String> cats = new java.util.TreeSet<>();
        for (AttemptRecord r : valid) {
            quizzes.put(r.quizId, r.quizTitle == null ? "Quiz " + r.quizId : r.quizTitle);
            cats.add(r.categoryLabel());
        }
        List<Map.Entry<Long, String>> sorted = new ArrayList<>(quizzes.entrySet());
        Collections.sort(sorted, (a, b) -> a.getValue().compareToIgnoreCase(b.getValue()));
        for (Map.Entry<Long, String> e : sorted) {
            rep.quizIds.add(e.getKey());
            rep.quizTitles.add(e.getValue());
        }
        rep.categories.addAll(cats);

        long since = f.rangeDays > 0 ? StreakCalculator.startOfDay(now, tz, f.rangeDays - 1) : 0;
        List<AttemptRecord> sel = new ArrayList<>();
        for (AttemptRecord r : valid) {
            if (r.finishedAt < since) {
                continue;
            }
            if (f.quizId >= 0 && r.quizId != f.quizId) {
                continue;
            }
            if (f.category != null && !f.category.equals(r.categoryLabel())) {
                continue;
            }
            sel.add(r);
        }
        Collections.sort(sel, Comparator.comparingLong((AttemptRecord r) -> r.finishedAt));
        rep.hasData = !sel.isEmpty();

        fillOverview(rep.overview, sel, enrolled, meId, now);
        rep.student = buildStudent(sel, rep.overview, meId, now);
        for (AttemptRecord r : sel) {
            rep.distribution[bucketOf(r.pct())]++;
        }
        rep.bySubject = groups(sel, meId, true);
        rep.byQuiz = groups(sel, meId, false);
        fillTime(rep, sel, meId, since, now, tz);
        fillImprovement(rep, sel, meId);
        return rep;
    }

    /** Bucket 0..4 matching DIST_LABELS: 0-20, 21-40, 41-60, 61-80, 81-100 (20% is in the first). */
    public static int bucketOf(double pct) {
        if (pct <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(4, (int) Math.ceil(pct / 20.0) - 1));
    }

    private static void fillOverview(Overview o, List<AttemptRecord> sel, int enrolled,
                                     long meId, long now) {
        o.enrolled = Math.max(0, enrolled);
        o.completed = sel.size();
        Set<Long> users = new HashSet<>();
        double pts = 0;
        double pct = 0;
        long timeSum = 0;
        int timeN = 0;
        AttemptRecord top = null;
        for (AttemptRecord r : sel) {
            users.add(r.userId);
            pts += r.score;
            pct += r.pct();
            if (r.totalTimeMs > 0) {
                timeSum += r.totalTimeMs;
                timeN++;
            }
            if (top == null || r.pct() > top.pct()
                    || (r.pct() == top.pct() && r.score > top.score)
                    || (r.pct() == top.pct() && r.score == top.score && r.totalTimeMs < top.totalTimeMs)) {
                top = r;
            }
        }
        o.participants = users.size();
        if (!sel.isEmpty()) {
            o.avgPoints = pts / sel.size();
            o.avgAccuracyPct = pct / sel.size();
        }
        if (top != null) {
            o.highestPct = top.pct();
            o.highestScore = top.score;
            o.highestTotal = top.total;
            o.highestName = top.nameLabel();
        }
        if (timeN > 0) {
            o.avgTimeMs = timeSum / timeN;
        }
        o.participationPct = o.enrolled == 0 ? 0 : Math.min(100.0, 100.0 * o.participants / o.enrolled);
        LeaderboardRanker.Result lb = LeaderboardRanker.rankPrefiltered(sel, meId, now);
        if (!lb.entries.isEmpty()) {
            o.topStudent = lb.entries.get(0).username;
            o.topStudentPoints = lb.entries.get(0).points;
        }
    }

    private static StudentSummary buildStudent(List<AttemptRecord> sel, Overview o, long meId, long now) {
        List<AttemptRecord> mine = new ArrayList<>();
        for (AttemptRecord r : sel) {
            if (r.userId == meId) {
                mine.add(r);
            }
        }
        StudentSummary s = new StudentSummary();
        s.attempts = mine.size();
        s.classAvgPct = o.avgAccuracyPct;
        s.ofParticipants = o.participants;
        if (mine.isEmpty()) {
            return s;
        }
        double pts = 0;
        double pct = 0;
        AttemptRecord best = null;
        for (AttemptRecord r : mine) {
            pts += r.score;
            pct += r.pct();
            if (best == null || r.pct() > best.pct() || (r.pct() == best.pct() && r.score > best.score)) {
                best = r;
            }
        }
        s.avgPoints = pts / mine.size();
        s.avgAccuracyPct = pct / mine.size();
        s.bestScore = best.score;
        s.bestTotal = best.total;
        s.bestPct = best.pct();
        s.deltaVsClass = s.avgAccuracyPct - o.avgAccuracyPct;

        LeaderboardRanker.Result lb = LeaderboardRanker.rankPrefiltered(sel, meId, now);
        if (lb.me != null) {
            s.rank = lb.me.rank;
        }

        int n = mine.size();
        if (n >= 2) {
            int k = Math.min(3, n / 2);
            double recent = 0;
            double prev = 0;
            for (int i = 0; i < k; i++) {
                recent += mine.get(n - 1 - i).pct();
                prev += mine.get(n - 1 - k - i).pct();
            }
            s.trendDelta = recent / k - prev / k;
            s.trend = Math.abs(s.trendDelta) < 3 ? "Steady" : (s.trendDelta > 0 ? "Improving" : "Declining");
        }

        Map<String, double[]> bySubject = new LinkedHashMap<>();
        for (AttemptRecord r : mine) {
            double[] acc = bySubject.get(r.categoryLabel());
            if (acc == null) {
                acc = new double[2];
                bySubject.put(r.categoryLabel(), acc);
            }
            acc[0] += r.pct();
            acc[1]++;
        }
        if (bySubject.size() >= 2) {
            for (Map.Entry<String, double[]> e : bySubject.entrySet()) {
                double avg = e.getValue()[0] / e.getValue()[1];
                if (s.strongest == null || avg > s.strongestPct) {
                    s.strongest = e.getKey();
                    s.strongestPct = avg;
                }
                if (s.weakest == null || avg < s.weakestPct) {
                    s.weakest = e.getKey();
                    s.weakestPct = avg;
                }
            }
            if (s.strongest != null && s.strongest.equals(s.weakest)) {
                s.strongest = null;
                s.weakest = null;
            }
        }
        return s;
    }

    private static List<Group> groups(List<AttemptRecord> sel, long meId, boolean bySubject) {
        Map<String, double[]> acc = new LinkedHashMap<>(); // classSum, classN, meSum, meN
        for (AttemptRecord r : sel) {
            String key = bySubject ? r.categoryLabel()
                    : (r.quizTitle == null ? "Quiz " + r.quizId : r.quizTitle);
            double[] a = acc.get(key);
            if (a == null) {
                a = new double[4];
                acc.put(key, a);
            }
            a[0] += r.pct();
            a[1]++;
            if (r.userId == meId) {
                a[2] += r.pct();
                a[3]++;
            }
        }
        List<Group> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : acc.entrySet()) {
            Group g = new Group();
            g.label = e.getKey();
            g.attempts = (int) e.getValue()[1];
            g.classPct = e.getValue()[0] / e.getValue()[1];
            if (e.getValue()[3] > 0) {
                g.studentPct = e.getValue()[2] / e.getValue()[3];
            }
            out.add(g);
        }
        Collections.sort(out, (a, b) -> Integer.compare(b.attempts, a.attempts));
        return out.size() > MAX_GROUPS ? new ArrayList<>(out.subList(0, MAX_GROUPS)) : out;
    }

    private static void fillTime(Report rep, List<AttemptRecord> sel, long meId, long since,
                                 long now, TimeZone tz) {
        if (sel.isEmpty()) {
            return;
        }
        long today = StreakCalculator.dayOf(now, tz);
        long first = since > 0 ? StreakCalculator.dayOf(since, tz)
                : StreakCalculator.dayOf(sel.get(0).finishedAt, tz);
        int span = (int) Math.max(1, today - first + 1);
        int bucketDays = 1;
        if (span > 31) {
            int b = (int) Math.ceil(span / 26.0);
            bucketDays = Math.max(7, ((b + 6) / 7) * 7);
        }
        int buckets = (int) Math.ceil(span / (double) bucketDays);
        rep.weeklyBuckets = bucketDays > 1;
        double[] cSum = new double[buckets];
        int[] cN = new int[buckets];
        double[] sSum = new double[buckets];
        int[] sN = new int[buckets];
        for (AttemptRecord r : sel) {
            int i = (int) ((StreakCalculator.dayOf(r.finishedAt, tz) - first) / bucketDays);
            if (i < 0 || i >= buckets) {
                continue;
            }
            cSum[i] += r.pct();
            cN[i]++;
            if (r.userId == meId) {
                sSum[i] += r.pct();
                sN[i]++;
            }
        }
        rep.timeClass = new double[buckets];
        rep.timeStudent = new double[buckets];
        rep.participation = new double[buckets];
        for (int i = 0; i < buckets; i++) {
            rep.timeLabels.add(StreakCalculator.formatShort(first + (long) i * bucketDays));
            rep.timeClass[i] = cN[i] == 0 ? Double.NaN : cSum[i] / cN[i];
            rep.timeStudent[i] = sN[i] == 0 ? Double.NaN : sSum[i] / sN[i];
            rep.participation[i] = cN[i];
        }
    }

    private static void fillImprovement(Report rep, List<AttemptRecord> sel, long meId) {
        List<AttemptRecord> mine = new ArrayList<>();
        for (AttemptRecord r : sel) {
            if (r.userId == meId) {
                mine.add(r);
            }
        }
        int start = Math.max(0, mine.size() - MAX_IMPROVEMENT_POINTS);
        rep.improvement = new double[mine.size() - start];
        for (int i = start; i < mine.size(); i++) {
            rep.improvementLabels.add("#" + (i + 1));
            rep.improvement[i - start] = mine.get(i).pct();
        }
    }
}
