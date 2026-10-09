package com.example.adminquiz.network;

import com.example.adminquiz.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";
    private static GeminiApiService gemini;

    private RetrofitClient() {
    }

    public static synchronized GeminiApiService gemini() {
        if (gemini == null) {
            OkHttpClient.Builder client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                logging.redactHeader("x-goog-api-key");
                client.addInterceptor(logging);
            }
            gemini = new Retrofit.Builder()
                    .baseUrl(GEMINI_BASE_URL)
                    .client(client.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GeminiApiService.class);
        }
        return gemini;
    }
}
