package com.example.campuscore.activities.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ActivitySignupBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.NavigationUtils;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

import java.util.Arrays;

public class SignupActivity extends AppCompatActivity {
    private ActivitySignupBinding binding;
    private FirebaseUserRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new FirebaseUserRepository();
        binding.roleInput.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Arrays.asList(getString(R.string.student), getString(R.string.teacher))));
        binding.roleInput.setText(getString(R.string.student), false);
        binding.roleInput.setOnItemClickListener((parent, view, position, id) -> updateSignupMode());
        updateSignupMode();

        binding.signupButton.setOnClickListener(view -> signup());
        binding.loginText.setOnClickListener(view -> finish());
    }

    private void signup() {
        String email = text(binding.emailInput.getText());
        String password = text(binding.passwordInput.getText());
        String rollNumber = text(binding.rollInput.getText());
        String registrationNumber = text(binding.registrationInput.getText());
        String employeeId = text(binding.employeeInput.getText());
        boolean teacherSignup = isTeacherSignup();
        clearErrors();

        if (!validate(email, password, rollNumber, registrationNumber, employeeId, teacherSignup) || !checkNetwork()) {
            return;
        }

        setLoading(true);
        FirestoreCallback<UserModel> callback = new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel data) {
                setLoading(false);
                Toast.makeText(SignupActivity.this,
                        getString(R.string.account_successfully_linked),
                        Toast.LENGTH_LONG).show();
                NavigationUtils.openVerifyEmailAndClear(SignupActivity.this);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        };
        if (teacherSignup) {
            repository.signupTeacher(email, password, employeeId, callback);
        } else {
            repository.signup(email, password, rollNumber, registrationNumber, callback);
        }
    }

    private boolean validate(String email, String password, String rollNumber, String registrationNumber,
                             String employeeId, boolean teacherSignup) {
        boolean valid = true;
        if (!ValidationUtils.isValidEmail(email)) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            binding.passwordLayout.setError(getString(R.string.error_password_length));
            valid = false;
        }
        if (!teacherSignup && ValidationUtils.isBlank(rollNumber)) {
            binding.rollLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (!teacherSignup && ValidationUtils.isBlank(registrationNumber)) {
            binding.registrationLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (teacherSignup && ValidationUtils.isBlank(employeeId)) {
            binding.employeeLayout.setError(getString(R.string.error_required));
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
        binding.signupButton.setEnabled(!loading);
    }

    private void clearErrors() {
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.rollLayout.setError(null);
        binding.registrationLayout.setError(null);
        binding.employeeLayout.setError(null);
    }

    private void updateSignupMode() {
        boolean teacherSignup = isTeacherSignup();
        binding.rollLayout.setVisibility(teacherSignup ? View.GONE : View.VISIBLE);
        binding.registrationLayout.setVisibility(teacherSignup ? View.GONE : View.VISIBLE);
        binding.employeeLayout.setVisibility(teacherSignup ? View.VISIBLE : View.GONE);
    }

    private boolean isTeacherSignup() {
        return getString(R.string.teacher).equals(binding.roleInput.getText().toString().trim());
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
