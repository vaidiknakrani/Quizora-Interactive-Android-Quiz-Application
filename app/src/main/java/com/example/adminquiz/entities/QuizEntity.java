package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** source: USER (creator quiz), AI (Gemini generated), GHOST (imported with a challenge). */
@Entity(tableName = "quizzes", indices = {@Index("creatorId")})
public class QuizEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String title;
    public String category;
    public String description;
    public String difficulty;
    public long creatorId;
    public String creatorName;
    public boolean published;
    public String source = "USER";
    public int timeLimitSec;
    public float ratingAvg;
    public int ratingCount;
    public long createdAt;
    public long updatedAt;
}
