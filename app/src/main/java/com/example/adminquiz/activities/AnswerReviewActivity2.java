package com.example.adminquiz.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adminquiz.adapters.AnswerReviewAdapter;
import com.example.adminquiz.databinding.ActivityAnswerReview2Binding;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.ResultViewModel;

public class AnswerReviewActivity2 extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityAnswerReview2Binding b = ActivityAnswerReview2Binding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtils.setupToolbar(this, b.appbar.toolbar, "Review Answers");

        AnswerReviewAdapter adapter = new AnswerReviewAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(this));
        b.recycler.setAdapter(adapter);

        ResultViewModel vm = new ViewModelProvider(this).get(ResultViewModel.class);
        vm.load(getIntent().getLongExtra(ResultActivity2.EXTRA_ATTEMPT_ID, 0));
        vm.data.observe(this, d -> {
            if (d != null) {
                adapter.submit(d.answers);
            }
        });
    }
}
