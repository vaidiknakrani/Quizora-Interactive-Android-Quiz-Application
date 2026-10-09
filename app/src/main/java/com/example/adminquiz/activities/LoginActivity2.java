package com.example.adminquiz.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.adminquiz.databinding.ActivityLogin2Binding;
import com.example.adminquiz.utils.Resource;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.AuthViewModel;

public class LoginActivity2 extends AppCompatActivity {
    private ActivityLogin2Binding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLogin2Binding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        AuthViewModel vm = new ViewModelProvider(this).get(AuthViewModel.class);

        b.btnLogin.setOnClickListener(v ->
                vm.login(UiUtils.text(b.etEmail), b.etPassword.getText() == null ? "" : b.etPassword.getText().toString()));
        b.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity2.class)));

        vm.authResult.observe(this, r -> {
            if (r == null) {
                return;
            }
            boolean loading = r.status == Resource.Status.LOADING;
            b.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
            b.btnLogin.setEnabled(!loading);
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
