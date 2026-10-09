package com.example.adminquiz;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class QuizApp extends Application {
    public static final String PREFS = "settings";
    public static final String KEY_DARK = "dark_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean(KEY_DARK, false)
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
