package com.example.adminquiz.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.adminquiz.analytics.StreakCalculator;
import com.example.adminquiz.dao.AttemptDao;
import com.example.adminquiz.dao.QuizDao;
import com.example.adminquiz.dao.UserDao;
import com.example.adminquiz.database.AppDatabase;
import com.example.adminquiz.entities.UserEntity;
import com.example.adminquiz.models.Achievement;
import com.example.adminquiz.models.LeaderboardRow;
import com.example.adminquiz.models.ProfileData;
import com.example.adminquiz.models.UserStats;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeSet;

public class StatsRepository {
    private static final long DAY = 24L * 60 * 60 * 1000;
    private final AttemptDao attemptDao;
    private final QuizDao quizDao;
    private final UserDao userDao;

    public StatsRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        attemptDao = db.attemptDao();
        quizDao = db.quizDao();
        userDao = db.userDao();
    }

    /** period: 0 = Global, 1 = Weekly, 2 = Monthly. */
    public static long sinceFor(int period, long now) {
        switch (period) {
            case 1: return now - 7 * DAY;
            case 2: return now - 30 * DAY;
            default: return 0;
        }
    }

    public LiveData<List<LeaderboardRow>> leaderboard(int period) {
        return attemptDao.leaderboardLive(sinceFor(period, System.currentTimeMillis()));
    }

    public ProfileData loadProfile(long userId) {
        UserEntity u = userDao.findById(userId);
        ProfileData p = new ProfileData();
        p.username = u == null ? "Unknown" : u.username;
        p.email = u == null ? "" : u.email;
        p.stats = attemptDao.stats(userId);
        p.quizzesCreated = quizDao.countCreated(userId);
        long now = System.currentTimeMillis();
        // Same rules as the Activity screen: valid attempts only, local calendar days, no future dates.
        StreakCalculator.StreakStats streak =
                StreakCalculator.compute(attemptDao.myRecords(userId), now, TimeZone.getDefault());
        p.streakDays = streak.currentStreak;

        boolean top10 = false;
        List<LeaderboardRow> board = attemptDao.leaderboard(0);
        for (int i = 0; i < board.size() && i < 10; i++) {
            if (board.get(i).userId == userId) {
                top10 = true;
            }
        }
        // The 7 Day Streak achievement uses the longest streak ever, which never resets.
        p.achievements = achievements(p.stats, p.quizzesCreated, streak.maxStreak, top10);
        return p;
    }

    public static List<Achievement> achievements(UserStats s, int created, int streak, boolean top10) {
        List<Achievement> a = new ArrayList<>();
        a.add(new Achievement("First Quiz", "Complete your first quiz", s.quizzes >= 1));
        a.add(new Achievement("10 Quizzes Completed", "Complete 10 quizzes", s.quizzes >= 10));
        a.add(new Achievement("100 Questions Answered", "Answer 100 questions", s.questionsAnswered >= 100));
        a.add(new Achievement("Ghost Champion", "Beat 3 ghosts", s.ghostWins >= 3));
        a.add(new Achievement("Perfect Score", "Get every answer right", s.perfects >= 1));
        a.add(new Achievement("AI Explorer", "Finish an AI test", s.aiTests >= 1));
        a.add(new Achievement("Quiz Creator", "Create a quiz", created >= 1));
        a.add(new Achievement("Top 10", "Reach the global top 10", top10));
        a.add(new Achievement("7 Day Streak", "Play 7 days in a row", streak >= 7));
        return a;
    }

    public static int computeStreak(List<Long> times, long now) {
        TreeSet<Long> days = new TreeSet<>();
        for (Long t : times) {
            days.add(dayOf(t));
        }
        long today = dayOf(now);
        long d = days.contains(today) ? today : today - 1;
        int streak = 0;
        while (days.contains(d)) {
            streak++;
            d--;
        }
        return streak;
    }

    private static long dayOf(long t) {
        return (t + TimeZone.getDefault().getOffset(t)) / DAY;
    }
}
