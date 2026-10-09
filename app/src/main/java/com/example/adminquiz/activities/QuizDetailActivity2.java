package com.example.adminquiz.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adminquiz.adapters.MessageAdapter;
import com.example.adminquiz.databinding.ActivityQuizDetail2Binding;
import com.example.adminquiz.databinding.DialogReviewBinding;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.QuizViewModel;

public class QuizDetailActivity2 extends AppCompatActivity {
    public static final String EXTRA_QUIZ_ID = "quiz_id";

    private ActivityQuizDetail2Binding b;
    private QuizViewModel vm;
    private long quizId;
    private QuizEntity quiz;
    private int questionCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityQuizDetail2Binding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtils.setupToolbar(this, b.appbar.toolbar, "Quiz");
        vm = new ViewModelProvider(this).get(QuizViewModel.class);
        quizId = getIntent().getLongExtra(EXTRA_QUIZ_ID, 0);

        MessageAdapter messages = new MessageAdapter(m -> showReplyDialog(m.id));
        b.recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        b.recyclerMessages.setAdapter(messages);

        vm.observeQuiz(quizId).observe(this, q -> {
            if (q == null) {
                finish();
                return;
            }
            quiz = q;
            bindQuiz(messages);
        });
        vm.questionCount(quizId).observe(this, n -> {
            questionCount = n == null ? 0 : n;
            b.tvQuestionCount.setText(questionCount + " questions");
            b.btnStart.setEnabled(questionCount > 0);
        });
        vm.messages(quizId).observe(this, list -> {
            messages.submit(list);
            b.tvNoMessages.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.reviews(quizId).observe(this, list -> {
            b.reviewsContainer.removeAllViews();
            if (list == null) {
                return;
            }
            for (com.example.adminquiz.entities.ReviewEntity r : list) {
                TextView t = new TextView(this);
                String stars = new String(new char[Math.max(0, Math.min(5, r.rating))]).replace("\0", "\u2605");
                t.setText(stars + "  " + r.username + (r.comment == null || r.comment.isEmpty() ? "" : ": " + r.comment));
                t.setPadding(0, 6, 0, 6);
                b.reviewsContainer.addView(t);
            }
        });

        b.btnStart.setOnClickListener(v -> {
            Intent i = new Intent(this, QuizActivity2.class);
            i.putExtra(QuizActivity2.EXTRA_QUIZ_ID, quizId);
            startActivity(i);
        });
        b.btnSend.setOnClickListener(v -> {
            String text = UiUtils.text(b.etMessage);
            if (text.isEmpty()) {
                return;
            }
            vm.sendMessage(quizId, text);
            b.etMessage.setText("");
        });
        b.btnRate.setOnClickListener(v -> showReviewDialog());
        b.btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateQuizActivity2.class);
            i.putExtra(CreateQuizActivity2.EXTRA_QUIZ_ID, quizId);
            startActivity(i);
        });
        b.btnPublish.setOnClickListener(v -> {
            if (quiz != null) {
                vm.setPublished(quizId, !quiz.published);
            }
        });
        b.btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete quiz?")
                .setMessage("This also removes its questions, ghosts and messages.")
                .setPositiveButton("Delete", (d, w) -> vm.deleteQuiz(quizId))
                .setNegativeButton("Cancel", null)
                .show());
        vm.deleted.observe(this, del -> {
            if (Boolean.TRUE.equals(del)) {
                finish();
            }
        });
    }

    private void bindQuiz(MessageAdapter messages) {
        b.tvTitle.setText(quiz.title);
        b.tvInfo.setText(quiz.category + " \u2022 " + quiz.difficulty + " \u2022 by " + quiz.creatorName
                + (quiz.timeLimitSec > 0 ? " \u2022 " + (quiz.timeLimitSec / 60) + " min" : ""));
        b.tvDescription.setText(quiz.description == null || quiz.description.isEmpty()
                ? "No description." : quiz.description);
        b.ratingBar.setRating(quiz.ratingAvg);
        b.tvRating.setText(quiz.ratingCount == 0 ? "No ratings yet"
                : String.format(java.util.Locale.US, "%.1f (%d)", quiz.ratingAvg, quiz.ratingCount));
        boolean owner = quiz.creatorId == vm.getUserId();
        b.ownerRow.setVisibility(owner ? View.VISIBLE : View.GONE);
        b.btnPublish.setText(quiz.published ? "Unpublish" : "Publish");
        messages.setOwner(owner);
    }

    private void showReviewDialog() {
        DialogReviewBinding d = DialogReviewBinding.inflate(LayoutInflater.from(this));
        new AlertDialog.Builder(this)
                .setTitle("Rate this quiz")
                .setView(d.getRoot())
                .setPositiveButton("Submit", (dlg, w) -> {
                    int rating = Math.round(d.ratingInput.getRating());
                    if (rating < 1) {
                        UiUtils.toast(this, "Choose at least 1 star");
                        return;
                    }
                    vm.submitReview(quizId, rating, UiUtils.text(d.etComment));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReplyDialog(long messageId) {
        EditText input = new EditText(this);
        input.setHint("Your answer");
        new AlertDialog.Builder(this)
                .setTitle("Reply to learner")
                .setView(input)
                .setPositiveButton("Send", (dlg, w) -> {
                    String t = UiUtils.text(input);
                    if (!t.isEmpty()) {
                        vm.reply(messageId, t);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
