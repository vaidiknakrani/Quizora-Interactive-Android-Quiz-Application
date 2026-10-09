package com.example.adminquiz.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.adminquiz.entities.UserEntity;

@Dao
public interface UserDao {
    @Insert
    long insert(UserEntity user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity findByEmail(String email);

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    UserEntity findByUsername(String username);

    @Query("SELECT * FROM users WHERE id = :id")
    UserEntity findById(long id);

    @Query("SELECT * FROM users WHERE id = :id")
    LiveData<UserEntity> observe(long id);

    @Query("UPDATE users SET classCode = :classCode WHERE id = :id")
    int updateClassCode(long id, String classCode);

    @Query("SELECT COUNT(*) FROM users WHERE COALESCE(classCode, 'GENERAL') = :classCode")
    LiveData<Integer> countInClass(String classCode);
}
