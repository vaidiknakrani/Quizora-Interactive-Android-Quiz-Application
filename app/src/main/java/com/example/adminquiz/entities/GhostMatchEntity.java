package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A player's challenge against a ghost. status: PENDING, COMPLETED, REJECTED. won: 1, 0 (draw), -1. */
@Entity(tableName = "ghost_matches",
        foreignKeys = @ForeignKey(entity = GhostEntity.class, parentColumns = "id",
                childColumns = "ghostId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"ghostId", "playerUserId"}, unique = true)})
public class GhostMatchEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long ghostId;
    public long playerUserId;
    public String status;
    public int playerScore;
    public long playerTimeMs;
    public int won;
    public long updatedAt;
}
