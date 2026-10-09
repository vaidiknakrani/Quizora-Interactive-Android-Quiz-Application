package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.adminquiz.models.ResultData;
import com.example.adminquiz.repositories.AttemptRepository;
import com.example.adminquiz.utils.AppExecutors;

public class ResultViewModel extends AndroidViewModel {
    private final AttemptRepository repo;
    public final MutableLiveData<ResultData> data = new MutableLiveData<>();

    public ResultViewModel(@NonNull Application app) {
        super(app);
        repo = new AttemptRepository(app);
    }

    public void load(long attemptId) {
        if (data.getValue() != null) {
            return;
        }
        AppExecutors.io(() -> data.postValue(repo.loadResult(attemptId)));
    }
}
