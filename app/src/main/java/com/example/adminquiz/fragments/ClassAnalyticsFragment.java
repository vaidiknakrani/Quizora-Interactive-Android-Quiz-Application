package com.example.adminquiz.fragments;

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

import com.example.adminquiz.R;
import com.example.adminquiz.analytics.ClassAnalytics;
import com.example.adminquiz.analytics.ClassAnalytics.Group;
import com.example.adminquiz.analytics.ClassAnalytics.Overview;
import com.example.adminquiz.analytics.ClassAnalytics.Report;
import com.example.adminquiz.analytics.ClassAnalytics.StudentSummary;
import com.example.adminquiz.databinding.FragmentClassAnalyticsBinding;
import com.example.adminquiz.utils.Fmt;
import com.example.adminquiz.viewmodels.ClassAnalyticsViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Class overview, charts and "you vs the class" for the selected range, subject and quiz. */
public class ClassAnalyticsFragment extends Fragment {
    private FragmentClassAnalyticsBinding b;
    private ClassAnalyticsViewModel vm;

    // Spinner state. Programmatic changes must not be mistaken for user selections.
    private final List<String> subjectValues = new ArrayList<>();
    private final List<Long> quizValues = new ArrayList<>();
    private boolean updatingSpinners;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentClassAnalyticsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(ClassAnalyticsViewModel.class);

        b.chipsRange.check(chipForRange(vm.getFilter().rangeDays));
        b.chipsRange.setOnCheckedStateChangeListener((group, ids) -> {
            if (!ids.isEmpty()) {
                vm.setRange(rangeForChip(ids.get(0)));
            }
        });
        b.spSubject.setOnItemSelectedListener(new SimpleListener() {
            @Override
            void selected(int pos) {
                if (!updatingSpinners && pos >= 0 && pos < subjectValues.size()) {
                    vm.setCategory(subjectValues.get(pos));
                }
            }
        });
        b.spQuiz.setOnItemSelectedListener(new SimpleListener() {
            @Override
            void selected(int pos) {
                if (!updatingSpinners && pos >= 0 && pos < quizValues.size()) {
                    vm.setQuiz(quizValues.get(pos));
                }
            }
        });
        b.chartTime.setEmptyText(getString(R.string.an_empty_chart));
        b.chartParticipation.setEmptyText(getString(R.string.an_empty_chart));
        b.chartDistribution.setEmptyText(getString(R.string.an_empty_chart));
        b.chartSubjects.setEmptyText(getString(R.string.an_empty_chart));
        b.chartQuizzes.setEmptyText(getString(R.string.an_empty_chart));
        b.chartImprovement.setEmptyText(getString(R.string.an_empty_chart));

