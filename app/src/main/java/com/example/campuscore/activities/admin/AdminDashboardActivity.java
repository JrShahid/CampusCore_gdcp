package com.example.campuscore.activities.admin;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ActivityAdminDashboardBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.fragments.AboutCampusCoreFragment;
import com.example.campuscore.fragments.AttendanceFragment;
import com.example.campuscore.fragments.HomeFragment;
import com.example.campuscore.fragments.NotesFragment;
import com.example.campuscore.fragments.ProfileFragment;
import com.example.campuscore.fragments.admin.AddEditDepartmentFragment;
import com.example.campuscore.fragments.admin.AddEditStudentFragment;
import com.example.campuscore.fragments.admin.AddEditSubjectFragment;
import com.example.campuscore.fragments.admin.AddEditTeachingAssignmentFragment;
import com.example.campuscore.fragments.admin.ManageDepartmentsFragment;
import com.example.campuscore.fragments.admin.ManagePendingTeachersFragment;
import com.example.campuscore.fragments.admin.ManageStudentsFragment;
import com.example.campuscore.fragments.admin.ManageSubjectsFragment;
import com.example.campuscore.fragments.admin.ManageTeachingAssignmentsFragment;
import com.example.campuscore.fragments.updates.CampusCoreUpdatesFragment;
import com.example.campuscore.utils.IntentConstants;
import com.example.campuscore.utils.NavigationUtils;

public class AdminDashboardActivity extends AppCompatActivity implements HomeFragment.DashboardNavigator {
    private static final String TAG_HOME = "admin_home";
    private static final String TAG_ATTENDANCE = "admin_attendance";
    private static final String TAG_MANAGE_STUDENTS = "admin_manage_students";
    private static final String TAG_MANAGE_DEPARTMENTS = "admin_manage_departments";
    private static final String TAG_MANAGE_SUBJECTS = "admin_manage_subjects";
    private static final String TAG_MANAGE_PENDING_TEACHERS = "admin_manage_pending_teachers";
    private static final String TAG_MANAGE_TEACHING_ASSIGNMENTS = "admin_manage_teaching_assignments";
    private static final String TAG_NOTES = "admin_notes";
    private static final String TAG_UPDATES = "admin_updates";
    private static final String TAG_PROFILE = "admin_profile";
    private static final String TAG_ABOUT = "admin_about";

    private ActivityAdminDashboardBinding binding;
    private FirebaseUserRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new FirebaseUserRepository();

