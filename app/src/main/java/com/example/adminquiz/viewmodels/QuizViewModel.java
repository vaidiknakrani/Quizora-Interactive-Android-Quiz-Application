package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.adminquiz.entities.MessageEntity;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.entities.ReviewEntity;
import com.example.adminquiz.models.QuizWithQuestions;
import com.example.adminquiz.repositories.QuizRepository;
import com.example.adminquiz.utils.AppExecutors;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.SessionManager;

import java.util.List;

/** Creator quizzes: browse, create, edit, publish, rate and message the creator. */
public class QuizViewModel extends AndroidViewModel {
    private final QuizRepository repo;
    private final long userId;
    private final MutableLiveData<String[]> filter = new MutableLiveData<>(new String[]{"", ""});

    public final LiveData<List<QuizEntity>> browseList;
    public final LiveData<List<QuizEntity>> mineList;
    public final MutableLiveData<Resource<Long>> saveResult = new MutableLiveData<>();
    public final MutableLiveData<QuizWithQuestions> editData = new MutableLiveData<>();
    public final MutableLiveData<Boolean> deleted = new MutableLiveData<>();

    public QuizViewModel(@NonNull Application app) {
        super(app);
        repo = new QuizRepository(app);
        userId = new SessionManager(app).getUserId();
        browseList = Transformations.switchMap(filter, f -> repo.browse(f[0], f[1]));
        mineList = repo.mine(userId);
    }

    public long getUserId() {
        return userId;
    }

    public void setFilter(String query, String category) {
        filter.setValue(new String[]{query == null ? "" : query, category == null ? "" : category});
    }

    public LiveData<QuizEntity> observeQuiz(long id) {
        return repo.observeQuiz(id);
    }

    public LiveData<Integer> questionCount(long id) {
        return repo.questionCount(id);
    }

    public LiveData<List<MessageEntity>> messages(long id) {
        return repo.messages(id);
    }

    public LiveData<List<ReviewEntity>> reviews(long id) {
        return repo.reviews(id);
    }

    public void loadForEdit(long id) {
        AppExecutors.io(() -> editData.postValue(repo.loadForEdit(id)));
    }

    public void saveQuiz(QuizEntity quiz, List<QuestionEntity> questions) {
        saveResult.setValue(Resource.loading());
        AppExecutors.io(() -> {
            try {
                saveResult.postValue(Resource.success(repo.saveQuiz(quiz, questions)));
            } catch (Exception e) {
                saveResult.postValue(Resource.error("Could not save the quiz"));
            }
        });
    }

    public void setPublished(long id, boolean published) {
        AppExecutors.io(() -> repo.setPublished(id, published));
    }

    public void deleteQuiz(long id) {
        AppExecutors.io(() -> {
            repo.deleteQuiz(id);
            deleted.postValue(true);
        });
    }

    public void sendMessage(long quizId, String text) {
        AppExecutors.io(() -> repo.sendMessage(quizId, userId, text));
    }

    public void reply(long messageId, String text) {
        AppExecutors.io(() -> repo.reply(messageId, text));
    }

    public void submitReview(long quizId, int rating, String comment) {
        AppExecutors.io(() -> repo.saveReview(quizId, userId, rating, comment));
    }
}
