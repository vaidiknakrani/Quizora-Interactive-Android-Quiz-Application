package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.analytics.LeaderboardRanker;
import com.example.adminquiz.analytics.LeaderboardRanker.Mode;
import com.example.adminquiz.analytics.LeaderboardRanker.Period;
import com.example.adminquiz.repositories.ClassRepository;
import com.example.adminquiz.utils.ClassCode;
import com.example.adminquiz.utils.SessionManager;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/** Whole-class ranking. Recomputed automatically whenever a quiz result is saved. */
public class ClassLeaderboardViewModel extends AndroidViewModel {
    private final ClassRepository repo;
    private final long userId;
    private final LiveData<List<AttemptRecord>> noRows =
            new MutableLiveData<List<AttemptRecord>>(Collections.<AttemptRecord>emptyList());
    private final LiveData<Integer> noCount = new MutableLiveData<>(0);

    private final MutableLiveData<Period> period = new MutableLiveData<>(Period.WEEKLY);
    private final MutableLiveData<Mode> mode = new MutableLiveData<>(Mode.POINTS);
    private final MutableLiveData<Long> tick = new MutableLiveData<>(System.currentTimeMillis());
    private final LiveData<List<AttemptRecord>> rows;

    public final LiveData<String> classCode;
    public final LiveData<Integer> enrolled;
    /** Null until the first load finishes (screen shows a progress indicator). */
    public final MediatorLiveData<LeaderboardRanker.Result> board = new MediatorLiveData<>();

    public ClassLeaderboardViewModel(@NonNull Application app) {
        super(app);
        repo = new ClassRepository(app);
        userId = new SessionManager(app).getUserId();
        LiveData<String> code = Transformations.map(repo.me(userId),
                u -> u == null ? null : ClassCode.of(u.classCode));
        classCode = Transformations.distinctUntilChanged(code);
        rows = Transformations.switchMap(classCode, c -> c == null ? noRows : repo.classAttempts(c));
        enrolled = Transformations.switchMap(classCode, c -> c == null ? noCount : repo.enrolled(c));

        board.addSource(rows, r -> recompute());
        board.addSource(period, p -> recompute());
        board.addSource(mode, m -> recompute());
        board.addSource(tick, t -> recompute());
    }

    public long getUserId() {
        return userId;
    }

    public Period getPeriod() {
        Period p = period.getValue();
        return p == null ? Period.WEEKLY : p;
    }

    public Mode getMode() {
        Mode m = mode.getValue();
        return m == null ? Mode.POINTS : m;
    }

    public void setPeriod(Period p) {
        if (p != period.getValue()) {
            period.setValue(p);
        }
    }

    public void setMode(Mode m) {
        if (m != mode.getValue()) {
            mode.setValue(m);
        }
    }

    /** Call when the screen becomes visible so day boundaries (midnight) are re-evaluated. */
    public void refresh() {
        tick.setValue(System.currentTimeMillis());
    }

    private void recompute() {
        List<AttemptRecord> list = rows.getValue();
        if (list == null) {
            return;
        }
        board.setValue(LeaderboardRanker.rank(list, getPeriod(), getMode(), userId,
                System.currentTimeMillis(), TimeZone.getDefault()));
    }
}
