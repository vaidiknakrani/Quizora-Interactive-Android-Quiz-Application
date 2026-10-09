package com.example.adminquiz.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.adminquiz.dao.AttemptDao;
import com.example.adminquiz.dao.GhostDao;
import com.example.adminquiz.dao.QuestionDao;
import com.example.adminquiz.dao.QuizDao;
import com.example.adminquiz.dao.SocialDao;
import com.example.adminquiz.dao.UserDao;
import com.example.adminquiz.entities.AnswerEntity;
import com.example.adminquiz.entities.AttemptEntity;
import com.example.adminquiz.entities.GhostEntity;
import com.example.adminquiz.entities.GhostMatchEntity;
import com.example.adminquiz.entities.MessageEntity;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.entities.ReviewEntity;
import com.example.adminquiz.entities.UserEntity;

@Database(entities = {UserEntity.class, QuizEntity.class, QuestionEntity.class, AttemptEntity.class,
        AnswerEntity.class, GhostEntity.class, GhostMatchEntity.class, MessageEntity.class,
        ReviewEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();

    public abstract QuizDao quizDao();

    public abstract QuestionDao questionDao();

    public abstract AttemptDao attemptDao();

    public abstract GhostDao ghostDao();

    public abstract SocialDao socialDao();

    /** v1 -> v2: students get a class code (existing students join the GENERAL class). */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN classCode TEXT");
            db.execSQL("UPDATE users SET classCode = 'GENERAL'");
        }
    };

    private static volatile AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "quiz.db")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
