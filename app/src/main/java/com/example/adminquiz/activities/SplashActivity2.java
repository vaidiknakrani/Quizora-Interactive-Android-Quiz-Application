package com.example.adminquiz.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.adminquiz.utils.SessionManager;

public class SplashActivity2 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Class<?> next = new SessionManager(this).isLoggedIn() ? MainActivity2.class : LoginActivity2.class;
        startActivity(new Intent(this, next));
        finish();
    }
}
