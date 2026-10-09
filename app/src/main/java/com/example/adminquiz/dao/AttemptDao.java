package com.example.adminquiz.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.entities.AnswerEntity;
import com.example.adminquiz.entities.AttemptEntity;
import com.example.adminquiz.models.LeaderboardRow;
import com.example.adminquiz.models.UserStats;

import java.util.List;

@Dao
public interface AttemptDao {
    @Insert
    long insert(AttemptEntity attempt);

    @Insert
    void insertAnswers(List<AnswerEntity> answers);

    @Query("SELECT * FROM attempts WHERE id = :id")
    AttemptEntity byId(long id);

    @Query("SELECT * FROM answers WHERE attemptId = :attemptId ORDER BY position")
    List<AnswerEntity> answers(long attemptId);

    @Query("SELECT * FROM attempts WHERE userId = :uid ORDER BY finishedAt DESC LIMIT 50")
    LiveData<List<AttemptEntity>> history(long uid);

    @Query("SELECT * FROM attempts WHERE userId = :uid AND persona IS NOT NULL ORDER BY finishedAt DESC")
    LiveData<List<AttemptEntity>> personas(long uid);

    @Query("SELECT finishedAt FROM attempts WHERE userId = :uid")
    List<Long> finishTimes(long uid);

    @Query("SELECT COUNT(*) AS quizzes, COALESCE(SUM(score),0) AS totalScore, "
            + "COALESCE(AVG(accuracy),0) AS avgAccuracy, COALESCE(MAX(score),0) AS bestScore, "
            + "COALESCE(SUM(ghostWon),0) AS ghostWins, COALESCE(SUM(total),0) AS questionsAnswered, "
            + "COALESCE(SUM(CASE WHEN mode='AI' THEN 1 ELSE 0 END),0) AS aiTests, "
            + "COALESCE(SUM(CASE WHEN accuracy >= 0.999 THEN 1 ELSE 0 END),0) AS perfects "
            + "FROM attempts WHERE userId = :uid")
    UserStats stats(long uid);

    String LEADERBOARD_SQL = "SELECT u.id AS userId, u.username AS username, "
            + "COALESCE(SUM(a.score),0) AS totalScore, COALESCE(AVG(a.accuracy),0) AS accuracy, "
            + "COALESCE(SUM(a.ghostWon),0) AS ghostWins "
            + "FROM users u JOIN attempts a ON a.userId = u.id WHERE a.finishedAt >= :since "
            + "GROUP BY u.id ORDER BY totalScore DESC, accuracy DESC LIMIT 100";

    @Query(LEADERBOARD_SQL)
    LiveData<List<LeaderboardRow>> leaderboardLive(long since);

    @Query(LEADERBOARD_SQL)
    List<LeaderboardRow> leaderboard(long since);

    // ---- class leaderboard, streaks and analytics (read-only views over saved attempts) ----

    /**
     * A valid attempt asked at least one question and answered at least half of them. Submitting a
     * blank quiz therefore never counts towards streaks, rankings or class statistics.
     */
    String VALID_ATTEMPT = "a.total > 0 AND (SELECT COUNT(*) FROM answers w WHERE w.attemptId = a.id "
            + "AND w.selectedIndex >= 0) * 2 >= a.total ";

    String RECORD_SELECT = "SELECT a.id AS attemptId, a.userId AS userId, u.username AS username, "
            + "a.quizId AS quizId, a.quizTitle AS quizTitle, q.category AS category, a.score AS score, "
            + "a.total AS total, a.accuracy AS accuracy, a.totalTimeMs AS totalTimeMs, "
            + "a.finishedAt AS finishedAt FROM attempts a JOIN users u ON u.id = a.userId "
            + "LEFT JOIN quizzes q ON q.id = a.quizId ";

    @Query(RECORD_SELECT + "WHERE COALESCE(u.classCode, 'GENERAL') = :classCode AND " + VALID_ATTEMPT
            + "ORDER BY a.finishedAt")
    LiveData<List<AttemptRecord>> classRecords(String classCode);

    @Query(RECORD_SELECT + "WHERE a.userId = :uid AND " + VALID_ATTEMPT + "ORDER BY a.finishedAt")
    LiveData<List<AttemptRecord>> myRecordsLive(long uid);

    @Query(RECORD_SELECT + "WHERE a.userId = :uid AND " + VALID_ATTEMPT + "ORDER BY a.finishedAt")
    List<AttemptRecord> myRecords(long uid);
}
