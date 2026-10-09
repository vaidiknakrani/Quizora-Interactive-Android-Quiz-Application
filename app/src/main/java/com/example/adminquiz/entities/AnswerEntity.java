package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "answers",
        foreignKeys = @ForeignKey(entity = AttemptEntity.class, parentColumns = "id",
                childColumns = "attemptId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("attemptId")})
public class AnswerEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long attemptId;
    public int position;
    public String question;
    public String optA;
    public String optB;
    public String optC;
    public String optD;
    public int correctIndex;
    public int selectedIndex;
    public String explanation;
    public long timeMs;

    public String option(int i) {
        switch (i) {
            case 0: return optA;
            case 1: return optB;
            case 2: return optC;
            default: return optD;
        }
    }
}
