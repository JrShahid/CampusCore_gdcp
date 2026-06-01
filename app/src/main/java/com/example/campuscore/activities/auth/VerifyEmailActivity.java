package com.example.campuscore.activities.auth;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ActivityVerifyEmailBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.NavigationUtils;
import com.example.campuscore.utils.SnackbarUtils;

public class VerifyEmailActivity extends AppCompatActivity {
    private ActivityVerifyEmailBinding binding;
    private FirebaseUserRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new FirebaseUserRepository();

        binding.resendButton.setOnClickListener(view -> sendVerificationEmail());
        binding.refreshButton.setOnClickListener(view -> refreshStatus());
        binding.logoutButton.setOnClickListener(view -> {
            repository.logout();
            NavigationUtils.openLoginAndClear(this);
        });

        sendVerificationEmail();
    }

    private void sendVerificationEmail() {
        setLoading(true);
        repository.sendVerificationEmail(new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.verification_email_sent));
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void refreshStatus() {
        setLoading(true);
        repository.refreshEmailVerification(new FirestoreCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean verified) {
                if (Boolean.TRUE.equals(verified)) {
                    repository.fetchCurrentUser(new FirestoreCallback<UserModel>() {
                        @Override
                        public void onSuccess(UserModel user) {
                            setLoading(false);
                            NavigationUtils.openDashboard(VerifyEmailActivity.this, user);
                        }

                        @Override
                        public void onError(String message) {
                            setLoading(false);
                            SnackbarUtils.show(binding.rootLayout, message);
                        }
                    });
                    return;
                }
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.email_not_verified));
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.resendButton.setEnabled(!loading);
        binding.refreshButton.setEnabled(!loading);
        binding.logoutButton.setEnabled(!loading);
    }
}
