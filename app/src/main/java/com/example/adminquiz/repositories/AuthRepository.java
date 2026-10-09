package com.example.adminquiz.repositories;

import android.content.Context;
import android.util.Patterns;

import androidx.lifecycle.LiveData;

import com.example.adminquiz.dao.UserDao;
import com.example.adminquiz.database.AppDatabase;
import com.example.adminquiz.entities.UserEntity;
import com.example.adminquiz.utils.ClassCode;
import com.example.adminquiz.utils.PasswordHasher;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.SessionManager;

import java.util.Locale;

/** Local account system: salted PBKDF2 hashes in Room, session id in private prefs. */
public class AuthRepository {
    private final UserDao userDao;
    private final SessionManager session;

    public AuthRepository(Context context) {
        userDao = AppDatabase.getInstance(context).userDao();
        session = new SessionManager(context);
    }

    public Resource<Long> register(String username, String email, String password, String confirm) {
        return register(username, email, password, confirm, null);
    }

    /** classCode is optional: blank joins the GENERAL class. */
    public Resource<Long> register(String username, String email, String password, String confirm,
                                   String classCode) {
        username = username == null ? "" : username.trim();
        email = email == null ? "" : email.trim().toLowerCase(Locale.US);
        if (username.length() < 3) {
            return Resource.error("Username must be at least 3 characters");
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return Resource.error("Enter a valid email address");
        }
        if (password == null || password.length() < 6) {
            return Resource.error("Password must be at least 6 characters");
        }
        if (!password.equals(confirm)) {
            return Resource.error("Passwords do not match");
        }
        String normalizedClass = ClassCode.normalize(classCode);
        if (normalizedClass == null) {
            return Resource.error("Class code must be 2-20 letters, numbers, - or _");
        }
        if (userDao.findByEmail(email) != null) {
            return Resource.error("An account with this email already exists");
        }
        if (userDao.findByUsername(username) != null) {
            return Resource.error("This username is taken");
        }
        UserEntity u = new UserEntity();
        u.username = username;
        u.email = email;
        u.salt = PasswordHasher.newSalt();
        u.passwordHash = PasswordHasher.hash(password, u.salt);
        u.createdAt = System.currentTimeMillis();
        u.classCode = normalizedClass;
        u.id = userDao.insert(u);
        session.login(u.id);
        return Resource.success(u.id);
    }

    public Resource<Long> login(String email, String password) {
        email = email == null ? "" : email.trim().toLowerCase(Locale.US);
        if (email.isEmpty() || password == null || password.isEmpty()) {
            return Resource.error("Enter your email and password");
        }
        UserEntity u = userDao.findByEmail(email);
        if (u == null || !u.passwordHash.equals(PasswordHasher.hash(password, u.salt))) {
            return Resource.error("Incorrect email or password");
        }
        session.login(u.id);
        return Resource.success(u.id);
    }

    public void logout() {
        session.logout();
    }

    public LiveData<UserEntity> currentUser() {
        return userDao.observe(session.getUserId());
    }
}
