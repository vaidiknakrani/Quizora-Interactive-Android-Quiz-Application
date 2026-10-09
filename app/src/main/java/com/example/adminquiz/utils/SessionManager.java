package com.example.adminquiz.utils;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores only the logged-in user id. Passwords are never stored in plain text. */
public class SessionManager {
    private static final String PREFS = "session";
    private static final String KEY_USER = "user_id";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void login(long userId) {
        prefs.edit().putLong(KEY_USER, userId).apply();
    }

    public void logout() {
        prefs.edit().remove(KEY_USER).apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER, -1L);
    }

    public boolean isLoggedIn() {
        return getUserId() > 0;
    }
}