        binding.toolbar.setNavigationOnClickListener(view -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        binding.navigationView.setCheckedItem(R.id.nav_home);
        binding.navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                repository.logout();
                NavigationUtils.openLoginAndClear(this);
                return true;
            }
            navigateToDestination(id);
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        getSupportFragmentManager().addOnBackStackChangedListener(this::syncToolbarWithCurrentFragment);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            showRoot(HomeFragment.newInstance(FirebaseUserRepository.ROLE_ADMIN), TAG_HOME);
        } else {
            syncToolbarWithCurrentFragment();
        }
    }

    private void showRoot(Fragment fragment, String tag) {
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        syncToolbarForTag(tag);
    }

    private void showSecondary(Fragment fragment, String tag) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current != null && tag.equals(current.getTag())) {
            syncToolbarForTag(tag);
            return;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment, tag)
                .addToBackStack(tag)
                .commit();
        syncToolbarForTag(tag);
    }

    private void navigateToDestination(int navigationItemId) {
        if (navigationItemId == R.id.nav_home) {
            showRoot(HomeFragment.newInstance(FirebaseUserRepository.ROLE_ADMIN), TAG_HOME);
        } else if (navigationItemId == R.id.nav_manage_students) {
            showSecondary(ManageStudentsFragment.newInstance(), TAG_MANAGE_STUDENTS);
        } else if (navigationItemId == R.id.nav_manage_departments) {
            showSecondary(ManageDepartmentsFragment.newInstance(), TAG_MANAGE_DEPARTMENTS);
        } else if (navigationItemId == R.id.nav_manage_subjects) {
            showSecondary(ManageSubjectsFragment.newInstance(), TAG_MANAGE_SUBJECTS);
        } else if (navigationItemId == R.id.nav_manage_pending_teachers) {
            showSecondary(ManagePendingTeachersFragment.newInstance(), TAG_MANAGE_PENDING_TEACHERS);
        } else if (navigationItemId == R.id.nav_manage_teaching_assignments) {
            showSecondary(ManageTeachingAssignmentsFragment.newInstance(), TAG_MANAGE_TEACHING_ASSIGNMENTS);
        } else if (navigationItemId == R.id.nav_attendance) {
            showSecondary(AttendanceFragment.create(), TAG_ATTENDANCE);
        } else if (navigationItemId == R.id.nav_notes) {
            showSecondary(NotesFragment.create(), TAG_NOTES);
        } else if (navigationItemId == R.id.nav_updates) {
            showSecondary(CampusCoreUpdatesFragment.newInstance(), TAG_UPDATES);
        } else if (navigationItemId == R.id.nav_profile) {
            showSecondary(ProfileFragment.create(userName(), userEmail(), FirebaseUserRepository.ROLE_ADMIN), TAG_PROFILE);
        } else if (navigationItemId == R.id.nav_about) {
            showSecondary(AboutCampusCoreFragment.newInstance(), TAG_ABOUT);
        }
    }

    @Override
    public void navigateTo(int navigationItemId) {
        navigateToDestination(navigationItemId);
    }

    private void syncToolbarWithCurrentFragment() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (current == null) {
            syncToolbarForTag(TAG_HOME);
            return;
        }
        if (current instanceof AddEditDepartmentFragment) {
            syncToolbar(getString(hasArgument(current, "departmentId")
                    ? R.string.edit_department
                    : R.string.add_department), R.id.nav_manage_departments);
        } else if (current instanceof AddEditSubjectFragment) {
            syncToolbar(getString(hasArgument(current, "subjectCode")
                    ? R.string.edit_subject
                    : R.string.add_subject), R.id.nav_manage_subjects);
        } else if (current instanceof AddEditStudentFragment) {
            syncToolbar(getString(hasArgument(current, "uid")
                    ? R.string.edit_student
                    : R.string.add_student), R.id.nav_manage_students);
        } else if (current instanceof AddEditTeachingAssignmentFragment) {
            syncToolbar(getString(hasArgument(current, "assignmentId")
                    ? R.string.edit_teaching_assignment
                    : R.string.add_teaching_assignment), R.id.nav_manage_teaching_assignments);
        } else if (current instanceof ManageDepartmentsFragment) {
            syncToolbar(getString(R.string.manage_departments), R.id.nav_manage_departments);
        } else if (current instanceof ManageSubjectsFragment) {
            syncToolbar(getString(R.string.manage_subjects), R.id.nav_manage_subjects);
        } else if (current instanceof ManageStudentsFragment) {
            syncToolbar(getString(R.string.manage_students), R.id.nav_manage_students);
        } else if (current instanceof ManagePendingTeachersFragment) {
            syncToolbar(getString(R.string.manage_pending_teachers), R.id.nav_manage_pending_teachers);
        } else if (current instanceof ManageTeachingAssignmentsFragment) {
            syncToolbar(getString(R.string.manage_teaching_assignments), R.id.nav_manage_teaching_assignments);
        } else if (current instanceof AttendanceFragment) {
            syncToolbar(getString(R.string.attendance), R.id.nav_attendance);
        } else if (current instanceof NotesFragment) {
            syncToolbar(getString(R.string.notes), R.id.nav_notes);
        } else if (current instanceof CampusCoreUpdatesFragment) {
            syncToolbar(getString(R.string.updates), R.id.nav_updates);
        } else if (current instanceof ProfileFragment) {
            syncToolbar(getString(R.string.profile), R.id.nav_profile);
        } else if (current instanceof AboutCampusCoreFragment) {
            syncToolbar(getString(R.string.about_campuscore), R.id.nav_about);
        } else {
            syncToolbar(getString(R.string.toolbar_brand_compact), R.id.nav_home);
        }
    }

    private void syncToolbarForTag(String tag) {
        if (TAG_ATTENDANCE.equals(tag)) {
            syncToolbar(getString(R.string.attendance), R.id.nav_attendance);
        } else if (TAG_MANAGE_STUDENTS.equals(tag)) {
            syncToolbar(getString(R.string.manage_students), R.id.nav_manage_students);
        } else if (TAG_MANAGE_DEPARTMENTS.equals(tag)) {
            syncToolbar(getString(R.string.manage_departments), R.id.nav_manage_departments);
        } else if (TAG_MANAGE_SUBJECTS.equals(tag)) {
            syncToolbar(getString(R.string.manage_subjects), R.id.nav_manage_subjects);
        } else if (TAG_MANAGE_PENDING_TEACHERS.equals(tag)) {
            syncToolbar(getString(R.string.manage_pending_teachers), R.id.nav_manage_pending_teachers);
        } else if (TAG_MANAGE_TEACHING_ASSIGNMENTS.equals(tag)) {
            syncToolbar(getString(R.string.manage_teaching_assignments), R.id.nav_manage_teaching_assignments);
        } else if (TAG_NOTES.equals(tag)) {
            syncToolbar(getString(R.string.notes), R.id.nav_notes);
        } else if (TAG_UPDATES.equals(tag)) {
            syncToolbar(getString(R.string.updates), R.id.nav_updates);
        } else if (TAG_PROFILE.equals(tag)) {
            syncToolbar(getString(R.string.profile), R.id.nav_profile);
        } else if (TAG_ABOUT.equals(tag)) {
            syncToolbar(getString(R.string.about_campuscore), R.id.nav_about);
        } else {
            syncToolbar(getString(R.string.toolbar_brand_compact), R.id.nav_home);
        }
    }

    private void syncToolbar(String title, int checkedItemId) {
        boolean hasBackStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        binding.toolbar.setTitle(title);
        binding.toolbar.setNavigationIcon(hasBackStack
                ? androidx.appcompat.R.drawable.abc_ic_ab_back_material
                : R.drawable.ic_menu_drawer);
        binding.toolbar.setNavigationIconTint(getColor(R.color.white));
        binding.navigationView.setCheckedItem(checkedItemId);
    }

    private boolean hasArgument(Fragment fragment, String key) {
        return fragment.getArguments() != null && fragment.getArguments().containsKey(key);
    }

    private String userName() {
        return getIntent().getStringExtra(IntentConstants.EXTRA_USER_NAME) == null
                ? "Admin"
                : getIntent().getStringExtra(IntentConstants.EXTRA_USER_NAME);
    }

    private String userEmail() {
        return getIntent().getStringExtra(IntentConstants.EXTRA_USER_EMAIL) == null
                ? ""
                : getIntent().getStringExtra(IntentConstants.EXTRA_USER_EMAIL);
    }
}
