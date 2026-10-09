package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.adminquiz.entities.UserEntity;
import com.example.adminquiz.repositories.AuthRepository;
import com.example.adminquiz.utils.AppExecutors;
import com.example.adminquiz.utils.Resource;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repo;
    public final MutableLiveData<Resource<Long>> authResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository(app);
    }

    public void login(String email, String password) {
        authResult.setValue(Resource.loading());
        AppExecutors.io(() -> authResult.postValue(repo.login(email, password)));
    }

    public void register(String username, String email, String password, String confirm) {
        register(username, email, password, confirm, null);
    }

    public void register(String username, String email, String password, String confirm,
                         String classCode) {
        authResult.setValue(Resource.loading());
        AppExecutors.io(() -> authResult.postValue(
                repo.register(username, email, password, confirm, classCode)));
    }

    public void logout() {
        repo.logout();
    }

    public LiveData<UserEntity> currentUser() {
        return repo.currentUser();
    }
}
