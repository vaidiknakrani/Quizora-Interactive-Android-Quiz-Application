package com.example.adminquiz.models;

/** Flattened row used by the Ghost Matches screen. status: OWNED, PENDING, COMPLETED. */
public class GhostItem {
    public long ghostId;
    public long matchId;
    public long quizId;
    public String code;
    public String quizTitle;
    public String ownerName;
    public int ghostScore;
    public int total;
    public long ghostTimeMs;
    public int playerScore;
    public long playerTimeMs;
    public int won;
    public String status;
}
