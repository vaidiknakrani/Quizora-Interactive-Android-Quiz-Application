package com.example.adminquiz.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.adminquiz.BuildConfig;

/** Encrypted storage for the Gemini API key. Falls back to private prefs if the keystore fails. */
public class SecureStore {
    private static final String KEY_GEMINI = "gemini_key";
    private final SharedPreferences prefs;

    public SecureStore(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences p;
        try {
            MasterKey masterKey = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            p = EncryptedSharedPreferences.create(app, "secure_prefs", masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) {
            p = app.getSharedPreferences("secure_prefs_fallback", Context.MODE_PRIVATE);
        }
        prefs = p;
    }

    /** Key saved in Settings wins; otherwise the development key from local.properties. */
    public String getGeminiKey() {
        String saved = prefs.getString(KEY_GEMINI, "");
        if (saved != null && !saved.trim().isEmpty()) {
            return saved.trim();
        }
        return BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
    }

    public boolean hasSavedKey() {
        String saved = prefs.getString(KEY_GEMINI, "");
        return saved != null && !saved.trim().isEmpty();
    }

    public void setGeminiKey(String key) {
        prefs.edit().putString(KEY_GEMINI, key == null ? "" : key.trim()).apply();
    }
}
