package com.example.adminquiz.analytics;

/**
 * One saved quiz attempt joined with its student name and quiz category.
 * Plain public fields so Room can map it directly and pure-Java code can use it.
 * Every saved attempt is a completed quiz: quitting a quiz mid-way saves nothing.
 */
public class AttemptRecord {
    public long attemptId;
    public long userId;
    public String username;
    public long quizId;
    public String quizTitle;
    public String category;
    public int score;
    public int total;
    public double accuracy;
    public long totalTimeMs;
    public long finishedAt;

    public static AttemptRecord of(long attemptId, long userId, String username, long quizId,
                                   String quizTitle, String category, int score, int total,
                                   long totalTimeMs, long finishedAt) {
        AttemptRecord r = new AttemptRecord();
        r.attemptId = attemptId;
        r.userId = userId;
        r.username = username;
        r.quizId = quizId;
        r.quizTitle = quizTitle;
        r.category = category;
        r.score = score;
        r.total = total;
        r.accuracy = total == 0 ? 0 : (double) score / total;
        r.totalTimeMs = totalTimeMs;
        r.finishedAt = finishedAt;
        return r;
    }

    /** Score as a percentage using one consistent denominator (questions in that attempt). */
    public double pct() {
        return total <= 0 ? 0 : 100.0 * score / total;
    }

    public String categoryLabel() {
        return category == null || category.trim().isEmpty() ? "Uncategorized" : category.trim();
    }

    public String nameLabel() {
        return username == null || username.trim().isEmpty() ? "Unknown" : username;
    }
}
