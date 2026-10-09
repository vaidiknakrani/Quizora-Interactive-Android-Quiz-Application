package com.example.adminquiz;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.models.AIQuestion;
import com.example.adminquiz.models.AITestResponse;
import com.example.adminquiz.models.GhostComparison;
import com.example.adminquiz.models.GhostStep;
import com.example.adminquiz.models.GhostTracker;
import com.example.adminquiz.models.Persona;
import com.example.adminquiz.models.PersonaCalculator;
import com.example.adminquiz.models.QuizEvaluator;
import com.example.adminquiz.models.QuizResult;
import com.example.adminquiz.models.QuizSession;
import com.example.adminquiz.repositories.GeminiRepository;
import com.example.adminquiz.repositories.StatsRepository;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuizEngineTest {

    private QuizSession session(int questions) {
        QuizEntity quiz = new QuizEntity();
        quiz.title = "T";
        List<QuestionEntity> list = new ArrayList<>();
        for (int i = 0; i < questions; i++) {
            QuestionEntity q = new QuestionEntity();
            q.text = "Q" + i;
            q.optA = "a";
            q.optB = "b";
            q.optC = "c";
            q.optD = "d";
            q.correctIndex = 1;
            q.difficulty = "Medium";
            list.add(q);
        }
        QuizSession s = new QuizSession(quiz, list);
        s.begin();
        return s;
    }

    @Test
    public void scoreCountsOnlyCorrectAnswers() {
        QuizSession s = session(4);
        for (int i = 0; i < 4; i++) {
            s.select(i < 3 ? 1 : 0);
            s.goTo(i + 1);
        }
        QuizResult r = QuizEvaluator.evaluate(s);
        assertEquals(3, r.score);
        assertEquals(4, r.total);
        assertEquals(0.75, r.accuracy, 0.0001);
        assertEquals(3, r.longestStreak);
    }

    @Test
    public void perfectRunIsPerfectionist() {
        QuizSession s = session(5);
        for (int i = 0; i < 5; i++) {
            s.select(1);
            s.goTo(i + 1);
        }
        assertEquals(Persona.PERFECTIONIST, QuizEvaluator.evaluate(s).persona);
    }

    @Test
    public void unansweredQuestionsAreWrong() {
        QuizResult r = QuizEvaluator.evaluate(session(3));
        assertEquals(0, r.score);
        assertEquals(-1, r.answers.get(0).selectedIndex);
    }

    @Test
    public void comebackKingNeedsStrongSecondHalf() {
        assertEquals(Persona.COMEBACK_KING,
                PersonaCalculator.pick(0.7, 0.2, 0.5, 0.25, 1.0, 2, 8));
    }

    @Test
    public void ghostWinnerRules() {
        assertEquals(1, GhostComparison.compare(9, 48000, 8, 52000));
        assertEquals(-1, GhostComparison.compare(7, 10000, 8, 52000));
        assertEquals(1, GhostComparison.compare(8, 40000, 8, 52000));
        assertEquals(-1, GhostComparison.compare(8, 60000, 8, 52000));
        assertEquals(0, GhostComparison.compare(8, 52000, 8, 52000));
    }

    @Test
    public void ghostTrackerReplaysAgainstClock() {
        GhostTracker g = new GhostTracker(Arrays.asList(
                new GhostStep(true, 1000), new GhostStep(false, 2000), new GhostStep(true, 1000)));
        assertEquals(0, g.scoreAt(500));
        assertEquals(1, g.scoreAt(1000));
        assertEquals(1, g.scoreAt(3000));
        assertEquals(2, g.scoreAt(4000));
        assertEquals(4000, g.totalTimeMs());
    }

    private AITestResponse ai(int n, int options, int correct) {
        AITestResponse r = new AITestResponse();
        r.questions = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            AIQuestion q = new AIQuestion();
            q.question = "Q" + i;
            q.options = new ArrayList<>();
            for (int o = 0; o < options; o++) {
                q.options.add("opt" + o);
            }
            q.correctAnswer = correct;
            q.explanation = "because";
            r.questions.add(q);
        }
        return r;
    }

    @Test
    public void validAiResponsePasses() {
        assertNull(GeminiRepository.validate(ai(5, 4, 1), 5, "MCQ"));
        assertNull(GeminiRepository.validate(ai(5, 2, 0), 5, "True/False"));
    }

    @Test
    public void invalidAiResponsesAreRejected() {
        assertNotNull(GeminiRepository.validate(ai(4, 4, 1), 5, "MCQ"));
        assertNotNull(GeminiRepository.validate(ai(5, 3, 1), 5, "MCQ"));
        assertNotNull(GeminiRepository.validate(ai(5, 4, 4), 5, "MCQ"));
        assertNotNull(GeminiRepository.validate(null, 5, "MCQ"));
    }

    @Test
    public void parserStripsMarkdownFences() {
        String raw = "```json\n{\"title\":\"T\",\"questions\":[{\"question\":\"Q\",\"options\":[\"a\",\"b\"],"
                + "\"correctAnswer\":1,\"explanation\":\"e\"}]}\n```";
        AITestResponse r = GeminiRepository.parse(raw);
        assertEquals("T", r.title);
        assertEquals(1, r.questions.size());
        assertEquals(1, r.questions.get(0).correctAnswer);
    }

    @Test
    public void streakCountsConsecutiveDays() {
        long day = 24L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        List<Long> times = Arrays.asList(now, now - day, now - 2 * day, now - 5 * day);
        assertEquals(3, StatsRepository.computeStreak(times, now));
        assertTrue(StatsRepository.computeStreak(new ArrayList<Long>(), now) == 0);
    }
}
