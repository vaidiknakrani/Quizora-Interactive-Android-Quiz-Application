package com.example.adminquiz.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adminquiz.activities.CreateQuizActivity2;
import com.example.adminquiz.activities.QuizDetailActivity2;
import com.example.adminquiz.adapters.QuizAdapter;
import com.example.adminquiz.databinding.FragmentMyQuizzesBinding;
import com.example.adminquiz.entities.QuizEntity;
import com.example.adminquiz.utils.Categories;
import com.example.adminquiz.viewmodels.QuizViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

/** Browse published creator quizzes (tab 1) and manage your own (tab 2). */
public class MyQuizzesFragment extends Fragment {
    private FragmentMyQuizzesBinding b;
    private QuizViewModel vm;
    private QuizAdapter adapter;
    private List<QuizEntity> browse;
    private List<QuizEntity> mine;
    private int tab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentMyQuizzesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(QuizViewModel.class);
        adapter = new QuizAdapter(q -> {
            Intent i = new Intent(requireContext(), QuizDetailActivity2.class);
            i.putExtra(QuizDetailActivity2.EXTRA_QUIZ_ID, q.id);
            startActivity(i);
        });
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        b.spCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, Categories.filterCategories()));
        b.tabs.addTab(b.tabs.newTab().setText("Browse"));
        b.tabs.addTab(b.tabs.newTab().setText("My Quizzes"));
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                tab = t.getPosition();
                b.filterRow.setVisibility(tab == 0 ? View.VISIBLE : View.GONE);
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });

        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int c, int d) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int c, int d) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        b.spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        });

        vm.browseList.observe(getViewLifecycleOwner(), l -> {
            browse = l;
            refresh();
        });
        vm.mineList.observe(getViewLifecycleOwner(), l -> {
            mine = l;
            refresh();
        });
        b.fabCreate.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CreateQuizActivity2.class)));
    }

    private void applyFilter() {
        String q = b.etSearch.getText() == null ? "" : b.etSearch.getText().toString().trim();
        int pos = b.spCategory.getSelectedItemPosition();
        vm.setFilter(q, pos <= 0 ? "" : Categories.QUIZ_CATEGORIES[pos - 1]);
    }

    private void refresh() {
        List<QuizEntity> list = tab == 0 ? browse : mine;
        adapter.submitList(list);
        boolean empty = list == null || list.isEmpty();
        b.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        b.tvEmpty.setText(tab == 0 ? "No published quizzes found.\nTry another search, or create one!"
                : "You haven't created any quizzes yet.\nTap + to create your first.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
