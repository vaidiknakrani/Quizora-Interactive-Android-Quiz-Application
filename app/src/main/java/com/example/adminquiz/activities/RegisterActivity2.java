package com.example.adminquiz.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adminquiz.databinding.ActivityRegister2Binding;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.AuthViewModel;

public class RegisterActivity2 extends AppCompatActivity {
    private ActivityRegister2Binding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegister2Binding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        AuthViewModel vm = new ViewModelProvider(this).get(AuthViewModel.class);

        b.btnRegister.setOnClickListener(v -> vm.register(
                UiUtils.text(b.etUsername), UiUtils.text(b.etEmail),
                b.etPassword.getText() == null ? "" : b.etPassword.getText().toString(),
                b.etConfirm.getText() == null ? "" : b.etConfirm.getText().toString(),
                UiUtils.text(b.etClassCode)));
        b.tvLogin.setOnClickListener(v -> finish());

        vm.authResult.observe(this, r -> {
            if (r == null) {
                return;
            }
            boolean loading = r.status == Resource.Status.LOADING;
            b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.btnRegister.setEnabled(!loading);
            b.tvError.setVisibility(r.status == Resource.Status.ERROR ? View.VISIBLE : View.GONE);
            if (r.status == Resource.Status.ERROR) {
                b.tvError.setText(r.message);
            } else if (r.status == Resource.Status.SUCCESS) {
                Intent i = new Intent(this, MainActivity2.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });
    }
}
