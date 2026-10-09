package com.example.adminquiz.models;

public class Achievement {
    public final String title;
    public final String description;
    public final boolean unlocked;

    public Achievement(String title, String description, boolean unlocked) {
        this.title = title;
        this.description = description;
        this.unlocked = unlocked;
    }
}
