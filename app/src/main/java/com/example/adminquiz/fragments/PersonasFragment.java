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

import com.example.adminquiz.activities.ResultActivity2;
import com.example.adminquiz.adapters.AttemptAdapter;
import com.example.adminquiz.databinding.FragmentPersonasBinding;
import com.example.adminquiz.viewmodels.StatsViewModel;

public class PersonasFragment extends Fragment {
    private FragmentPersonasBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPersonasBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        StatsViewModel vm = new ViewModelProvider(this).get(StatsViewModel.class);
        AttemptAdapter adapter = new AttemptAdapter(a -> {
            Intent i = new Intent(requireContext(), ResultActivity2.class);
            i.putExtra(ResultActivity2.EXTRA_ATTEMPT_ID, a.id);
            startActivity(i);
        });
        b.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.recycler.setAdapter(adapter);
        vm.personas.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            b.tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
