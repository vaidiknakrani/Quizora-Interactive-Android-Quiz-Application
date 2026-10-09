package com.example.adminquiz.models;

import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;

import java.util.List;

/** Reusable quiz state shared by Normal, Creator, Ghost and AI quizzes. */
public class QuizSession {
    public final QuizEntity quiz;
    private final List<QuestionEntity> questions;
    private final int[] selected;
    private final long[] timeMs;
    private int current;
    private long startedAt;
    private long enteredAt;
    private long finishedAt;
    private boolean begun;

    public QuizSession(QuizEntity quiz, List<QuestionEntity> questions) {
        this.quiz = quiz;
        this.questions = questions;
        this.selected = new int[questions.size()];
        this.timeMs = new long[questions.size()];
        java.util.Arrays.fill(selected, -1);
    }

    public void begin() {
        if (begun) {
            return;
        }
        begun = true;
        startedAt = System.currentTimeMillis();
        enteredAt = startedAt;
    }

    public boolean isBegun() {
        return begun;
    }

    public int size() {
        return questions.size();
    }

    public int getCurrentIndex() {
        return current;
    }

    public QuestionEntity getQuestion(int i) {
        return questions.get(i);
    }

    public QuestionEntity getCurrentQuestion() {
        return questions.get(current);
    }

    public void select(int option) {
        selected[current] = option;
    }

    public int getSelected(int i) {
        return selected[i];
    }

    public void goTo(int index) {
        if (index < 0 || index >= size() || index == current) {
            return;
        }
        commit();
        current = index;
    }

    private void commit() {
        long now = System.currentTimeMillis();
        timeMs[current] += now - enteredAt;
        enteredAt = now;
    }

    public void finish() {
        if (finishedAt == 0) {
            commit();
            finishedAt = System.currentTimeMillis();
        }
    }

    public long getTimeMs(int i) {
        return timeMs[i];
    }

    public long getElapsedMs() {
        if (!begun) {
            return 0;
        }
        return (finishedAt > 0 ? finishedAt : System.currentTimeMillis()) - startedAt;
    }

    public long getTimeLimitMs() {
        return quiz.timeLimitSec * 1000L;
    }

    public long getRemainingMs() {
        return Math.max(0, getTimeLimitMs() - getElapsedMs());
    }

    public int answeredCount() {
        int n = 0;
        for (int s : selected) {
            if (s >= 0) {
                n++;
            }
        }
        return n;
    }

    public int correctSoFar() {
        int n = 0;
        for (int i = 0; i < selected.length; i++) {
            if (selected[i] == questions.get(i).correctIndex) {
                n++;
            }
        }
        return n;
    }
}
