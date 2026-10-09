package com.example.adminquiz.models;

/** One recorded question of a ghost run. */
public class GhostStep {
    public boolean correct;
    public long timeMs;

    public GhostStep() {
    }

    public GhostStep(boolean correct, long timeMs) {
        this.correct = correct;
        this.timeMs = timeMs;
    }
}
