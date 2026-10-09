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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adminquiz.activities.QuizActivity2;
import com.example.adminquiz.adapters.GhostAdapter;
import com.example.adminquiz.databinding.FragmentGhostMatchesBinding;
import com.example.adminquiz.models.GhostItem;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.GhostViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class GhostMatchesFragment extends Fragment {
    private FragmentGhostMatchesBinding b;
    private GhostViewModel vm;
    private GhostAdapter adapter;
    private List<GhostItem> mine;
    private List<GhostItem> pending;
    private List<GhostItem> completed;
    private int tab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentGhostMatchesBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(this).get(GhostViewModel.class);
        adapter = new GhostAdapter(new GhostAdapter.Actions() {
            @Override
            public void onShare(GhostItem item) {
                vm.shareExisting(item.ghostId);
            }

            @Override
            public void onPlay(GhostItem item) {
                Intent i = new Intent(requireContext(), QuizActivity2.class);
                i.putExtra(QuizActivity2.EXTRA_QUIZ_ID, item.quizId);
                i.putExtra(QuizActivity2.EXTRA_MATCH_ID, item.matchId);
                startActivity(i);
            }

            @Override
            public void onReject(GhostItem item) {
                vm.reject(item.matchId);
            }
        });
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);

        b.tabs.addTab(b.tabs.newTab().setText("My Ghosts"));
        b.tabs.addTab(b.tabs.newTab().setText("Pending"));
        b.tabs.addTab(b.tabs.newTab().setText("Completed"));
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab t) {
                tab = t.getPosition();
                refresh();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab t) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab t) {
            }
        });

        vm.myGhosts.observe(getViewLifecycleOwner(), l -> {
            mine = l;
            refresh();
        });
        vm.pending.observe(getViewLifecycleOwner(), l -> {
            pending = l;
            refresh();
        });
        vm.completed.observe(getViewLifecycleOwner(), l -> {
            completed = l;
            refresh();
        });

        b.btnAdd.setOnClickListener(v -> vm.importChallenge(UiUtils.text(b.etChallenge)));
        vm.importResult.observe(getViewLifecycleOwner(), r -> {
            if (r == null) {
                return;
            }
            if (r.status == Resource.Status.SUCCESS) {
                UiUtils.toast(requireContext(), "Challenge added to Pending");
                b.etChallenge.setText("");
                TabLayout.Tab t = b.tabs.getTabAt(1);
                if (t != null) {
                    t.select();
                }
            } else if (r.status == Resource.Status.ERROR) {
                UiUtils.toast(requireContext(), r.message);
            }
            vm.consumeImport();
        });
        vm.shareResult.observe(getViewLifecycleOwner(), r -> {
            if (r == null) {
                return;
            }
            if (r.status == Resource.Status.SUCCESS) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, r.data);
                startActivity(Intent.createChooser(send, "Share challenge"));
            } else if (r.status == Resource.Status.ERROR) {
                UiUtils.toast(requireContext(), r.message);
            }
            vm.consumeShare();
        });
    }

    private void refresh() {
        if (b == null) {
            return;
        }
        List<GhostItem> list = tab == 0 ? mine : tab == 1 ? pending : completed;
        adapter.submit(list);
        boolean empty = list == null || list.isEmpty();
        b.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        b.tvEmpty.setText(tab == 0 ? "No ghosts yet.\nFinish a quiz, then tap Challenge a Friend."
                : tab == 1 ? "No pending challenges.\nPaste a challenge above."
                : "No completed matches yet.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
