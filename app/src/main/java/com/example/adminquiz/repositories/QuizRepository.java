package com.example.adminquiz.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.adminquiz.dao.QuestionDao;
import com.example.adminquiz.dao.QuizDao;
import com.example.adminquiz.dao.SocialDao;
import com.example.adminquiz.dao.UserDao;
import com.example.adminquiz.database.AppDatabase;
import com.example.adminquiz.entities.MessageEntity;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.entities.ReviewEntity;
import com.example.adminquiz.entities.UserEntity;
import com.example.adminquiz.models.QuizWithQuestions;

import java.util.List;

/** Blocking methods must be called from a background thread; LiveData methods are main-safe. */
public class QuizRepository {
    private final AppDatabase db;
    private final QuizDao quizDao;
    private final QuestionDao questionDao;
    private final SocialDao socialDao;
    private final UserDao userDao;

    public QuizRepository(Context context) {
        db = AppDatabase.getInstance(context);
        quizDao = db.quizDao();
        questionDao = db.questionDao();
        socialDao = db.socialDao();
        userDao = db.userDao();
    }

    // ---------- observable ----------
    public LiveData<List<QuizEntity>> browse(String query, String category) {
        return quizDao.browse(query, category);
    }

    public LiveData<List<QuizEntity>> mine(long userId) {
        return quizDao.mine(userId);
    }

    public LiveData<List<QuizEntity>> aiTests(long userId) {
        return quizDao.aiTests(userId);
    }

    public LiveData<QuizEntity> observeQuiz(long id) {
        return quizDao.observe(id);
    }

    public LiveData<Integer> questionCount(long quizId) {
        return questionDao.countLive(quizId);
    }

    public LiveData<List<MessageEntity>> messages(long quizId) {
        return socialDao.messages(quizId);
    }

    public LiveData<List<ReviewEntity>> reviews(long quizId) {
        return socialDao.reviews(quizId);
    }

    // ---------- blocking ----------
    public QuizEntity getQuiz(long id) {
        return quizDao.findById(id);
    }

    public List<QuestionEntity> getQuestions(long quizId) {
        return questionDao.byQuiz(quizId);
    }

    public QuizWithQuestions loadForEdit(long id) {
        QuizWithQuestions d = new QuizWithQuestions();
        d.quiz = quizDao.findById(id);
        d.questions = questionDao.byQuiz(id);
        return d;
    }

    public long saveQuiz(QuizEntity quiz, List<QuestionEntity> questions) {
        final long[] id = {quiz.id};
        db.runInTransaction(() -> {
            long now = System.currentTimeMillis();
            quiz.updatedAt = now;
            if (quiz.id == 0) {
                quiz.createdAt = now;
                if (quiz.creatorName == null) {
                    UserEntity u = userDao.findById(quiz.creatorId);
                    quiz.creatorName = u == null ? "Unknown" : u.username;
                }
                id[0] = quizDao.insert(quiz);
            } else {
                quizDao.update(quiz);
                questionDao.deleteForQuiz(quiz.id);
            }
            for (int i = 0; i < questions.size(); i++) {
                QuestionEntity q = questions.get(i);
                q.id = 0;
                q.quizId = id[0];
                q.position = i;
            }
            questionDao.insertAll(questions);
        });
        return id[0];
    }

    public void setPublished(long id, boolean published) {
        quizDao.setPublished(id, published, System.currentTimeMillis());
    }

    public void deleteQuiz(long id) {
        quizDao.delete(id);
    }

    public void sendMessage(long quizId, long userId, String text) {
        UserEntity u = userDao.findById(userId);
        MessageEntity m = new MessageEntity();
        m.quizId = quizId;
        m.senderId = userId;
        m.senderName = u == null ? "Learner" : u.username;
        m.text = text;
        m.createdAt = System.currentTimeMillis();
        socialDao.insertMessage(m);
    }

    public void reply(long messageId, String reply) {
        MessageEntity m = socialDao.messageById(messageId);
        if (m != null) {
            m.reply = reply;
            m.repliedAt = System.currentTimeMillis();
            socialDao.updateMessage(m);
        }
    }

    public void saveReview(long quizId, long userId, int rating, String comment) {
        db.runInTransaction(() -> {
            UserEntity u = userDao.findById(userId);
            ReviewEntity r = new ReviewEntity();
            r.quizId = quizId;
            r.userId = userId;
            r.username = u == null ? "Learner" : u.username;
            r.rating = rating;
            r.comment = comment;
            r.createdAt = System.currentTimeMillis();
            socialDao.upsertReview(r);
            quizDao.setRating(quizId, socialDao.avgRating(quizId), socialDao.reviewCount(quizId));
        });
    }
}
