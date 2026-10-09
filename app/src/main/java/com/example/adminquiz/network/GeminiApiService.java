package com.example.adminquiz.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    Call<GeminiModels.GeminiResponse> generate(
            @Path("model") String model,
            @Header("x-goog-api-key") String apiKey,
            @Body GeminiModels.GeminiRequest body);
}
