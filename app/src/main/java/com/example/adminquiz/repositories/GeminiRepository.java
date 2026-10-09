package com.example.adminquiz.repositories;

import android.content.Context;

import com.example.adminquiz.BuildConfig;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.models.AIQuestion;
import com.example.adminquiz.models.AITestResponse;
import com.example.adminquiz.models.AiConfig;
import com.example.adminquiz.network.GeminiModels;
import com.example.adminquiz.network.RetrofitClient;
import com.example.adminquiz.utils.AppExecutors;
import com.example.adminquiz.utils.NetworkUtils;
import com.example.adminquiz.utils.SecureStore;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Gemini integration. Never throws: every failure becomes a readable message. */
public class GeminiRepository {

    public interface ResultCallback {
        void onSuccess(long quizId);

        void onError(String message);
    }

    private final Context context;
    private final QuizRepository quizRepository;

    public GeminiRepository(Context context) {
        this.context = context.getApplicationContext();
        this.quizRepository = new QuizRepository(context);
    }

    public void generate(AiConfig cfg, long userId, ResultCallback cb) {
        if (!NetworkUtils.isOnline(context)) {
            cb.onError("AI tests require an internet connection.");
            return;
        }
        String key = new SecureStore(context).getGeminiKey();
        if (key.isEmpty()) {
            cb.onError("No Gemini API key set. Add it in Settings (or local.properties for development).");
            return;
        }

        GeminiModels.GeminiRequest req = new GeminiModels.GeminiRequest();
        GeminiModels.Content content = new GeminiModels.Content();
        content.role = "user";
        content.parts.add(new GeminiModels.Part(buildPrompt(cfg)));
        req.contents.add(content);

        RetrofitClient.gemini().generate(BuildConfig.GEMINI_MODEL, key, req)
                .enqueue(new Callback<GeminiModels.GeminiResponse>() {
                    @Override
                    public void onResponse(Call<GeminiModels.GeminiResponse> call,
                                           Response<GeminiModels.GeminiResponse> response) {
                        if (!response.isSuccessful()) {
                            cb.onError(httpMessage(response.code()));
                            return;
                        }
                        String text = extractText(response.body());
                        if (text == null || text.trim().isEmpty()) {
                            cb.onError("The AI returned an empty response.");
                            return;
                        }
                        AppExecutors.io(() -> handleText(text, cfg, userId, cb));
                    }

                    @Override
                    public void onFailure(Call<GeminiModels.GeminiResponse> call, Throwable t) {
                        if (t instanceof SocketTimeoutException) {
                            cb.onError("The request timed out. Please try again.");
                        } else if (t instanceof IOException) {
                            cb.onError("Network error. Check your connection.");
                        } else {
                            cb.onError("Unexpected error: " + t.getMessage());
                        }
                    }
                });
    }

    private void handleText(String raw, AiConfig cfg, long userId, ResultCallback cb) {
        try {
            AITestResponse parsed = parse(raw);
            String problem = validate(parsed, cfg.count, cfg.type);
            if (problem != null) {
                cb.onError(problem);
                return;
            }
            long id = quizRepository.saveQuiz(toQuiz(parsed, cfg, userId), toQuestions(parsed, cfg));
            cb.onSuccess(id);
        } catch (Exception e) {
            cb.onError("Invalid AI response. Please try again.");
        }
    }

    // ---------- pure helpers (unit-tested) ----------
    static String buildPrompt(AiConfig cfg) {
        String typeRule;
        if ("MCQ".equals(cfg.type)) {
            typeRule = "Every question must have exactly 4 options.";
        } else if ("True/False".equals(cfg.type)) {
            typeRule = "Every question must be True/False with options exactly [\"True\",\"False\"].";
        } else {
            typeRule = "Each question has either exactly 4 options (MCQ) or exactly 2 options [\"True\",\"False\"].";
        }
        StringBuilder p = new StringBuilder();
        p.append("Generate a quiz.\n\n");
        p.append("Category: ").append(cfg.category).append("\n");
        p.append("Difficulty: ").append(cfg.difficulty).append("\n");
        p.append("Questions: ").append(cfg.count).append("\n");
        p.append("Question Type: ").append(cfg.type).append("\n");
        if (cfg.instruction != null && !cfg.instruction.trim().isEmpty()) {
            p.append("Focus: ").append(cfg.instruction.trim()).append("\n");
        }
        p.append("\nRequirements:\n");
        p.append("1. Generate exactly ").append(cfg.count).append(" questions.\n");
        p.append("2. ").append(typeRule).append("\n");
        p.append("3. Only one option can be correct.\n");
        p.append("4. Avoid ambiguous questions.\n");
        p.append("5. Do not repeat questions.\n");
        p.append("6. Return valid JSON only, with no markdown.\n");
        p.append("7. Include a short explanation for every answer.\n");
        p.append("8. correctAnswer is the zero-based index of the correct option.\n\n");
        p.append("JSON format:\n");
        p.append("{\"title\":\"...\",\"category\":\"...\",\"difficulty\":\"...\",\"questions\":[");
        p.append("{\"question\":\"...\",\"options\":[\"...\",\"...\"],\"correctAnswer\":0,\"explanation\":\"...\"}]}");
        return p.toString();
    }

