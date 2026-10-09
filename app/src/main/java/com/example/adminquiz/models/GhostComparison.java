package com.example.adminquiz.models;

public final class GhostComparison {
    private GhostComparison() {
    }

    /** 1 = player wins, 0 = draw, -1 = ghost wins. Higher score wins; equal score: lower time wins. */
    public static int compare(int playerScore, long playerTimeMs, int ghostScore, long ghostTimeMs) {
        if (playerScore != ghostScore) {
            return playerScore > ghostScore ? 1 : -1;
        }
        if (playerTimeMs != ghostTimeMs) {
            return playerTimeMs < ghostTimeMs ? 1 : -1;
        }
        return 0;
    }
}
