package com.example.campuscore.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ActivityLoginBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.NavigationUtils;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private FirebaseUserRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new FirebaseUserRepository();

        binding.loginButton.setOnClickListener(view -> login());
        binding.signupText.setOnClickListener(view -> startActivity(new Intent(this, SignupActivity.class)));
        binding.forgotPasswordText.setOnClickListener(view -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void login() {
        String email = text(binding.emailInput.getText());
        String password = text(binding.passwordInput.getText());
        clearErrors();

        if (!validate(email, password) || !checkNetwork()) {
            return;
        }

        setLoading(true);
        repository.login(email, password, new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel data) {
                setLoading(false);
                if (!repository.isEmailVerified()) {
                    NavigationUtils.openVerifyEmailAndClear(LoginActivity.this);
                    return;
                }
                NavigationUtils.openDashboard(LoginActivity.this, data);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private boolean validate(String email, String password) {
        boolean valid = true;
        if (!ValidationUtils.isValidEmail(email)) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            binding.passwordLayout.setError(getString(R.string.error_password_length));
            valid = false;
        }
        return valid;
    }

    private boolean checkNetwork() {
        if (!NetworkUtils.isOnline(this)) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return false;
        }
        return true;
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
    }

    private void clearErrors() {
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
