package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** mode: NORMAL, AI or GHOST. */
@Entity(tableName = "attempts", indices = {@Index("userId")})
public class AttemptEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public long quizId;
    public String quizTitle;
    public String mode;
    public int score;
    public int total;
    public double accuracy;
    public int speedPct;
    public int consistencyPct;
    public int overallPct;
    public long totalTimeMs;
    public String persona;
    public int ghostWon;
    public long ghostMatchId;
    public long finishedAt;
}
