package com.example.adminquiz.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.adminquiz.entities.GhostEntity;
import com.example.adminquiz.entities.GhostMatchEntity;
import com.example.adminquiz.models.GhostItem;

import java.util.List;

@Dao
public interface GhostDao {
    @Insert
    long insertGhost(GhostEntity ghost);

    @Insert
    long insertMatch(GhostMatchEntity match);

    @Update
    void updateMatch(GhostMatchEntity match);

    @Query("SELECT * FROM ghosts WHERE code = :code LIMIT 1")
    GhostEntity ghostByCode(String code);

    @Query("SELECT * FROM ghosts WHERE id = :id")
    GhostEntity ghostById(long id);

    @Query("SELECT * FROM ghosts WHERE sourceAttemptId = :attemptId LIMIT 1")
    GhostEntity ghostByAttempt(long attemptId);

    @Query("SELECT * FROM ghost_matches WHERE id = :id")
    GhostMatchEntity matchById(long id);

    @Query("SELECT * FROM ghost_matches WHERE ghostId = :ghostId AND playerUserId = :playerId LIMIT 1")
    GhostMatchEntity matchFor(long ghostId, long playerId);

    @Query("UPDATE ghost_matches SET status = :status, updatedAt = :time WHERE id = :id")
    void setMatchStatus(long id, String status, long time);

    String MATCH_SELECT = "SELECT g.id AS ghostId, m.id AS matchId, g.quizId AS quizId, g.code AS code, "
            + "q.title AS quizTitle, g.ownerName AS ownerName, g.score AS ghostScore, g.total AS total, "
            + "g.totalTimeMs AS ghostTimeMs, m.playerScore AS playerScore, m.playerTimeMs AS playerTimeMs, "
            + "m.won AS won, m.status AS status FROM ghost_matches m "
            + "JOIN ghosts g ON g.id = m.ghostId JOIN quizzes q ON q.id = g.quizId ";

    @Query("SELECT g.id AS ghostId, 0 AS matchId, g.quizId AS quizId, g.code AS code, q.title AS quizTitle, "
            + "g.ownerName AS ownerName, g.score AS ghostScore, g.total AS total, g.totalTimeMs AS ghostTimeMs, "
            + "0 AS playerScore, 0 AS playerTimeMs, 0 AS won, 'OWNED' AS status "
            + "FROM ghosts g JOIN quizzes q ON q.id = g.quizId WHERE g.ownerUserId = :uid ORDER BY g.createdAt DESC")
    LiveData<List<GhostItem>> myGhosts(long uid);

    @Query(MATCH_SELECT + "WHERE m.playerUserId = :uid AND m.status = :status ORDER BY m.updatedAt DESC")
    LiveData<List<GhostItem>> matchesByStatus(long uid, String status);

    @Query(MATCH_SELECT + "WHERE m.id = :matchId")
    GhostItem matchItem(long matchId);
}
