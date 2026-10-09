package com.example.adminquiz.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.adminquiz.entities.QuestionEntity;

import java.util.List;

@Dao
public interface QuestionDao {
    @Insert
    void insertAll(List<QuestionEntity> questions);

    @Query("DELETE FROM questions WHERE quizId = :quizId")
    void deleteForQuiz(long quizId);

    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY position")
    List<QuestionEntity> byQuiz(long quizId);

    @Query("SELECT COUNT(*) FROM questions WHERE quizId = :quizId")
    LiveData<Integer> countLive(long quizId);
}
