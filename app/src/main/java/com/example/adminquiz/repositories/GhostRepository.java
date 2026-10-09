package com.example.adminquiz.repositories;

import android.content.Context;
import android.util.Base64;

import androidx.lifecycle.LiveData;

import com.example.adminquiz.dao.AttemptDao;
import com.example.adminquiz.dao.GhostDao;
import com.example.adminquiz.dao.QuestionDao;
import com.example.adminquiz.dao.QuizDao;
import com.example.adminquiz.dao.UserDao;
import com.example.adminquiz.database.AppDatabase;
import com.example.adminquiz.entities.AnswerEntity;
import com.example.adminquiz.entities.AttemptEntity;
import com.example.adminquiz.entities.GhostEntity;
import com.example.adminquiz.entities.GhostMatchEntity;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.entities.UserEntity;
import com.example.adminquiz.models.GhostItem;
import com.example.adminquiz.models.GhostPayload;
import com.example.adminquiz.models.GhostStep;
import com.example.adminquiz.utils.Fmt;
import com.example.adminquiz.utils.Resource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Asynchronous ghost matches without a server: a ghost is stored locally and can be shared as
 * text. The share message carries a compressed copy of the quiz and recording, so another phone
 * can import it and race the ghost offline.
 */
public class GhostRepository {
    private static final Pattern CODE = Pattern.compile("QUIZ-[A-Z0-9]{5}");
    private static final Pattern PAYLOAD = Pattern.compile("\\[QG1:([A-Za-z0-9_=\\-]+)\\]");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int B64 = Base64.NO_WRAP | Base64.URL_SAFE;

    private final AppDatabase db;
    private final GhostDao ghostDao;
    private final QuizDao quizDao;
    private final QuestionDao questionDao;
    private final AttemptDao attemptDao;
    private final UserDao userDao;
    private final Gson gson = new Gson();

    public GhostRepository(Context context) {
        db = AppDatabase.getInstance(context);
        ghostDao = db.ghostDao();
        quizDao = db.quizDao();
        questionDao = db.questionDao();
        attemptDao = db.attemptDao();
        userDao = db.userDao();
    }

    public LiveData<List<GhostItem>> myGhosts(long uid) {
        return ghostDao.myGhosts(uid);
    }

    public LiveData<List<GhostItem>> matches(long uid, String status) {
        return ghostDao.matchesByStatus(uid, status);
    }

    public void reject(long matchId) {
        ghostDao.setMatchStatus(matchId, "REJECTED", System.currentTimeMillis());
    }

    public GhostEntity getGhostForMatch(long matchId) {
        GhostMatchEntity m = ghostDao.matchById(matchId);
        return m == null ? null : ghostDao.ghostById(m.ghostId);
    }

    public static List<GhostStep> parseSteps(String json) {
        List<GhostStep> list = null;
        try {
            list = new Gson().fromJson(json, new TypeToken<List<GhostStep>>() { }.getType());
        } catch (Exception ignored) {
            // fall through to empty list
        }
        return list == null ? new ArrayList<>() : list;
    }

    /** Records a finished attempt as a shareable ghost (reuses the ghost if it already exists). */
    public GhostEntity createGhost(long userId, long attemptId) {
        GhostEntity existing = ghostDao.ghostByAttempt(attemptId);
        if (existing != null) {
            return existing;
        }
        AttemptEntity attempt = attemptDao.byId(attemptId);
        if (attempt == null || quizDao.findById(attempt.quizId) == null) {
            return null;
        }
        List<AnswerEntity> answers = attemptDao.answers(attemptId);
        List<GhostStep> steps = new ArrayList<>();
        for (AnswerEntity a : answers) {
            steps.add(new GhostStep(a.selectedIndex == a.correctIndex, a.timeMs));
        }
        UserEntity owner = userDao.findById(userId);
        GhostEntity g = new GhostEntity();
        g.code = newCode();
        g.quizId = attempt.quizId;
        g.ownerUserId = userId;
        g.ownerName = owner == null ? "Player" : owner.username;
        g.sourceAttemptId = attemptId;
        g.score = attempt.score;
        g.total = attempt.total;
        g.totalTimeMs = attempt.totalTimeMs;
        g.recordingJson = gson.toJson(steps);
        g.createdAt = System.currentTimeMillis();
        g.id = ghostDao.insertGhost(g);
        return g;
    }

