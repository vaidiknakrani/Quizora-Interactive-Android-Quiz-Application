package com.example.adminquiz.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adminquiz.R;
import com.example.adminquiz.activities.ResultActivity2;
import com.example.adminquiz.adapters.AttemptAdapter;
import com.example.adminquiz.databinding.FragmentProfileBinding;
import com.example.adminquiz.models.Achievement;
import com.example.adminquiz.models.ProfileData;
import com.example.adminquiz.models.UserStats;
import com.example.adminquiz.utils.Fmt;
import com.example.adminquiz.viewmodels.StatsViewModel;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;
    private StatsViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentProfileBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(StatsViewModel.class);
        AttemptAdapter adapter = new AttemptAdapter(a -> {
            Intent i = new Intent(requireContext(), ResultActivity2.class);
            i.putExtra(ResultActivity2.EXTRA_ATTEMPT_ID, a.id);
            startActivity(i);
        });
        b.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recyclerHistory.setAdapter(adapter);
        b.recyclerHistory.setNestedScrollingEnabled(false);
        vm.history.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            b.tvHistoryEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
        vm.profile.observe(getViewLifecycleOwner(), this::bind);
        b.btnPersonas.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.personas));
    }

    @Override
    public void onResume() {
        super.onResume();
        vm.refreshProfile();
    }

    private void bind(ProfileData p) {
        if (p == null || b == null) {
            return;
        }
        UserStats s = p.stats;
        b.tvUsername.setText(p.username);
        b.tvEmail.setText(p.email);
        b.tvStats.setText("Total Quizzes: " + s.quizzes
                + "\nTotal Score: " + s.totalScore
                + "\nAverage Accuracy: " + Fmt.pct(s.avgAccuracy) + "%"
                + "\nBest Score: " + s.bestScore
                + "\nCurrent Streak: " + p.streakDays + " day(s)"
                + "\nGhost Wins: " + s.ghostWins
                + "\nQuizzes Created: " + p.quizzesCreated
                + "\nAI Tests Completed: " + s.aiTests);
        StringBuilder sb = new StringBuilder();
        for (Achievement a : p.achievements) {
            sb.append(a.unlocked ? "\u2705 " : "\uD83D\uDD12 ").append(a.title)
                    .append(" - ").append(a.description).append('\n');
        }
        b.tvAchievements.setText(sb.toString().trim());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
