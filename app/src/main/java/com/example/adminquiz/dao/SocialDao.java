package com.example.adminquiz.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.adminquiz.entities.MessageEntity;
import com.example.adminquiz.entities.ReviewEntity;

import java.util.List;

@Dao
public interface SocialDao {
    @Insert
    long insertMessage(MessageEntity message);

    @Update
    void updateMessage(MessageEntity message);

    @Query("SELECT * FROM messages WHERE id = :id")
    MessageEntity messageById(long id);

    @Query("SELECT * FROM messages WHERE quizId = :quizId ORDER BY createdAt DESC")
    LiveData<List<MessageEntity>> messages(long quizId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsertReview(ReviewEntity review);

    @Query("SELECT * FROM reviews WHERE quizId = :quizId ORDER BY createdAt DESC")
    LiveData<List<ReviewEntity>> reviews(long quizId);

    @Query("SELECT COALESCE(AVG(rating),0) FROM reviews WHERE quizId = :quizId")
    float avgRating(long quizId);

    @Query("SELECT COUNT(*) FROM reviews WHERE quizId = :quizId")
    int reviewCount(long quizId);
}
