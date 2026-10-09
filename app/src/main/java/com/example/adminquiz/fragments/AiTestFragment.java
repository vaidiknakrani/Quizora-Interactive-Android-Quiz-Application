package com.example.adminquiz.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adminquiz.activities.QuizActivity2;
import com.example.adminquiz.adapters.QuizAdapter;
import com.example.adminquiz.databinding.FragmentAiTestBinding;
import com.example.adminquiz.models.AiConfig;
import com.example.adminquiz.utils.Categories;
import com.example.adminquiz.utils.NetworkUtils;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.AiViewModel;

public class AiTestFragment extends Fragment {
    private FragmentAiTestBinding b;
    private AiViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentAiTestBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(AiViewModel.class);

        fill(b.spCategory, Categories.aiCategories());
        fill(b.spDifficulty, Categories.DIFFICULTIES);
        fill(b.spCount, Categories.AI_COUNTS);
        fill(b.spType, Categories.AI_TYPES);
        fill(b.spTime, Categories.AI_TIMES);
        b.spCount.setSelection(1);
        b.spTime.setSelection(1);
        b.spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean custom = Categories.CUSTOM.equals(p.getItemAtPosition(pos));
                b.tilCustom.setVisibility(custom ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> p) {
            }
        });

        QuizAdapter adapter = new QuizAdapter(q -> {
            Intent i = new Intent(requireContext(), QuizActivity2.class);
            i.putExtra(QuizActivity2.EXTRA_QUIZ_ID, q.id);
            startActivity(i);
        });
        b.recyclerRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recyclerRecent.setAdapter(adapter);
        vm.recentTests.observe(getViewLifecycleOwner(), l -> {
            adapter.submitList(l);
            b.tvRecentEmpty.setVisibility(l == null || l.isEmpty() ? View.VISIBLE : View.GONE);
        });

        b.btnGenerate.setOnClickListener(v -> generate());
        b.btnRetry.setOnClickListener(v -> generate());

        vm.result.observe(getViewLifecycleOwner(), r -> {
            if (r == null) {
                return;
            }
            boolean loading = r.status == Resource.Status.LOADING;
            b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.btnGenerate.setEnabled(!loading);
            boolean error = r.status == Resource.Status.ERROR;
            b.errorBox.setVisibility(error ? View.VISIBLE : View.GONE);
            if (error) {
                b.tvError.setText(r.message);
            }
            if (r.status == Resource.Status.SUCCESS) {
                Intent i = new Intent(requireContext(), QuizActivity2.class);
                i.putExtra(QuizActivity2.EXTRA_QUIZ_ID, r.data.longValue());
                startActivity(i);
                vm.consume();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (b != null) {
            b.tvOffline.setVisibility(NetworkUtils.isOnline(requireContext()) ? View.GONE : View.VISIBLE);
        }
    }

    private void fill(Spinner s, String[] items) {
        s.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, items));
    }

    private void generate() {
        AiConfig c = new AiConfig();
        String cat = (String) b.spCategory.getSelectedItem();
        if (Categories.CUSTOM.equals(cat)) {
            cat = UiUtils.text(b.etCustomCategory);
            if (cat.isEmpty()) {
                b.etCustomCategory.setError("Enter a category");
                return;
            }
        }
        c.category = cat;
        c.difficulty = (String) b.spDifficulty.getSelectedItem();
        c.count = Integer.parseInt((String) b.spCount.getSelectedItem());
        c.type = (String) b.spType.getSelectedItem();
        int t = b.spTime.getSelectedItemPosition();
        c.timeMinutes = t == 0 ? 5 : t == 1 ? 10 : t == 2 ? 15 : 0;
        c.instruction = UiUtils.text(b.etInstruction);
        vm.generate(c);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
