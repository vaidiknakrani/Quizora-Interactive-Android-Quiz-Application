package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.adminquiz.entities.GhostEntity;
import com.example.adminquiz.models.GhostItem;
import com.example.adminquiz.repositories.GhostRepository;
import com.example.adminquiz.utils.AppExecutors;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.SessionManager;

import java.util.List;

public class GhostViewModel extends AndroidViewModel {
    private final GhostRepository repo;
    private final long userId;

    public final LiveData<List<GhostItem>> myGhosts;
    public final LiveData<List<GhostItem>> pending;
    public final LiveData<List<GhostItem>> completed;
    /** One-shot events: observers must call the matching consume method after handling. */
    public final MutableLiveData<Resource<Long>> importResult = new MutableLiveData<>();
    public final MutableLiveData<Resource<String>> shareResult = new MutableLiveData<>();

    public GhostViewModel(@NonNull Application app) {
        super(app);
        repo = new GhostRepository(app);
        userId = new SessionManager(app).getUserId();
        myGhosts = repo.myGhosts(userId);
        pending = repo.matches(userId, "PENDING");
        completed = repo.matches(userId, "COMPLETED");
    }

    public void importChallenge(String text) {
        AppExecutors.io(() -> {
            try {
                importResult.postValue(repo.importChallenge(userId, text));
            } catch (Exception e) {
                importResult.postValue(Resource.error("Could not add this challenge."));
            }
        });
    }

    public void reject(long matchId) {
        AppExecutors.io(() -> repo.reject(matchId));
    }

    /** Creates (or reuses) a ghost for an attempt and returns the text to share. */
    public void createAndShare(long attemptId) {
        AppExecutors.io(() -> {
            GhostEntity g = repo.createGhost(userId, attemptId);
            String text = g == null ? null : repo.buildShareText(g.id);
            shareResult.postValue(text == null
                    ? Resource.error("Could not create the challenge.") : Resource.success(text));
        });
    }

    public void shareExisting(long ghostId) {
        AppExecutors.io(() -> {
            String text = repo.buildShareText(ghostId);
            shareResult.postValue(text == null
                    ? Resource.error("Could not create the challenge.") : Resource.success(text));
        });
    }

    public void consumeImport() {
        importResult.setValue(null);
    }

    public void consumeShare() {
        shareResult.setValue(null);
    }
}
