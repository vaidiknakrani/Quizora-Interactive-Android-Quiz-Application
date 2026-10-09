package com.example.adminquiz.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.adminquiz.R;
import com.example.adminquiz.analytics.StreakCalculator;
import com.example.adminquiz.databinding.FragmentHomeBinding;
import com.example.adminquiz.databinding.ViewHomeCardBinding;
import com.example.adminquiz.utils.ClickAnimator;
import com.example.adminquiz.viewmodels.StreakViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private StreakViewModel streakVm;
    private TextView[] dayLabels;
    private TextView[] dayDots;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindCard(binding.cardGhost, R.drawable.ic_ghost, R.string.card_ghost_title,
                R.string.card_ghost_desc, "#7B2FF7", R.id.ghostMatches);
        bindCard(binding.cardCreator, R.drawable.ic_creator, R.string.card_creator_title,
                R.string.card_creator_desc, "#F7971E", R.id.myQuizzes);
        bindCard(binding.cardPersona, R.drawable.ic_persona, R.string.card_persona_title,
                R.string.card_persona_desc, "#E91E63", R.id.personas);
        bindCard(binding.cardAi, R.drawable.ic_ai, R.string.card_ai_title,
                R.string.card_ai_desc, "#00A8CC", R.id.aiTests);

        // Streak card (reads the same StreakViewModel as Activity & Streaks; opens that screen).
        dayLabels = new TextView[]{binding.tvDayLabel0, binding.tvDayLabel1, binding.tvDayLabel2,
                binding.tvDayLabel3, binding.tvDayLabel4, binding.tvDayLabel5, binding.tvDayLabel6};
        dayDots = new TextView[]{binding.dayDot0, binding.dayDot1, binding.dayDot2,
                binding.dayDot3, binding.dayDot4, binding.dayDot5, binding.dayDot6};
        ClickAnimator.attach(binding.streakCard);
        binding.streakCard.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.activityStreaks));

        streakVm = new ViewModelProvider(this).get(StreakViewModel.class);
        streakVm.stats.observe(getViewLifecycleOwner(), this::bindStreak);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (streakVm != null) {
            streakVm.refresh();
        }
    }

    private void bindStreak(@Nullable StreakCalculator.StreakStats s) {
        if (binding == null || s == null) {
            return;
        }
        binding.tvStreakCount.setText(getString(R.string.home_streak_days, s.currentStreak));
        binding.tvStreakUnit.setVisibility(View.VISIBLE);
        binding.tvStreakMeta.setText(getString(R.string.home_streak_best, s.maxStreak)
                + "  ·  " + getString(R.string.home_streak_active, s.activeDays));
        if (s.activeToday()) {
            binding.tvStreakHint.setText(R.string.home_streak_hint_done);
        } else if (s.currentStreak > 0) {
            binding.tvStreakHint.setText(R.string.home_streak_hint_todo);
        } else {
            binding.tvStreakHint.setText(R.string.home_streak_hint_start);
        }

        // Last 7 local days, oldest on the left and today on the right.
        TimeZone tz = TimeZone.getDefault();
        SimpleDateFormat dayName = new SimpleDateFormat("EEE", Locale.getDefault());
        dayName.setTimeZone(tz);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 7; i++) {
            int back = 6 - i;
            long epochDay = s.today - back;
            Calendar c = Calendar.getInstance(tz);
            c.setTimeInMillis(now);
            c.add(Calendar.DAY_OF_YEAR, -back);
            dayLabels[i].setText(dayName.format(c.getTime()));
            boolean active = s.days.containsKey(epochDay);
            TextView dot = dayDots[i];
            if (active) {
                dot.setBackgroundResource(R.drawable.bg_day_dot_active);
                dot.setText("\u2713");
            } else if (back == 0) {
                dot.setBackgroundResource(R.drawable.bg_day_dot_today);
                dot.setText("");
            } else {
                dot.setBackgroundResource(R.drawable.bg_day_dot);
                dot.setText("");
            }
        }
    }

    private void bindCard(ViewHomeCardBinding card, @DrawableRes int icon, @StringRes int title,
                          @StringRes int desc, String colorHex, @IdRes int destination) {
        card.cardIcon.setImageResource(icon);
        card.cardTitle.setText(title);
        card.cardDescription.setText(desc);
        card.cardBackground.setBackgroundColor(Color.parseColor(colorHex));

        ClickAnimator.attach(card.getRoot());
        card.getRoot().setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(destination));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
