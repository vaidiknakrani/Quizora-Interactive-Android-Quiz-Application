package com.example.adminquiz.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.adminquiz.analytics.AttemptRecord;
import com.example.adminquiz.dao.AttemptDao;
import com.example.adminquiz.dao.UserDao;
import com.example.adminquiz.database.AppDatabase;
import com.example.adminquiz.entities.UserEntity;
import com.example.adminquiz.utils.ClassCode;
import com.example.adminquiz.utils.Resource;

import java.util.List;

/**
 * Read-only class data plus the one write a student may make: changing their OWN class.
 * Rankings, streaks and analytics are always derived from saved attempts, never stored, so no
 * screen can edit another student's points or activity.
 */
public class ClassRepository {
    private final UserDao userDao;
    private final AttemptDao attemptDao;

    public ClassRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        userDao = db.userDao();
        attemptDao = db.attemptDao();
    }

    public LiveData<UserEntity> me(long userId) {
        return userDao.observe(userId);
    }

    public LiveData<List<AttemptRecord>> classAttempts(String classCode) {
        return attemptDao.classRecords(classCode);
    }

    public LiveData<Integer> enrolled(String classCode) {
        return userDao.countInClass(classCode);
    }

    public LiveData<List<AttemptRecord>> myAttempts(long userId) {
        return attemptDao.myRecordsLive(userId);
    }

    /** Blocking: call from a background thread. Only ever updates the given (signed-in) user. */
    public Resource<String> changeClass(long userId, String raw) {
        String code = ClassCode.normalize(raw);
        if (code == null) {
            return Resource.error("Class code must be 2-20 letters, numbers, - or _");
        }
        if (userDao.findById(userId) == null) {
            return Resource.error("Account not found");
        }
        userDao.updateClassCode(userId, code);
        return Resource.success(code);
    }
}
