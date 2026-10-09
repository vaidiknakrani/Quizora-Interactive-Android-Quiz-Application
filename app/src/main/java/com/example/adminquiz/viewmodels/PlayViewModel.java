package com.example.adminquiz.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.adminquiz.entities.GhostEntity;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.models.GhostTracker;
import com.example.adminquiz.models.QuizEvaluator;
import com.example.adminquiz.models.QuizResult;
import com.example.adminquiz.models.QuizSession;
import com.example.adminquiz.repositories.AttemptRepository;
import com.example.adminquiz.repositories.GhostRepository;
import com.example.adminquiz.repositories.QuizRepository;
import com.example.adminquiz.utils.AppExecutors;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.SessionManager;

import java.util.List;

/** One ViewModel drives every quiz mode: normal, creator, AI and ghost. */
public class PlayViewModel extends AndroidViewModel {
    private final QuizRepository quizRepo;
    private final AttemptRepository attemptRepo;
    private final GhostRepository ghostRepo;
    private final long userId;

    public final MutableLiveData<Resource<QuizSession>> sessionState = new MutableLiveData<>();
    public final MutableLiveData<Long> finished = new MutableLiveData<>();

    private QuizSession session;
    private GhostTracker ghost;
    private long matchId;
    private boolean loading;
    private boolean submitting;

    public PlayViewModel(@NonNull Application app) {
        super(app);
        quizRepo = new QuizRepository(app);
        attemptRepo = new AttemptRepository(app);
        ghostRepo = new GhostRepository(app);
        userId = new SessionManager(app).getUserId();
    }

    public QuizSession getSession() {
        return session;
    }

    public GhostTracker getGhost() {
        return ghost;
    }

    public boolean isGhostMode() {
        return ghost != null;
    }

    public void load(long quizId, long matchId) {
        if (session != null || loading) {
            return;
        }
        loading = true;
        this.matchId = matchId;
        sessionState.setValue(Resource.loading());
        AppExecutors.io(() -> {
            QuizEntity quiz = quizRepo.getQuiz(quizId);
            List<QuestionEntity> questions = quiz == null ? null : quizRepo.getQuestions(quizId);
            if (quiz == null || questions == null || questions.isEmpty()) {
                loading = false;
                sessionState.postValue(Resource.error("This quiz is no longer available."));
                return;
            }
            if (matchId > 0) {
                GhostEntity g = ghostRepo.getGhostForMatch(matchId);
                if (g == null) {
                    loading = false;
                    sessionState.postValue(Resource.error("This challenge is no longer available."));
                    return;
                }
                ghost = new GhostTracker(GhostRepository.parseSteps(g.recordingJson));
            }
            session = new QuizSession(quiz, questions);
            loading = false;
            sessionState.postValue(Resource.success(session));
        });
    }

    public void submit() {
        if (submitting || session == null) {
            return;
        }
        submitting = true;
        AppExecutors.io(() -> {
            QuizResult result = QuizEvaluator.evaluate(session);
            long attemptId = attemptRepo.save(userId, session, result, matchId);
            finished.postValue(attemptId);
        });
    }
}
