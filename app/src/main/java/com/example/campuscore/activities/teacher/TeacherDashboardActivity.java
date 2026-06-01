package com.example.campuscore.activities.teacher;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ActivityDashboardBottomBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.fragments.attendance.MarkAttendanceFragment;
import com.example.campuscore.fragments.HomeFragment;
import com.example.campuscore.fragments.notes.UploadNotesFragment;
import com.example.campuscore.fragments.ProfileFragment;
import com.example.campuscore.fragments.updates.CampusCoreUpdatesFragment;
import com.example.campuscore.utils.IntentConstants;
import com.example.campuscore.utils.NavigationUtils;

public class TeacherDashboardActivity extends AppCompatActivity implements HomeFragment.DashboardNavigator {
    private ActivityDashboardBottomBinding binding;
    private FirebaseUserRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBottomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new FirebaseUserRepository();

        binding.toolbar.setTitle(R.string.toolbar_brand_compact);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                repository.logout();
                NavigationUtils.openLoginAndClear(this);
                return true;
            }
            return false;
        });

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_attendance) {
                fragment = MarkAttendanceFragment.newInstance();
            } else if (id == R.id.nav_notes) {
                fragment = UploadNotesFragment.newInstance();
            } else if (id == R.id.nav_updates) {
                fragment = CampusCoreUpdatesFragment.newInstance();
            } else if (id == R.id.nav_profile) {
                fragment = ProfileFragment.create(userName(), userEmail(), FirebaseUserRepository.ROLE_TEACHER);
            } else {
                fragment = HomeFragment.newInstance(FirebaseUserRepository.ROLE_TEACHER);
            }
            show(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            show(HomeFragment.newInstance(FirebaseUserRepository.ROLE_TEACHER));
        }
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void navigateTo(int navigationItemId) {
        binding.bottomNavigation.setSelectedItemId(navigationItemId);
    }

    private String userName() {
        return getIntent().getStringExtra(IntentConstants.EXTRA_USER_NAME) == null
                ? "Teacher"
                : getIntent().getStringExtra(IntentConstants.EXTRA_USER_NAME);
    }

    private String userEmail() {
        return getIntent().getStringExtra(IntentConstants.EXTRA_USER_EMAIL) == null
                ? ""
                : getIntent().getStringExtra(IntentConstants.EXTRA_USER_EMAIL);
    }
}
