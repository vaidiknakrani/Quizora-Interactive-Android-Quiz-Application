package com.example.adminquiz.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adminquiz.R;
import com.example.adminquiz.adapters.ClassRankAdapter;
import com.example.adminquiz.analytics.LeaderboardRanker;
import com.example.adminquiz.analytics.LeaderboardRanker.Mode;
import com.example.adminquiz.analytics.LeaderboardRanker.Period;
import com.example.adminquiz.analytics.LeaderboardRanker.RankedEntry;
import com.example.adminquiz.databinding.FragmentClassLeaderboardBinding;
import com.example.adminquiz.viewmodels.ClassLeaderboardViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Whole-class ranking with Daily / Weekly / Monthly / All-time filters. */
public class ClassLeaderboardFragment extends Fragment {
    private static final int VISIBLE_ROWS = 25;

    private FragmentClassLeaderboardBinding b;
    private ClassLeaderboardViewModel vm;
    private ClassRankAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentClassLeaderboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(ClassLeaderboardViewModel.class);
        adapter = new ClassRankAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        b.chipsPeriod.check(chipForPeriod(vm.getPeriod()));
        b.chipsMode.check(vm.getMode() == Mode.POINTS ? R.id.chipPoints : R.id.chipAccuracy);
        b.chipsPeriod.setOnCheckedStateChangeListener((group, ids) -> {
            if (!ids.isEmpty()) {
                vm.setPeriod(periodForChip(ids.get(0)));
            }
        });
        b.chipsMode.setOnCheckedStateChangeListener((group, ids) -> {
            if (!ids.isEmpty()) {
                vm.setMode(ids.get(0) == R.id.chipPoints ? Mode.POINTS : Mode.ACCURACY);
            }
        });
        b.btnHow.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lb_how_title)
                .setMessage(LeaderboardRanker.explain(vm.getMode(), vm.getPeriod()))
                .setPositiveButton(R.string.lb_ok, null)
                .show());

        b.progress.setVisibility(View.VISIBLE);
        vm.board.observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) {
            vm.refresh();
        }
    }

    private static int chipForPeriod(Period p) {
        switch (p) {
            case DAILY: return R.id.chipToday;
            case MONTHLY: return R.id.chipMonth;
            case ALL_TIME: return R.id.chipAll;
            default: return R.id.chipWeek;
        }
    }

    private static Period periodForChip(int id) {
        if (id == R.id.chipToday) {
            return Period.DAILY;
        }
        if (id == R.id.chipMonth) {
            return Period.MONTHLY;
        }
        if (id == R.id.chipAll) {
            return Period.ALL_TIME;
        }
        return Period.WEEKLY;
    }

    private void render(@Nullable LeaderboardRanker.Result r) {
        if (b == null) {
            return;
        }
        b.progress.setVisibility(r == null ? View.VISIBLE : View.GONE);
        if (r == null) {
            return;
        }
        b.tvNote.setText(getString(R.string.local_note,
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(r.computedAt)))
                + " " + getString(R.string.local_scope_note));

        // Visible list: ranked students only (unranked ones are explained in the "Your rank" card).
        List<RankedEntry> visible = new ArrayList<>();
        for (RankedEntry e : r.entries) {
            if (e.ranked && visible.size() < VISIBLE_ROWS) {
                visible.add(e);
            }
        }
        adapter.submit(visible, r.mode);
        b.tvEmpty.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        b.tvAlone.setVisibility(r.participants == 1 && r.me != null ? View.VISIBLE : View.GONE);

        RankedEntry me = r.me;
        if (me == null) {
            b.cardMe.setVisibility(View.VISIBLE);
            b.tvMeRank.setText("-");
            b.tvMeDetail.setText(R.string.lb_unranked_none);
        } else if (!me.ranked) {
            b.cardMe.setVisibility(View.VISIBLE);
            b.tvMeRank.setText("-");
            b.tvMeDetail.setText(getString(R.string.lb_unranked_accuracy, r.minQuizzes, me.quizzes));
        } else {
            b.cardMe.setVisibility(View.VISIBLE);
            b.tvMeRank.setText(getString(R.string.lb_your_rank_value, me.rank, r.rankedCount));
            b.tvMeDetail.setText(getString(R.string.lb_points_value, me.points) + " · "
                    + getString(R.string.lb_row_meta, me.quizzes, me.accuracyPct()));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
