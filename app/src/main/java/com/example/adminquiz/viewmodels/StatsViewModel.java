package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.adminquiz.entities.AttemptEntity;
import com.example.adminquiz.models.LeaderboardRow;
import com.example.adminquiz.models.ProfileData;
import com.example.adminquiz.repositories.AttemptRepository;
import com.example.adminquiz.repositories.StatsRepository;
import com.example.adminquiz.utils.AppExecutors;
import com.example.adminquiz.utils.SessionManager;

import java.util.List;

public class StatsViewModel extends AndroidViewModel {
    private final StatsRepository stats;
    private final long userId;
    private final MutableLiveData<Integer> period = new MutableLiveData<>(0);

    public final LiveData<List<LeaderboardRow>> leaderboard;
    public final LiveData<List<AttemptEntity>> history;
    public final LiveData<List<AttemptEntity>> personas;
    public final MutableLiveData<ProfileData> profile = new MutableLiveData<>();

    public StatsViewModel(@NonNull Application app) {
        super(app);
        stats = new StatsRepository(app);
        AttemptRepository attempts = new AttemptRepository(app);
        userId = new SessionManager(app).getUserId();
        leaderboard = Transformations.switchMap(period, stats::leaderboard);
        history = attempts.history(userId);
        personas = attempts.personas(userId);
    }

    public void setPeriod(int p) {
        period.setValue(p);
    }

    public void refreshProfile() {
        AppExecutors.io(() -> profile.postValue(stats.loadProfile(userId)));
    }
}
