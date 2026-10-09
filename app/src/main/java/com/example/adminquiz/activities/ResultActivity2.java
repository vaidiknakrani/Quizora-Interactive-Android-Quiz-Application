package com.example.adminquiz.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adminquiz.databinding.ActivityResult2Binding;
import com.example.adminquiz.entities.AttemptEntity;
import com.example.adminquiz.models.GhostItem;
import com.example.adminquiz.models.Persona;
import com.example.adminquiz.models.ResultData;
import com.example.adminquiz.utils.Fmt;
import com.example.adminquiz.utils.ResultCardGenerator;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.GhostViewModel;
import com.example.adminquiz.viewmodels.ResultViewModel;

public class ResultActivity2 extends AppCompatActivity {
    public static final String EXTRA_ATTEMPT_ID = "attempt_id";

    private ActivityResult2Binding b;
    private long attemptId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityResult2Binding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtils.setupToolbar(this, b.appbar.toolbar, "Result");

        attemptId = getIntent().getLongExtra(EXTRA_ATTEMPT_ID, 0);
        ResultViewModel vm = new ViewModelProvider(this).get(ResultViewModel.class);
        GhostViewModel ghostVm = new ViewModelProvider(this).get(GhostViewModel.class);
        vm.load(attemptId);
        vm.data.observe(this, this::render);

        b.btnChallenge.setOnClickListener(v -> ghostVm.createAndShare(attemptId));
        ghostVm.shareResult.observe(this, r -> {
            if (r == null) {
                return;
            }
            if (r.status == Resource.Status.SUCCESS) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, r.data);
                startActivity(Intent.createChooser(send, "Share challenge"));
            } else if (r.status == Resource.Status.ERROR) {
                UiUtils.toast(this, r.message);
            }
            ghostVm.consumeShare();
        });
        b.btnDone.setOnClickListener(v -> finish());
    }

    private void render(ResultData d) {
        if (d == null) {
            return;
        }
        AttemptEntity a = d.attempt;
        Persona p = Persona.fromName(a.persona);

        String headline = "QUIZ COMPLETE";
        if ("AI".equals(a.mode)) {
            headline = "AI TEST COMPLETE";
        }
        GhostItem g = d.ghost;
        if (g != null) {
            headline = g.won > 0 ? "YOU WON!" : (g.won == 0 ? "IT'S A DRAW" : "YOU LOST");
        }
        b.tvHeadline.setText(headline);
        b.tvScore.setText(a.score + " / " + a.total);
        b.tvAccuracy.setText("Accuracy: " + Fmt.pct(a.accuracy) + "%   \u2022   Time: " + Fmt.time(a.totalTimeMs));

        if (g != null) {
            b.tvGhost.setVisibility(View.VISIBLE);
            b.tvGhost.setText("Your Score: " + g.playerScore + "/" + g.total + "\nYour Time: "
                    + Fmt.seconds(g.playerTimeMs) + "\n\nGhost Score: " + g.ghostScore + "/" + g.total
                    + "\nGhost Time: " + Fmt.seconds(g.ghostTimeMs));
        } else {
            b.tvGhost.setVisibility(View.GONE);
        }

        b.cardPersona.setCardBackgroundColor(p.color());
        b.tvEmoji.setText(p.emoji);
        b.tvPersonaName.setText(p.title);
        b.tvTagline.setText("\u201C" + p.tagline + "\u201D");
        b.tvPersonaStats.setText("Score: " + a.overallPct + "%\nAccuracy: " + Fmt.pct(a.accuracy)
                + "%   Speed: " + a.speedPct + "%   Consistency: " + a.consistencyPct + "%");

        b.btnAnswers.setOnClickListener(v -> {
            Intent i = new Intent(this, AnswerReviewActivity2.class);
            i.putExtra(ResultActivity2.EXTRA_ATTEMPT_ID, a.id);
            startActivity(i);
        });
        b.btnShare.setOnClickListener(v -> {
            try {
                Bitmap bmp = ResultCardGenerator.create(p, a.overallPct, Fmt.pct(a.accuracy), a.speedPct, a.consistencyPct);
                ResultCardGenerator.share(this, bmp, "I got " + p.title + " " + p.emoji
                        + " on Quiz App! Scored " + a.score + "/" + a.total);
            } catch (Exception e) {
                UiUtils.toast(this, "Could not create the image");
            }
        });
        b.btnTryAgain.setOnClickListener(v -> {
            Intent i = new Intent(this, QuizActivity2.class);
            i.putExtra(QuizActivity2.EXTRA_QUIZ_ID, a.quizId);
            i.putExtra(QuizActivity2.EXTRA_MATCH_ID, a.ghostMatchId);
            startActivity(i);
            finish();
        });
    }
}
