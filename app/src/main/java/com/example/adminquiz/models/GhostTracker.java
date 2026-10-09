package com.example.adminquiz.models;

import java.util.List;

/** Replays a recorded ghost run against the live clock. */
public class GhostTracker {
    private final List<GhostStep> steps;

    public GhostTracker(List<GhostStep> steps) {
        this.steps = steps;
    }

    public int scoreAt(long elapsedMs) {
        long t = 0;
        int score = 0;
        for (GhostStep s : steps) {
            t += s.timeMs;
            if (t > elapsedMs) {
                break;
            }
            if (s.correct) {
                score++;
            }
        }
        return score;
    }

    public long totalTimeMs() {
        long t = 0;
        for (GhostStep s : steps) {
            t += s.timeMs;
        }
        return t;
    }
}
