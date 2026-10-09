package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.adminquiz.repositories.ClassRepository;
import com.example.adminquiz.utils.AppExecutors;
import com.example.adminquiz.utils.ClassCode;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.SessionManager;

/** Shows and changes the signed-in student's own class (Settings screen). */
public class ClassSettingsViewModel extends AndroidViewModel {
    private final ClassRepository repo;
    private final long userId;

    public final LiveData<String> classCode;
    /** One-shot result: observers call {@link #consume()} after handling it. */
    public final MutableLiveData<Resource<String>> saveResult = new MutableLiveData<>();

    public ClassSettingsViewModel(@NonNull Application app) {
        super(app);
        repo = new ClassRepository(app);
        userId = new SessionManager(app).getUserId();
        classCode = Transformations.map(repo.me(userId),
                u -> u == null ? null : ClassCode.of(u.classCode));
    }

    public void save(String raw) {
        AppExecutors.io(() -> saveResult.postValue(repo.changeClass(userId, raw)));
    }

    public void consume() {
        saveResult.setValue(null);
    }
}
