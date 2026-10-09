package com.example.adminquiz.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.adminquiz.dao.AttemptDao;
import com.example.adminquiz.dao.GhostDao;
import com.example.adminquiz.database.AppDatabase;
import com.example.adminquiz.entities.AnswerEntity;
import com.example.adminquiz.entities.AttemptEntity;
import com.example.adminquiz.entities.GhostEntity;
import com.example.adminquiz.entities.GhostMatchEntity;
import com.example.adminquiz.models.GhostComparison;
import com.example.adminquiz.models.GhostItem;
import com.example.adminquiz.models.QuizResult;
import com.example.adminquiz.models.QuizSession;
import com.example.adminquiz.models.ResultData;
import com.example.adminquiz.utils.TrustedClock;

import java.util.List;

public class AttemptRepository {
    private final AppDatabase db;
    private final AttemptDao attemptDao;
    private final GhostDao ghostDao;
    private final Context appContext;

    public AttemptRepository(Context context) {
        appContext = context.getApplicationContext();
        db = AppDatabase.getInstance(context);
        attemptDao = db.attemptDao();
        ghostDao = db.ghostDao();
    }

    /** Saves the attempt, its answers and (for ghost matches) the match result in one transaction. */
    public long save(long userId, QuizSession session, QuizResult result, long matchId) {
        final long[] id = new long[1];
        db.runInTransaction(() -> {
            AttemptEntity a = new AttemptEntity();
            a.userId = userId;
            a.quizId = session.quiz.id;
            a.quizTitle = session.quiz.title;
            a.score = result.score;
            a.total = result.total;
            a.accuracy = result.accuracy;
            a.speedPct = (int) Math.round(result.speed * 100);
            a.consistencyPct = (int) Math.round(result.consistency * 100);
            a.overallPct = result.overallPct;
            a.totalTimeMs = result.totalTimeMs;
            a.persona = result.persona.name();
            // Guarded time: a rolled-back or jumped phone clock cannot back-fill or fake streak days.
            a.finishedAt = TrustedClock.now(appContext);
            a.ghostMatchId = matchId;
            a.mode = matchId > 0 ? "GHOST" : ("AI".equals(session.quiz.source) ? "AI" : "NORMAL");

            if (matchId > 0) {
                GhostMatchEntity m = ghostDao.matchById(matchId);
                GhostEntity g = m == null ? null : ghostDao.ghostById(m.ghostId);
                if (m != null && g != null) {
                    int cmp = GhostComparison.compare(result.score, result.totalTimeMs, g.score, g.totalTimeMs);
                    a.ghostWon = cmp > 0 ? 1 : 0;
                    m.status = "COMPLETED";
                    m.playerScore = result.score;
                    m.playerTimeMs = result.totalTimeMs;
                    m.won = cmp;
                    m.updatedAt = System.currentTimeMillis();
                    ghostDao.updateMatch(m);
                }
            }
            id[0] = attemptDao.insert(a);
            for (AnswerEntity ans : result.answers) {
                ans.id = 0;
                ans.attemptId = id[0];
            }
            attemptDao.insertAnswers(result.answers);
        });
        return id[0];
    }

    public ResultData loadResult(long attemptId) {
        AttemptEntity a = attemptDao.byId(attemptId);
        if (a == null) {
            return null;
        }
        ResultData d = new ResultData();
        d.attempt = a;
        d.answers = attemptDao.answers(attemptId);
        if (a.ghostMatchId > 0) {
            GhostItem item = ghostDao.matchItem(a.ghostMatchId);
            d.ghost = item;
        }
        return d;
    }

    public LiveData<List<AttemptEntity>> history(long userId) {
        return attemptDao.history(userId);
    }

    public LiveData<List<AttemptEntity>> personas(long userId) {
        return attemptDao.personas(userId);
    }
}
