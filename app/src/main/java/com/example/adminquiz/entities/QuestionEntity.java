package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "questions",
        foreignKeys = @ForeignKey(entity = QuizEntity.class, parentColumns = "id",
                childColumns = "quizId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("quizId")})
public class QuestionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long quizId;
    public int position;
    public String text;
    public String optA;
    public String optB;
    public String optC;
    public String optD;
    public int correctIndex;
    public String explanation;
    public String difficulty;

    public String option(int i) {
        switch (i) {
            case 0: return optA;
            case 1: return optB;
            case 2: return optC;
            default: return optD;
        }
    }

    public int optionCount() {
        int n = 0;
        for (int i = 0; i < 4; i++) {
            String o = option(i);
            if (o != null && !o.trim().isEmpty()) {
                n++;
            }
        }
        return n;
    }
}
