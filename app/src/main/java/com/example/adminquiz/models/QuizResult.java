package com.example.adminquiz.models;

import com.example.adminquiz.entities.AnswerEntity;

import java.util.List;

public class QuizResult {
    public int score;
    public int total;
    public double accuracy;
    public double speed;
    public double consistency;
    public int overallPct;
    public int longestStreak;
    public long totalTimeMs;
    public Persona persona;
    public List<AnswerEntity> answers;
}
