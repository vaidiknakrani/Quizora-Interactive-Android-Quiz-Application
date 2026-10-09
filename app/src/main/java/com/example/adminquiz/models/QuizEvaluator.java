package com.example.adminquiz.models;

import com.example.adminquiz.entities.AnswerEntity;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.utils.Fmt;

import java.util.ArrayList;
import java.util.List;

/** Pure Java scoring: no Android or network dependency, fully unit-testable. */
public final class QuizEvaluator {
    private QuizEvaluator() {
    }

    public static QuizResult evaluate(QuizSession s) {
        s.finish();
        int n = s.size();
        QuizResult r = new QuizResult();
        r.total = n;
        r.answers = new ArrayList<>();

        int streak = 0;
        int best = 0;
        int half = n / 2;
        int firstCorrect = 0;
        int secondCorrect = 0;
        double diffSum = 0;
        long sumTime = 0;

        for (int i = 0; i < n; i++) {
            QuestionEntity q = s.getQuestion(i);
            int sel = s.getSelected(i);
            boolean ok = sel == q.correctIndex;
            if (ok) {
                r.score++;
                streak++;
                best = Math.max(best, streak);
                if (i < half) {
                    firstCorrect++;
                } else {
                    secondCorrect++;
                }
            } else {
                streak = 0;
            }
            AnswerEntity a = new AnswerEntity();
            a.position = i;
            a.question = q.text;
            a.optA = q.optA;
            a.optB = q.optB;
            a.optC = q.optC;
            a.optD = q.optD;
            a.correctIndex = q.correctIndex;
            a.selectedIndex = sel;
            a.explanation = q.explanation;
            a.timeMs = s.getTimeMs(i);
            r.answers.add(a);
            sumTime += a.timeMs;
            diffSum += difficultyWeight(q.difficulty);
        }

        r.longestStreak = best;
        r.totalTimeMs = s.getElapsedMs();
        r.accuracy = n == 0 ? 0 : (double) r.score / n;

        long expected = s.getTimeLimitMs() > 0 && n > 0 ? s.getTimeLimitMs() / n : 30000L;
        double avg = n == 0 ? 0 : (double) sumTime / n;
        r.speed = clamp(1.0 - avg / expected);

        double mean = n == 0 ? 0 : (double) sumTime / n;
        double var = 0;
        for (int i = 0; i < n; i++) {
            double d = s.getTimeMs(i) - mean;
            var += d * d;
        }
        double std = n == 0 ? 0 : Math.sqrt(var / n);
        double cv = mean <= 0 ? 0 : std / mean;
        r.consistency = n < 2 ? 1.0 : clamp(1.0 - cv);

        r.overallPct = Fmt.pct(0.6 * r.accuracy + 0.25 * r.speed + 0.15 * r.consistency);

        int secondCount = n - half;
        double firstAcc = half == 0 ? -1 : (double) firstCorrect / half;
        double secondAcc = secondCount == 0 ? 0 : (double) secondCorrect / secondCount;
        double avgDiff = n == 0 ? 1 : diffSum / n;
        r.persona = PersonaCalculator.pick(r.accuracy, r.speed, r.consistency, firstAcc, secondAcc, avgDiff, n);
        return r;
    }

    static double difficultyWeight(String d) {
        if ("Easy".equals(d)) {
            return 1;
        }
        if ("Hard".equals(d)) {
            return 3;
        }
        if ("Expert".equals(d)) {
            return 4;
        }
        return 2;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