        vm.classCode.observe(getViewLifecycleOwner(), c -> {
            if (b != null && c != null) {
                b.tvClass.setText(getString(R.string.class_label, c));
            }
        });
        vm.report.observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (vm != null) {
            vm.refresh();
        }
    }

    private static int chipForRange(int days) {
        switch (days) {
            case 7: return R.id.chipR7;
            case 90: return R.id.chipR90;
            case 0: return R.id.chipRAll;
            default: return R.id.chipR30;
        }
    }

    private static int rangeForChip(int id) {
        if (id == R.id.chipR7) {
            return 7;
        }
        if (id == R.id.chipR90) {
            return 90;
        }
        if (id == R.id.chipRAll) {
            return 0;
        }
        return 30;
    }

    private void render(@Nullable Report r) {
        if (b == null || r == null) {
            return;
        }
        bindSpinners(r);
        b.tvEmptyBanner.setText(R.string.an_empty);
        b.tvEmptyBanner.setVisibility(r.hasData ? View.GONE : View.VISIBLE);

        Overview o = r.overview;
        b.tvEnrolled.setText(String.valueOf(o.enrolled));
        b.tvParticipants.setText(String.valueOf(o.participants));
        b.tvParticipation.setText(getString(R.string.pct_format, Math.round(o.participationPct)));
        b.tvCompleted.setText(String.valueOf(o.completed));
        boolean data = r.hasData;
        b.tvAvgScore.setText(data ? getString(R.string.an_avg_points_value, o.avgPoints)
                : getString(R.string.an_not_available));
        b.tvAvgAccuracy.setText(data ? getString(R.string.pct_format, Math.round(o.avgAccuracyPct))
                : getString(R.string.an_not_available));
        b.tvHighest.setText(data && o.highestPct >= 0
                ? getString(R.string.an_highest_value, Math.round(o.highestPct), o.highestName)
                : getString(R.string.an_not_available));
        b.tvTopStudent.setText(data && o.topStudent != null && !o.topStudent.isEmpty()
                ? getString(R.string.an_top_value, o.topStudent, o.topStudentPoints)
                : getString(R.string.an_not_available));
        b.tvAvgTime.setText(o.avgTimeMs >= 0 ? Fmt.time(o.avgTimeMs) : getString(R.string.an_not_available));

        renderStudent(r);

        String cls = getString(R.string.an_series_class);
        String you = getString(R.string.an_series_you);
        b.chartTime.setData(r.timeLabels, r.timeClass, cls, r.timeStudent, you);
        b.tvParticipationSub.setText(r.weeklyBuckets ? R.string.an_chart_participation_week
                : R.string.an_chart_participation_day);
        b.chartParticipation.setData(r.timeLabels, r.participation);

        double[] dist = new double[r.distribution.length];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = r.distribution[i];
        }
        b.chartDistribution.setData(Arrays.asList(ClassAnalytics.DIST_LABELS), data ? dist : new double[0]);

        bindGroups(b.chartSubjects, r.bySubject, cls, you);
        bindGroups(b.chartQuizzes, r.byQuiz, cls, you);

        boolean hasImprovement = r.improvement.length > 0;
        double[] classLine = null;
        if (hasImprovement && r.student != null) {
            classLine = new double[r.improvement.length];
            Arrays.fill(classLine, r.student.classAvgPct);
        }
        b.chartImprovement.setData(r.improvementLabels, r.improvement, you, classLine, cls);
    }

    private void renderStudent(Report r) {
        StudentSummary s = r.student;
        if (s == null || s.attempts == 0) {
            b.tvYou.setText(R.string.an_you_none);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.an_you_attempts, s.attempts)).append('\n');
        sb.append(getString(R.string.an_you_avg, Math.round(s.avgAccuracyPct), Math.round(s.classAvgPct))).append('\n');
        sb.append(getString(R.string.an_you_best, s.bestScore, s.bestTotal, Math.round(s.bestPct))).append('\n');
        if (s.rank > 0) {
            sb.append(getString(R.string.an_you_rank, s.rank, s.ofParticipants)).append('\n');
        }
        double d = s.deltaVsClass;
        sb.append(Math.abs(d) < 0.05 ? getString(R.string.an_you_delta_same)
                : d > 0 ? getString(R.string.an_you_delta_up, d)
                : getString(R.string.an_you_delta_down, Math.abs(d))).append('\n');
        sb.append(s.trend != null && s.trend.equals("Not enough data yet")
                ? getString(R.string.an_you_trend, s.trend)
                : getString(R.string.an_you_trend_delta, s.trend, s.trendDelta));
        if (s.strongest != null && s.weakest != null) {
            sb.append('\n').append(getString(R.string.an_you_strongest, s.strongest, Math.round(s.strongestPct)));
            sb.append('\n').append(getString(R.string.an_you_weakest, s.weakest, Math.round(s.weakestPct)));
        }
        b.tvYou.setText(sb.toString());
    }

    private void bindGroups(com.example.adminquiz.views.GroupedBarsView view, List<Group> groups,
                            String cls, String you) {
        int n = groups.size();
        List<String> labels = new ArrayList<>(n);
        double[] classPct = new double[n];
        double[] yourPct = new double[n];
        for (int i = 0; i < n; i++) {
            Group g = groups.get(i);
            labels.add(g.label);
            classPct[i] = g.classPct;
            yourPct[i] = g.studentPct;
        }
        view.setData(labels, classPct, yourPct, cls, you);
    }

    /** Rebuilds the subject and quiz drop-downs without triggering their listeners. */
    private void bindSpinners(Report r) {
        updatingSpinners = true;
        try {
            subjectValues.clear();
            List<String> subjectLabels = new ArrayList<>();
            subjectValues.add(null);
            subjectLabels.add(getString(R.string.an_all_subjects));
            for (String c : r.categories) {
                subjectValues.add(c);
                subjectLabels.add(c);
            }
            quizValues.clear();
            List<String> quizLabels = new ArrayList<>();
            quizValues.add(-1L);
            quizLabels.add(getString(R.string.an_all_quizzes));
            for (int i = 0; i < r.quizIds.size(); i++) {
                quizValues.add(r.quizIds.get(i));
                quizLabels.add(r.quizTitles.get(i));
            }
            fill(b.spSubject, subjectLabels, indexOfString(subjectValues, r.filter.category));
            fill(b.spQuiz, quizLabels, quizValues.indexOf(r.filter.quizId < 0 ? -1L : r.filter.quizId));
        } finally {
            b.spSubject.post(() -> updatingSpinners = false);
        }
    }

    private static int indexOfString(List<String> list, String v) {
        for (int i = 0; i < list.size(); i++) {
            String x = list.get(i);
            if (x == null ? v == null : x.equals(v)) {
                return i;
            }
        }
        return 0;
    }

    private void fill(Spinner sp, List<String> labels, int selection) {
        ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        sp.setSelection(Math.max(0, selection), false);
    }

    private abstract static class SimpleListener implements AdapterView.OnItemSelectedListener {
        abstract void selected(int pos);

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            selected(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
