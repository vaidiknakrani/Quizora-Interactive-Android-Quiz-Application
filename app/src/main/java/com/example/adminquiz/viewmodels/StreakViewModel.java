package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.analytics.StreakCalculator;
import com.example.adminquiz.repositories.ClassRepository;
import com.example.adminquiz.utils.SessionManager;

import java.util.List;
import java.util.TimeZone;

/** The signed-in student's own activity calendar and streaks. */
public class StreakViewModel extends AndroidViewModel {
    private final LiveData<List<AttemptRecord>> mine;
    private final MutableLiveData<Long> tick = new MutableLiveData<>(System.currentTimeMillis());

    /** Null until the first load finishes. */
    public final MediatorLiveData<StreakCalculator.StreakStats> stats = new MediatorLiveData<>();

    public StreakViewModel(@NonNull Application app) {
        super(app);
        long userId = new SessionManager(app).getUserId();
        mine = new ClassRepository(app).myAttempts(userId);
        stats.addSource(mine, l -> recompute());
        stats.addSource(tick, t -> recompute());
    }

    /** Re-evaluates "today" (for example after midnight or a time-zone change). */
    public void refresh() {
        tick.setValue(System.currentTimeMillis());
    }

    private void recompute() {
        List<AttemptRecord> list = mine.getValue();
        if (list == null) {
            return;
        }
        stats.setValue(StreakCalculator.compute(list, System.currentTimeMillis(), TimeZone.getDefault()));
    }
}
