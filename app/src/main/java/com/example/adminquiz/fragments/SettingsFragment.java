package com.example.adminquiz.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.adminquiz.QuizApp;
import com.example.adminquiz.R;
import com.example.adminquiz.databinding.FragmentSettingsBinding;
import com.example.adminquiz.utils.SecureStore;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.ClassSettingsViewModel;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding b;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentSettingsBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SharedPreferences prefs = requireContext().getSharedPreferences(QuizApp.PREFS, Context.MODE_PRIVATE);
        SecureStore store = new SecureStore(requireContext());

        b.switchDark.setChecked(prefs.getBoolean(QuizApp.KEY_DARK, false));
        b.switchDark.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean(QuizApp.KEY_DARK, checked).apply();
            AppCompatDelegate.setDefaultNightMode(checked
                    ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        b.tvKeyStatus.setText(store.hasSavedKey() ? "A Gemini API key is saved (encrypted)."
                : "No key saved. A development key from local.properties is used if present.");
        b.btnSaveKey.setOnClickListener(v -> {
            store.setGeminiKey(UiUtils.text(b.etApiKey));
            b.etApiKey.setText("");
            b.tvKeyStatus.setText(store.hasSavedKey() ? "A Gemini API key is saved (encrypted)."
                    : "Saved key cleared.");
            UiUtils.toast(requireContext(), "Saved");
        });

        setupClassSection();
    }

    /** Lets a student move to another class code; only their own record is ever changed. */
    private void setupClassSection() {
        ClassSettingsViewModel vm = new ViewModelProvider(this).get(ClassSettingsViewModel.class);
        vm.classCode.observe(getViewLifecycleOwner(), code -> {
            if (code != null) {
                b.tvClassCurrent.setText(getString(R.string.settings_class_current, code));
            }
        });
        b.btnSaveClass.setOnClickListener(v -> vm.save(UiUtils.text(b.etClassCode)));
        vm.saveResult.observe(getViewLifecycleOwner(), res -> {
            if (res == null) {
                return;
            }
            if (res.status == Resource.Status.SUCCESS) {
                b.etClassCode.setText("");
                UiUtils.toast(requireContext(), getString(R.string.settings_class_saved, res.data));
            } else if (res.status == Resource.Status.ERROR) {
                UiUtils.toast(requireContext(), res.message);
            }
            vm.consume();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
