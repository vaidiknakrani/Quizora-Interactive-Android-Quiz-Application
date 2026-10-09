package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.models.AiConfig;
import com.example.adminquiz.repositories.GeminiRepository;
import com.example.adminquiz.repositories.QuizRepository;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.SessionManager;

import java.util.List;

public class AiViewModel extends AndroidViewModel {
    private final GeminiRepository gemini;
    private final long userId;

    public final MutableLiveData<Resource<Long>> result = new MutableLiveData<>();
    public final LiveData<List<QuizEntity>> recentTests;

    public AiViewModel(@NonNull Application app) {
        super(app);
        gemini = new GeminiRepository(app);
        userId = new SessionManager(app).getUserId();
        recentTests = new QuizRepository(app).aiTests(userId);
    }

    public void generate(AiConfig cfg) {
        result.setValue(Resource.loading());
        gemini.generate(cfg, userId, new GeminiRepository.ResultCallback() {
            @Override
            public void onSuccess(long quizId) {
                result.postValue(Resource.success(quizId));
            }

            @Override
            public void onError(String message) {
                result.postValue(Resource.error(message));
            }
        });
    }

    public void consume() {
        result.setValue(null);
    }
}
