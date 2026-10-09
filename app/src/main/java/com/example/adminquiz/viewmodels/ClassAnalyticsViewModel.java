package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.analytics.ClassAnalytics;
import com.example.adminquiz.analytics.ClassAnalytics.Filter;
import com.example.adminquiz.repositories.ClassRepository;
import com.example.adminquiz.utils.ClassCode;
import com.example.adminquiz.utils.SessionManager;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

/** Class performance report for the selected range, subject and quiz. */
public class ClassAnalyticsViewModel extends AndroidViewModel {
    private final long userId;
    private final LiveData<List<AttemptRecord>> noRows =
            new MutableLiveData<List<AttemptRecord>>(Collections.<AttemptRecord>emptyList());
    private final LiveData<Integer> noCount = new MutableLiveData<>(0);
    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.all().withRange(30));
    private final MutableLiveData<Long> tick = new MutableLiveData<>(System.currentTimeMillis());
    private final LiveData<List<AttemptRecord>> rows;

    public final LiveData<String> classCode;
    public final LiveData<Integer> enrolled;
    /** Null until the first load finishes. */
    public final MediatorLiveData<ClassAnalytics.Report> report = new MediatorLiveData<>();

    public ClassAnalyticsViewModel(@NonNull Application app) {
        super(app);
        ClassRepository repo = new ClassRepository(app);
        userId = new SessionManager(app).getUserId();
        LiveData<String> code = Transformations.map(repo.me(userId),
                u -> u == null ? null : ClassCode.of(u.classCode));
        classCode = Transformations.distinctUntilChanged(code);
        rows = Transformations.switchMap(classCode, c -> c == null ? noRows : repo.classAttempts(c));
        enrolled = Transformations.switchMap(classCode, c -> c == null ? noCount : repo.enrolled(c));

        report.addSource(rows, r -> recompute());
        report.addSource(enrolled, e -> recompute());
        report.addSource(filter, f -> recompute());
        report.addSource(tick, t -> recompute());
    }

    public Filter getFilter() {
        Filter f = filter.getValue();
        return f == null ? Filter.all() : f;
    }

    public void setRange(int days) {
        Filter f = getFilter();
        if (f.rangeDays != days) {
            filter.setValue(f.withRange(days));
        }
    }

    /** Null means all subjects. */
    public void setCategory(String category) {
        Filter f = getFilter();
        String current = f.category;
        if (current == null ? category != null : !current.equals(category)) {
            filter.setValue(f.withCategory(category));
        }
    }

    /** Negative means all quizzes. */
    public void setQuiz(long quizId) {
        Filter f = getFilter();
        if (f.quizId != quizId) {
            filter.setValue(f.withQuiz(quizId));
        }
    }

    public void refresh() {
        tick.setValue(System.currentTimeMillis());
    }

    private void recompute() {
        List<AttemptRecord> list = rows.getValue();
        Integer enrolledCount = enrolled.getValue();
        if (list == null || enrolledCount == null) {
            return;
        }
        report.setValue(ClassAnalytics.build(list, enrolledCount, userId, getFilter(),
                System.currentTimeMillis(), TimeZone.getDefault()));
    }
}
