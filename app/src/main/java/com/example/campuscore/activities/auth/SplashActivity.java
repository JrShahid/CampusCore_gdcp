package com.example.campuscore.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ActivitySplashBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.NavigationUtils;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;

public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    private FirebaseUserRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new FirebaseUserRepository();

        new Handler(Looper.getMainLooper()).postDelayed(this::routeUser, 900);
    }

    private void routeUser() {
        if (repository.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            binding.progressBar.setVisibility(View.GONE);
            SnackbarUtils.show(binding.getRoot(), getString(R.string.error_no_internet));
            binding.getRoot().postDelayed(this::routeUser, 2500);
            return;
        }

        if (!repository.isEmailVerified()) {
            NavigationUtils.openVerifyEmailAndClear(this);
            return;
        }

        repository.fetchCurrentUser(new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel data) {
                NavigationUtils.openDashboard(SplashActivity.this, data);
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                SnackbarUtils.show(binding.getRoot(), message);
                repository.logout();
                binding.getRoot().postDelayed(() -> {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }, 1800);
            }
        });
    }
}