    static String extractText(GeminiModels.GeminiResponse r) {
        if (r == null || r.candidates == null || r.candidates.isEmpty()) {
            return null;
        }
        GeminiModels.Candidate c = r.candidates.get(0);
        if (c == null || c.content == null || c.content.parts == null || c.content.parts.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (GeminiModels.Part part : c.content.parts) {
            if (part != null && part.text != null) {
                sb.append(part.text);
            }
        }
        return sb.toString();
    }

    public static AITestResponse parse(String raw) {
        String t = raw.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (nl > 0 && end > nl) {
                t = t.substring(nl + 1, end).trim();
            }
        }
        int s = t.indexOf('{');
        int e = t.lastIndexOf('}');
        if (s >= 0 && e > s) {
            t = t.substring(s, e + 1);
        }
        return new Gson().fromJson(t, AITestResponse.class);
    }

    /** Returns null when valid, otherwise a user-facing message. */
    public static String validate(AITestResponse r, int expected, String type) {
        String invalid = "Invalid AI response. Please try again.";
        if (r == null || r.questions == null || r.questions.size() != expected) {
            return invalid;
        }
        for (AIQuestion q : r.questions) {
            if (q == null || isBlank(q.question) || q.options == null) {
                return invalid;
            }
            int n = q.options.size();
            boolean okCount;
            if ("MCQ".equals(type)) {
                okCount = n == 4;
            } else if ("True/False".equals(type)) {
                okCount = n == 2;
            } else {
                okCount = n == 4 || n == 2;
            }
            if (!okCount) {
                return invalid;
            }
            for (String o : q.options) {
                if (isBlank(o)) {
                    return invalid;
                }
            }
            if (q.correctAnswer < 0 || q.correctAnswer >= n) {
                return invalid;
            }
            if (isBlank(q.explanation)) {
                q.explanation = "No explanation provided.";
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String httpMessage(int code) {
        if (code == 400 || code == 401 || code == 403) {
            return "The Gemini API key was rejected. Check the key in Settings.";
        }
        if (code == 404) {
            return "Gemini model not found. Check GEMINI_MODEL in local.properties.";
        }
        if (code == 429) {
            return "Too many requests or quota reached. Try again later.";
        }
        if (code >= 500) {
            return "The AI service had a server error. Try again.";
        }
        return "Request failed (HTTP " + code + ").";
    }

    private static QuizEntity toQuiz(AITestResponse r, AiConfig cfg, long userId) {
        QuizEntity q = new QuizEntity();
        q.title = isBlank(r.title) ? cfg.category + " Test" : r.title.trim();
        q.category = cfg.category;
        q.difficulty = cfg.difficulty;
        q.description = "AI generated test" + (isBlank(cfg.instruction) ? "" : " - " + cfg.instruction.trim());
        q.creatorId = userId;
        q.published = false;
        q.source = "AI";
        q.timeLimitSec = cfg.timeMinutes * 60;
        return q;
    }

    private static List<QuestionEntity> toQuestions(AITestResponse r, AiConfig cfg) {
        List<QuestionEntity> list = new ArrayList<>();
        for (AIQuestion a : r.questions) {
            QuestionEntity q = new QuestionEntity();
            q.text = a.question.trim();
            q.optA = a.options.get(0).trim();
            q.optB = a.options.get(1).trim();
            q.optC = a.options.size() > 2 ? a.options.get(2).trim() : "";
            q.optD = a.options.size() > 3 ? a.options.get(3).trim() : "";
            q.correctIndex = a.correctAnswer;
            q.explanation = a.explanation;
            q.difficulty = cfg.difficulty;
            list.add(q);
        }
        return list;
    }
}
