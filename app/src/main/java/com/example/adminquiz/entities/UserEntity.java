package com.example.adminquiz.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users",
        indices = {@Index(value = {"email"}, unique = true), @Index(value = {"username"}, unique = true)})
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String username;
    public String email;
    public String passwordHash;
    public String salt;
    public long createdAt;
    /** Class the student belongs to (upper-case). Null on rows created before DB v2 = GENERAL. */
    public String classCode;
}