    private String newCode() {
        SecureRandom rnd = new SecureRandom();
        while (true) {
            StringBuilder sb = new StringBuilder("QUIZ-");
            for (int i = 0; i < 5; i++) {
                sb.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
            }
            if (ghostDao.ghostByCode(sb.toString()) == null) {
                return sb.toString();
            }
        }
    }

    public String buildShareText(long ghostId) {
        GhostEntity g = ghostDao.ghostById(ghostId);
        if (g == null) {
            return null;
        }
        GhostPayload p = new GhostPayload();
        p.quiz = quizDao.findById(g.quizId);
        p.questions = questionDao.byQuiz(g.quizId);
        p.ghost = g;
        String payload;
        try {
            payload = Base64.encodeToString(gzip(gson.toJson(p).getBytes("UTF-8")), B64);
        } catch (IOException e) {
            payload = "";
        }
        return "I challenged you to beat my quiz score!\n\nChallenge Code:\n" + g.code
                + "\n\nCan you beat my ghost?\nMy result: " + g.score + "/" + g.total
                + " in " + Fmt.time(g.totalTimeMs)
                + "\n\nIn the Quiz app open Ghost Matches, paste this whole message and tap Add Challenge.\n\n[QG1:"
                + payload + "]";
    }

    /** Accepts a pasted share message (or just a code) and creates a PENDING match for the user. */
    public Resource<Long> importChallenge(long userId, String text) {
        if (text == null || text.trim().isEmpty()) {
            return Resource.error("Paste a challenge code or the full challenge message.");
        }
        String code = find(CODE, text);
        if (code == null && text.trim().length() <= 12) {
            code = find(CODE, text.trim().toUpperCase(Locale.US));
        }
        GhostEntity ghost = code == null ? null : ghostDao.ghostByCode(code);

        if (ghost == null) {
            String b64 = find(PAYLOAD, text, 1);
            if (b64 == null) {
                return Resource.error("Challenge not found. Paste the complete message you received.");
            }
            try {
                GhostPayload p = gson.fromJson(new String(gunzip(Base64.decode(b64, B64)), "UTF-8"),
                        GhostPayload.class);
                if (p == null || p.quiz == null || p.ghost == null || p.questions == null || p.questions.isEmpty()) {
                    return Resource.error("The challenge data is damaged.");
                }
                GhostEntity dup = ghostDao.ghostByCode(p.ghost.code);
                if (dup != null) {
                    ghost = dup;
                } else {
                    ghost = importPayload(p);
                }
            } catch (Exception e) {
                return Resource.error("The challenge data is damaged.");
            }
        }

        GhostMatchEntity m = ghostDao.matchFor(ghost.id, userId);
        if (m == null) {
            m = new GhostMatchEntity();
            m.ghostId = ghost.id;
            m.playerUserId = userId;
            m.status = "PENDING";
            m.updatedAt = System.currentTimeMillis();
            m.id = ghostDao.insertMatch(m);
        } else if ("REJECTED".equals(m.status)) {
            ghostDao.setMatchStatus(m.id, "PENDING", System.currentTimeMillis());
        }
        return Resource.success(m.id);
    }

    private GhostEntity importPayload(GhostPayload p) {
        final GhostEntity[] out = new GhostEntity[1];
        db.runInTransaction(() -> {
            p.quiz.id = 0;
            p.quiz.published = false;
            p.quiz.source = "GHOST";
            p.quiz.creatorId = -1;
            long quizId = quizDao.insert(p.quiz);
            for (QuestionEntity q : p.questions) {
                q.id = 0;
                q.quizId = quizId;
            }
            questionDao.insertAll(p.questions);
            p.ghost.id = 0;
            p.ghost.quizId = quizId;
            p.ghost.ownerUserId = -1;
            p.ghost.sourceAttemptId = 0;
            p.ghost.id = ghostDao.insertGhost(p.ghost);
            out[0] = p.ghost;
        });
        return out[0];
    }

    private static String find(Pattern pattern, String text) {
        return find(pattern, text, 0);
    }

    private static String find(Pattern pattern, String text, int group) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(group) : null;
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = gz.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
        }
        return bos.toByteArray();
    }
}
