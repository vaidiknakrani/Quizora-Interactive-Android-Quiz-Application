package com.example.adminquiz.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adminquiz.databinding.ActivityCreateQuiz2Binding;
import com.example.adminquiz.databinding.ItemQuestionEditorBinding;
import com.example.adminquiz.entities.QuestionEntity;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.utils.Categories;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.QuizViewModel;

import java.util.ArrayList;
import java.util.List;

/** Create or edit a quiz. Reached from the drawer, the Browse screen, or the detail screen. */
public class CreateQuizActivity2 extends AppCompatActivity {
    public static final String EXTRA_QUIZ_ID = "quiz_id";

    private ActivityCreateQuiz2Binding b;
    private QuizViewModel vm;
    private final List<ItemQuestionEditorBinding> editors = new ArrayList<>();
    private QuizEntity editing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCreateQuiz2Binding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtils.setupToolbar(this, b.appbar.toolbar, "Create Quiz");
        vm = new ViewModelProvider(this).get(QuizViewModel.class);

        b.etCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
                Categories.QUIZ_CATEGORIES));
        b.etCategory.setOnClickListener(v -> b.etCategory.showDropDown());
        b.spDifficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Categories.DIFFICULTIES));
        b.container.setSaveFromParentEnabled(false);

        long id = getIntent().getLongExtra(EXTRA_QUIZ_ID, 0);
        if (id > 0) {
            b.appbar.toolbar.setTitle("Edit Quiz");
            vm.loadForEdit(id);
        } else {
            addEditor(null);
        }
        vm.editData.observe(this, d -> {
            if (d == null || d.quiz == null || editing != null) {
                return;
            }
            editing = d.quiz;
            b.etTitle.setText(d.quiz.title);
            b.etCategory.setText(d.quiz.category, false);
            b.etDescription.setText(d.quiz.description);
            b.etTimeLimit.setText(d.quiz.timeLimitSec > 0 ? String.valueOf(d.quiz.timeLimitSec / 60) : "");
            for (int i = 0; i < Categories.DIFFICULTIES.length; i++) {
                if (Categories.DIFFICULTIES[i].equals(d.quiz.difficulty)) {
                    b.spDifficulty.setSelection(i);
                }
            }
            for (QuestionEntity q : d.questions) {
                addEditor(q);
            }
        });

        b.btnAddQuestion.setOnClickListener(v -> addEditor(null));
        b.btnSaveDraft.setOnClickListener(v -> save(false));
        b.btnPublish.setOnClickListener(v -> save(true));

        vm.saveResult.observe(this, r -> {
            if (r == null) {
                return;
            }
            b.progress.setVisibility(r.status == Resource.Status.LOADING ? View.VISIBLE : View.GONE);
            if (r.status == Resource.Status.ERROR) {
                UiUtils.toast(this, r.message);
            } else if (r.status == Resource.Status.SUCCESS) {
                UiUtils.toast(this, "Quiz saved");
                finish();
            }
        });
    }

    private void addEditor(QuestionEntity q) {
        ItemQuestionEditorBinding e = ItemQuestionEditorBinding.inflate(getLayoutInflater(), b.container, false);
        e.spDifficulty.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Categories.DIFFICULTIES));
        if (q != null) {
            e.etQuestion.setText(q.text);
            e.etA.setText(q.optA);
            e.etB.setText(q.optB);
            e.etC.setText(q.optC);
            e.etD.setText(q.optD);
            e.etExplanation.setText(q.explanation);
            ((RadioButton) e.radioCorrect.getChildAt(Math.max(0, Math.min(3, q.correctIndex)))).setChecked(true);
            for (int i = 0; i < Categories.DIFFICULTIES.length; i++) {
                if (Categories.DIFFICULTIES[i].equals(q.difficulty)) {
                    e.spDifficulty.setSelection(i);
                }
            }
        }
        e.btnRemove.setOnClickListener(v -> {
            if (editors.size() <= 1) {
                UiUtils.toast(this, "A quiz needs at least one question");
                return;
            }
            b.container.removeView(e.getRoot());
            editors.remove(e);
            renumber();
        });
        editors.add(e);
        b.container.addView(e.getRoot());
        renumber();
    }

    private void renumber() {
        for (int i = 0; i < editors.size(); i++) {
            editors.get(i).tvNumber.setText("Question " + (i + 1));
        }
    }

    private void save(boolean publish) {
        String title = UiUtils.text(b.etTitle);
        String category = b.etCategory.getText() == null ? "" : b.etCategory.getText().toString().trim();
        if (title.isEmpty()) {
            b.etTitle.setError("Title is required");
            return;
        }
        if (category.isEmpty()) {
            b.etCategory.setError("Category is required");
            return;
        }
        List<QuestionEntity> questions = new ArrayList<>();
        for (int i = 0; i < editors.size(); i++) {
            ItemQuestionEditorBinding e = editors.get(i);
            QuestionEntity q = new QuestionEntity();
            q.text = UiUtils.text(e.etQuestion);
            q.optA = UiUtils.text(e.etA);
            q.optB = UiUtils.text(e.etB);
            q.optC = UiUtils.text(e.etC);
            q.optD = UiUtils.text(e.etD);
            q.explanation = UiUtils.text(e.etExplanation);
            q.difficulty = (String) e.spDifficulty.getSelectedItem();
            int checked = e.radioCorrect.getCheckedRadioButtonId();
            q.correctIndex = checked == e.rbA.getId() ? 0 : checked == e.rbB.getId() ? 1
                    : checked == e.rbC.getId() ? 2 : checked == e.rbD.getId() ? 3 : -1;
            if (q.text.isEmpty() || q.optA.isEmpty() || q.optB.isEmpty() || q.optC.isEmpty() || q.optD.isEmpty()) {
                UiUtils.toast(this, "Question " + (i + 1) + ": fill the question and all 4 options");
                return;
            }
            if (q.correctIndex < 0) {
                UiUtils.toast(this, "Question " + (i + 1) + ": select the correct answer");
                return;
            }
            questions.add(q);
        }

        QuizEntity quiz = editing != null ? editing : new QuizEntity();
        quiz.title = title;
        quiz.category = category;
        quiz.description = UiUtils.text(b.etDescription);
        quiz.difficulty = (String) b.spDifficulty.getSelectedItem();
        quiz.published = publish;
        int minutes = 0;
        try {
            minutes = Integer.parseInt(UiUtils.text(b.etTimeLimit));
        } catch (NumberFormatException ignored) {
            // blank means no time limit
        }
        quiz.timeLimitSec = Math.max(0, minutes) * 60;
        if (editing == null) {
            quiz.creatorId = vm.getUserId();
            quiz.source = "USER";
        }
        vm.saveQuiz(quiz, questions);
    }
}
