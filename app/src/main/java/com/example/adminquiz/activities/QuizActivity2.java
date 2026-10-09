package com.example.adminquiz.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adminquiz.databinding.ActivityQuiz2Binding;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.models.GhostTracker;
import com.example.adminquiz.models.QuizSession;
import com.example.adminquiz.models.QuizTimer;
import com.example.adminquiz.utils.Fmt;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.PlayViewModel;

/** The single play screen used by Normal, Creator, AI and Ghost quizzes. */
public class QuizActivity2 extends AppCompatActivity {
    public static final String EXTRA_QUIZ_ID = "quiz_id";
    public static final String EXTRA_MATCH_ID = "match_id";

    private ActivityQuiz2Binding b;
    private PlayViewModel vm;
    private QuizTimer timer;
    private RadioButton[] radios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityQuiz2Binding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(PlayViewModel.class);
        radios = new RadioButton[]{b.opt0, b.opt1, b.opt2, b.opt3};

        long quizId = getIntent().getLongExtra(EXTRA_QUIZ_ID, 0);
        long matchId = getIntent().getLongExtra(EXTRA_MATCH_ID, 0);
        vm.load(quizId, matchId);

        for (int i = 0; i < radios.length; i++) {
            final int index = i;
            radios[i].setOnClickListener(v -> {
                vm.getSession().select(index);
                updateLive();
            });
        }
        b.btnPrev.setOnClickListener(v -> move(-1));
        b.btnNext.setOnClickListener(v -> move(1));
        b.btnSubmit.setOnClickListener(v -> confirmSubmit());

        vm.sessionState.observe(this, r -> {
            if (r == null) {
                return;
            }
            b.progress.setVisibility(r.status == Resource.Status.LOADING ? View.VISIBLE : View.GONE);
            if (r.status == Resource.Status.ERROR) {
                UiUtils.toast(this, r.message);
                finish();
            } else if (r.status == Resource.Status.SUCCESS) {
                startQuiz(r.data);
            }
        });

        vm.finished.observe(this, attemptId -> {
            if (attemptId == null) {
                return;
            }
            Intent i = new Intent(this, ResultActivity2.class);
            i.putExtra(ResultActivity2.EXTRA_ATTEMPT_ID, attemptId.longValue());
            startActivity(i);
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(QuizActivity2.this)
                        .setTitle("Quit quiz?")
                        .setMessage("Your progress will be lost.")
                        .setPositiveButton("Quit", (d, w) -> finish())
                        .setNegativeButton("Keep playing", null)
                        .show();
            }
        });
    }

    private void startQuiz(QuizSession s) {
        b.content.setVisibility(View.VISIBLE);
        b.tvQuizTitle.setText(s.quiz.title);
        b.ghostPanel.setVisibility(vm.isGhostMode() ? View.VISIBLE : View.GONE);
        s.begin();
        if (timer != null) {
            timer.stop();
        }
        timer = new QuizTimer(s, new QuizTimer.Listener() {
            @Override
            public void onTick() {
                updateLive();
            }

            @Override
            public void onTimeUp() {
                UiUtils.toast(QuizActivity2.this, "Time is up!");
                submit();
            }
        });
        timer.start();
        render();
    }

    private void render() {
        QuizSession s = vm.getSession();
        QuestionEntity q = s.getCurrentQuestion();
        int idx = s.getCurrentIndex();
        b.tvCounter.setText("Question " + (idx + 1) + " / " + s.size());
        b.progressBar.setMax(s.size());
        b.progressBar.setProgress(idx + 1);
        b.tvQuestion.setText(q.text);

        b.radioGroup.clearCheck();
        int count = q.optionCount();
        for (int i = 0; i < radios.length; i++) {
            radios[i].setVisibility(i < count ? View.VISIBLE : View.GONE);
            radios[i].setText(q.option(i));
        }
        int sel = s.getSelected(idx);
        if (sel >= 0 && sel < radios.length) {
            radios[sel].setChecked(true);
        }
        boolean last = idx == s.size() - 1;
        b.btnPrev.setEnabled(idx > 0);
        b.btnNext.setVisibility(last ? View.GONE : View.VISIBLE);
        b.btnSubmit.setVisibility(last ? View.VISIBLE : View.GONE);
        updateLive();
    }

    private void updateLive() {
        QuizSession s = vm.getSession();
        if (s == null) {
            return;
        }
        long elapsed = s.getElapsedMs();
        b.tvTimer.setText(s.getTimeLimitMs() > 0
                ? "\u23F1 " + Fmt.time(s.getRemainingMs()) : "\u23F1 " + Fmt.time(elapsed));
        GhostTracker g = vm.getGhost();
        if (g != null) {
            b.tvGhostScores.setText("YOUR SCORE: " + s.correctSoFar() + "      GHOST SCORE: " + g.scoreAt(elapsed));
            b.tvGhostTimes.setText("YOUR TIME: " + Fmt.seconds(elapsed) + "      GHOST TIME: "
                    + Fmt.seconds(Math.min(elapsed, g.totalTimeMs())));
        }
    }

    private void move(int delta) {
        QuizSession s = vm.getSession();
        s.goTo(s.getCurrentIndex() + delta);
        render();
    }

    private void confirmSubmit() {
        int unanswered = vm.getSession().size() - vm.getSession().answeredCount();
        if (unanswered == 0) {
            submit();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Submit quiz?")
                .setMessage("You have " + unanswered + " unanswered question(s).")
                .setPositiveButton("Submit", (d, w) -> submit())
                .setNegativeButton("Review", null)
                .show();
    }

    private void submit() {
        if (timer != null) {
            timer.stop();
        }
        b.btnSubmit.setEnabled(false);
        vm.submit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.stop();
        }
    }
}
