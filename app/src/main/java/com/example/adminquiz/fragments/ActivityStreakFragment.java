package com.example.adminquiz.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.adminquiz.R;
import com.example.adminquiz.analytics.StreakCalculator;
import com.example.adminquiz.analytics.StreakCalculator.DayActivity;
import com.example.adminquiz.analytics.StreakCalculator.StreakStats;
import com.example.adminquiz.databinding.FragmentActivityStreakBinding;
import com.example.adminquiz.viewmodels.StreakViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;

import java.util.Map;

/** LeetCode-inspired daily activity calendar, streak counters and milestone badges. */
public class ActivityStreakFragment extends Fragment {
    private static final int RECENT_DAYS = 7;

    private FragmentActivityStreakBinding b;
    private StreakViewModel vm;
    private StreakStats current;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentActivityStreakBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(StreakViewModel.class);

        View[] legend = {b.legend0, b.legend1, b.legend2, b.legend3, b.legend4};
        for (int i = 0; i < legend.length; i++) {
            legend[i].setBackgroundColor(b.heatmap.levelColor(i));
        }
        b.tvDayDetail.setText(R.string.st_tap_hint);
        b.heatmap.setOnDayClickListener((day, activity) -> showDay(day, activity));
        vm.stats.observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) {
            vm.refresh();
        }
    }

    private void showDay(long day, @Nullable DayActivity a) {
        if (b == null) {
            return;
        }
        String date = StreakCalculator.formatDay(day);
        b.tvDayDetail.setText(a == null || a.quizzes == 0
                ? getString(R.string.st_day_none, date)
                : getString(R.string.st_day_detail, date, a.quizzes, a.points, a.accuracyPct()));
    }

    private void render(@Nullable StreakStats s) {
        if (b == null || s == null) {
            return;
        }
        current = s;
        b.tvCurrent.setText(getString(R.string.st_days_unit, s.currentStreak));
        b.tvMax.setText(getString(R.string.st_days_unit, s.maxStreak));
        b.tvActiveDays.setText(String.valueOf(s.activeDays));
        b.tvTotalQuizzes.setText(String.valueOf(s.totalQuizzes));
        b.tvToday.setText(s.activeToday() ? getString(R.string.st_today_done)
                : s.currentStreak > 0 ? getString(R.string.st_today_todo_streak, s.currentStreak)
                : getString(R.string.st_today_todo_none));

        b.heatmap.setData(s.days, s.today);
        renderBadges(s);
        renderRecent(s);
    }

    private void renderBadges(StreakStats s) {
        b.badges.removeAllViews();
        for (int m : StreakCalculator.MILESTONES) {
            boolean earned = s.maxStreak >= m;
            Chip chip = new Chip(requireContext());
            chip.setText(getString(R.string.st_milestone_earned, m));
            chip.setCheckable(false);
            chip.setClickable(false);
            chip.setEnabled(earned);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                    MaterialColors.getColor(chip, earned
                            ? com.google.android.material.R.attr.colorSecondaryContainer
                            : com.google.android.material.R.attr.colorSurfaceVariant)));
            b.badges.addView(chip);
        }
        int next = StreakCalculator.nextMilestone(s.currentStreak);
        b.tvNext.setText(next == 0 ? getString(R.string.st_all_milestones)
                : getString(R.string.st_next_milestone, next, next - s.currentStreak));
    }

    private void renderRecent(StreakStats s) {
        b.recentList.removeAllViews();
        int shown = 0;
        for (Map.Entry<Long, DayActivity> e : s.days.descendingMap().entrySet()) {
            if (shown++ >= RECENT_DAYS) {
                break;
            }
            DayActivity a = e.getValue();
            TextView row = new TextView(requireContext());
            row.setText(getString(R.string.st_recent_row, StreakCalculator.formatDay(e.getKey()),
                    a.quizzes, a.points));
            row.setTextSize(14);
            row.setPadding(0, 8, 0, 8);
            row.setTextColor(MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurface));
            b.recentList.addView(row);
        }
        if (shown == 0) {
            b.tvRecentEmpty.setText(R.string.st_recent_empty);
            b.tvRecentEmpty.setVisibility(View.VISIBLE);
        } else {
            b.tvRecentEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        current = null;
        b = null;
    }
}
