package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Learner question to the creator, with an optional creator reply. */
@Entity(tableName = "messages",
        foreignKeys = @ForeignKey(entity = QuizEntity.class, parentColumns = "id",
                childColumns = "quizId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("quizId")})
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long quizId;
    public long senderId;
    public String senderName;
    public String text;
    public String reply;
    public long createdAt;
    public long repliedAt;
}
