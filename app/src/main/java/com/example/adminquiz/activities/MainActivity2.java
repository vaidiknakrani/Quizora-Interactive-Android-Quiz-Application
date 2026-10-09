package com.example.adminquiz.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.adminquiz.R;
import com.example.adminquiz.databinding.ActivityMain2Binding;
import com.example.adminquiz.databinding.NavHeaderBinding;
import com.example.adminquiz.utils.SessionManager;
import com.example.adminquiz.utils.UiUtils;
import com.example.adminquiz.viewmodels.AuthViewModel;

/** Hosts navigation only. No networking, database or AI logic lives here. */
public class MainActivity2 extends AppCompatActivity {

    private ActivityMain2Binding binding;
    private NavController navController;
    private AuthViewModel auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity2.class));
            finish();
            return;
        }
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = new ViewModelProvider(this).get(AuthViewModel.class);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (host == null) {
            return;
        }
        navController = host.getNavController();

        AppBarConfiguration config = new AppBarConfiguration.Builder(
                R.id.homeFragment, R.id.myQuizzes, R.id.ghostMatches, R.id.aiTests,
                R.id.leaderboard, R.id.classLeaderboard, R.id.activityStreaks,
                R.id.classAnalytics, R.id.personas, R.id.profile, R.id.settings, R.id.about)
                .setOpenableLayout(binding.drawerLayout)
                .build();
        NavigationUI.setupWithNavController(binding.toolbar, navController, config);

        binding.toolbar.inflateMenu(R.menu.toolbar_menu);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                openTopLevel(R.id.profile);
                return true;
            }
            if (item.getItemId() == R.id.action_leaderboard) {
                openTopLevel(R.id.leaderboard);
                return true;
            }
            return false;
        });

        setupDrawer();
        setupBackPress();
    }

    /** Opens a top-level destination from the toolbar unless it is already showing. */
    private void openTopLevel(int destinationId) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destinationId) {
            return;
        }
        navController.navigate(destinationId);
    }

    private void setupDrawer() {
        NavHeaderBinding header = NavHeaderBinding.bind(binding.navigationView.getHeaderView(0));
        auth.currentUser().observe(this, user -> {
            if (user != null) {
                header.headerName.setText(user.username);
                header.headerEmail.setText(user.email);
            }
        });

        binding.navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_privacy) {
                openPrivacyPolicy();
            } else if (id == R.id.nav_logout) {
                auth.logout();
                Intent i = new Intent(this, LoginActivity2.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
                return true;
            } else if (id == R.id.createQuiz) {
                startActivity(new Intent(this, CreateQuizActivity2.class));
            } else {
                NavigationUI.onNavDestinationSelected(item, navController);
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        navController.addOnDestinationChangedListener((controller, destination, args) ->
                binding.navigationView.setCheckedItem(destination.getId()));
    }

    private void openPrivacyPolicy() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))));
        } catch (Exception e) {
            UiUtils.toast(this, "No browser available");
        }
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                } else if (!navController.navigateUp()) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}
