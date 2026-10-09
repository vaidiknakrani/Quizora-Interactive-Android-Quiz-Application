package com.example.adminquiz.utils;

import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public final class UiUtils {
    private UiUtils() {
    }

    public static void setupToolbar(AppCompatActivity activity, MaterialToolbar toolbar, String title) {
        activity.setSupportActionBar(toolbar);
        ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setTitle(title);
            bar.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v ->
                activity.getOnBackPressedDispatcher().onBackPressed());
    }

    public static String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    public static void toast(android.content.Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }
}
