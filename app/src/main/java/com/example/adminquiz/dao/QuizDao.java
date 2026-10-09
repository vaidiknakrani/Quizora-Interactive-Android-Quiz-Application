package com.example.adminquiz.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.adminquiz.entities.QuizEntity;

import java.util.List;

@Dao
public interface QuizDao {
    @Insert
    long insert(QuizEntity quiz);

    @Update
    int update(QuizEntity quiz);

    @Query("DELETE FROM quizzes WHERE id = :id")
    void delete(long id);

    @Query("SELECT * FROM quizzes WHERE id = :id")
    QuizEntity findById(long id);

    @Query("SELECT * FROM quizzes WHERE id = :id")
    LiveData<QuizEntity> observe(long id);

    @Query("SELECT * FROM quizzes WHERE published = 1 AND source = 'USER' "
            + "AND (title LIKE '%' || :q || '%' OR description LIKE '%' || :q || '%' OR category LIKE '%' || :q || '%') "
            + "AND (:category = '' OR category = :category) ORDER BY updatedAt DESC")
    LiveData<List<QuizEntity>> browse(String q, String category);

    @Query("SELECT * FROM quizzes WHERE creatorId = :uid AND source = 'USER' ORDER BY updatedAt DESC")
    LiveData<List<QuizEntity>> mine(long uid);

    @Query("SELECT * FROM quizzes WHERE creatorId = :uid AND source = 'AI' ORDER BY createdAt DESC LIMIT 20")
    LiveData<List<QuizEntity>> aiTests(long uid);

    @Query("UPDATE quizzes SET published = :published, updatedAt = :time WHERE id = :id")
    void setPublished(long id, boolean published, long time);

    @Query("UPDATE quizzes SET ratingAvg = :avg, ratingCount = :count WHERE id = :id")
    void setRating(long id, float avg, int count);

    @Query("SELECT COUNT(*) FROM quizzes WHERE creatorId = :uid AND source = 'USER'")
    int countCreated(long uid);
}
