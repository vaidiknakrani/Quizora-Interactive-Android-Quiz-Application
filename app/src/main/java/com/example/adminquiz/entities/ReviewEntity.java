package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviews",
        foreignKeys = @ForeignKey(entity = QuizEntity.class, parentColumns = "id",
                childColumns = "quizId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"quizId", "userId"}, unique = true)})
public class ReviewEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long quizId;
    public long userId;
    public String username;
    public int rating;
    public String comment;
    public long createdAt;
}
