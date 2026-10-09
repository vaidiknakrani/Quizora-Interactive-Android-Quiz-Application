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

import com.example.adminquiz.adapters.LeaderboardAdapter;
import com.example.adminquiz.databinding.FragmentLeaderboardBinding;
import com.example.adminquiz.viewmodels.StatsViewModel;
import com.google.android.material.tabs.TabLayout;

public class LeaderboardFragment extends Fragment {
    private FragmentLeaderboardBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentLeaderboardBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        StatsViewModel vm = new ViewModelProvider(this).get(StatsViewModel.class);
        LeaderboardAdapter adapter = new LeaderboardAdapter();
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        b.tabs.addTab(b.tabs.newTab().setText("Global"));
        b.tabs.addTab(b.tabs.newTab().setText("Weekly"));
        b.tabs.addTab(b.tabs.newTab().setText("Monthly"));
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                vm.setPeriod(t.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });
        vm.setPeriod(0);
        vm.leaderboard.observe(getViewLifecycleOwner(), rows -> {
            adapter.submit(rows);
            b.tvEmpty.setVisibility(rows == null || rows.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
