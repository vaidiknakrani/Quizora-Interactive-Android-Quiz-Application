package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A recorded run that other players can race against. */
@Entity(tableName = "ghosts",
        foreignKeys = @ForeignKey(entity = QuizEntity.class, parentColumns = "id",
                childColumns = "quizId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"code"}, unique = true), @Index("quizId")})
public class GhostEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String code;
    public long quizId;
    public long ownerUserId;
    public String ownerName;
    public long sourceAttemptId;
    public int score;
    public int total;
    public long totalTimeMs;
    public String recordingJson;
    public long createdAt;
}
