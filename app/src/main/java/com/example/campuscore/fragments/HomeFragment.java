package com.example.campuscore.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.FeatureCardAdapter;
import com.example.campuscore.databinding.FragmentHomeBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.models.FeatureCard;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.repositories.TeachingAssignmentsRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String ARG_ROLE = "role";
    private FragmentHomeBinding binding;
    private FirebaseUserRepository userRepository;
    private TeachingAssignmentsRepository assignmentsRepository;
    private AcademicStructureRepository academicStructureRepository;
    private String currentRole;

    public interface DashboardNavigator {
        void navigateTo(int navigationItemId);
    }

    public static HomeFragment newInstance(String role) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROLE, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String role = getArguments() == null
                ? FirebaseUserRepository.ROLE_STUDENT
                : getArguments().getString(ARG_ROLE, FirebaseUserRepository.ROLE_STUDENT);
        currentRole = role;
        userRepository = new FirebaseUserRepository();
        assignmentsRepository = new TeachingAssignmentsRepository();
        academicStructureRepository = new AcademicStructureRepository();
        List<FeatureCard> cards = cardsFor(role);

        binding.headerText.setText(titleFor(role));
        binding.subtitleText.setText("Loading your active academic context...");
        binding.featureRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.featureRecyclerView.setAdapter(new FeatureCardAdapter(cards, this::openFeature));
        binding.retryButton.setOnClickListener(v -> loadDashboardContext());
        showEmptyState(cards.isEmpty(), getString(R.string.empty_home));
        loadDashboardContext();
    }

    private void openFeature(FeatureCard card) {
        if (getActivity() instanceof DashboardNavigator) {
            ((DashboardNavigator) getActivity()).navigateTo(card.getNavigationItemId());
        }
    }

    private String titleFor(String role) {
        if (FirebaseUserRepository.ROLE_ADMIN.equals(role)) {
            return getString(R.string.admin_dashboard_welcome);
        }
        if (FirebaseUserRepository.ROLE_TEACHER.equals(role)) {
            return getString(R.string.teacher_dashboard_welcome);
        }
        return getString(R.string.student_dashboard_welcome);
    }

    private List<FeatureCard> cardsFor(String role) {
        List<FeatureCard> cards = new ArrayList<>();
        int icon = R.drawable.ic_feature;

        if (FirebaseUserRepository.ROLE_ADMIN.equals(role)) {
            cards.add(new FeatureCard(icon, "Manage Users", "Review and organize campus accounts.", true));
            cards.add(new FeatureCard(icon, getString(R.string.manage_students), getString(R.string.manage_students_card), false, R.id.nav_manage_students));
            cards.add(new FeatureCard(icon, getString(R.string.manage_departments), getString(R.string.manage_departments_card), false, R.id.nav_manage_departments));
            cards.add(new FeatureCard(icon, getString(R.string.manage_subjects), getString(R.string.manage_subjects_card), false, R.id.nav_manage_subjects));
            cards.add(new FeatureCard(icon, getString(R.string.manage_pending_teachers), "Validate teacher onboarding records.", false, R.id.nav_manage_pending_teachers));
            cards.add(new FeatureCard(icon, getString(R.string.manage_teaching_assignments), "Assign teachers to classes and sections.", false, R.id.nav_manage_teaching_assignments));
            cards.add(new FeatureCard(icon, "Announcements", "Prepare official campus updates.", true));
            cards.add(new FeatureCard(icon, "Reports", "Institution reports and summaries.", true));
            cards.add(new FeatureCard(icon, "System Analytics", "Campus-wide insights and usage patterns.", true));
            return cards;
        }

        if (FirebaseUserRepository.ROLE_TEACHER.equals(role)) {
            cards.add(new FeatureCard(icon, "Mark Attendance", "Capture and review class attendance.", false, R.id.nav_attendance));
            cards.add(new FeatureCard(icon, "Upload Notes", "Share study material with students.", false, R.id.nav_notes));
            cards.add(new FeatureCard(icon, getString(R.string.updates), getString(R.string.updates_teacher_card), false, R.id.nav_updates));
            cards.add(new FeatureCard(icon, "Analytics", "Performance trends and engagement.", true));
            cards.add(new FeatureCard(icon, "Student Reports", "Individual academic reports.", true));
            return cards;
        }

        cards.add(new FeatureCard(icon, "Attendance", "Track your class attendance status.", false, R.id.nav_attendance));
        cards.add(new FeatureCard(icon, "Notes", "Access shared academic notes.", false, R.id.nav_notes));
        cards.add(new FeatureCard(icon, getString(R.string.updates), getString(R.string.updates_student_card), false, R.id.nav_updates));
        cards.add(new FeatureCard(icon, "Assignments", "Submission tracking and deadlines.", true));
        cards.add(new FeatureCard(icon, "AI Assistant", "Personal academic assistance.", true));
        cards.add(new FeatureCard(icon, "Placements", "Career drives and placement updates.", true));
        return cards;
    }

    private void loadDashboardContext() {
        if (FirebaseUserRepository.ROLE_ADMIN.equals(currentRole)) {
            binding.subtitleText.setText("Institution controls, academic structure, and onboarding workflows.");
            showEmptyState(false, "");
            return;
        }
        userRepository.fetchCurrentUser(new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel user) {
                if (FirebaseUserRepository.ROLE_TEACHER.equals(currentRole)) {
                    loadTeacherContext(user);
                } else {
                    loadStudentContext(user);
                }
            }

            @Override
            public void onError(String message) {
                binding.subtitleText.setText(message);
                showEmptyState(true, message);
            }
        });
    }

    private void loadTeacherContext(UserModel user) {
        assignmentsRepository.fetchTeacherAssignments(new FirestoreCallback<List<TeachingAssignmentModel>>() {
            @Override
            public void onSuccess(List<TeachingAssignmentModel> assignments) {
                if (assignments.isEmpty()) {
                    binding.subtitleText.setText(getString(R.string.no_teaching_assignments_help));
                    showEmptyState(true, getString(R.string.no_teaching_assignments));
                    return;
                }
                binding.subtitleText.setText(assignments.size() + " assigned classes active for attendance and notes.");
                showEmptyState(false, "");
            }

            @Override
            public void onError(String message) {
                binding.subtitleText.setText(message);
                showEmptyState(true, message);
            }
        });
    }

    private void loadStudentContext(UserModel user) {
        if (user.getDepartmentId().isEmpty() || user.getSemester().isEmpty()) {
            binding.subtitleText.setText(getString(R.string.no_academic_context_help));
            showEmptyState(true, getString(R.string.no_academic_context));
            return;
        }
        academicStructureRepository.fetchActiveSubjects(user.getDepartmentId(), user.getSemester(),
                new FirestoreCallback<List<SubjectModel>>() {
                    @Override
                    public void onSuccess(List<SubjectModel> subjects) {
                        if (subjects.isEmpty()) {
                            binding.subtitleText.setText(getString(R.string.no_academic_context_help));
                            showEmptyState(true, getString(R.string.no_academic_context));
                            return;
                        }
                        binding.subtitleText.setText(user.getDepartmentId() + " " + user.getSemester()
                                + " Section " + user.getSection() + " - " + subjects.size()
                                + " subjects connected.");
                        showEmptyState(false, "");
                    }

                    @Override
                    public void onError(String message) {
                        binding.subtitleText.setText(message);
                        showEmptyState(true, message);
                    }
                });
    }

    private void showEmptyState(boolean visible, String message) {
        if (binding == null) {
            return;
        }
        binding.emptyStateLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible && message != null && !message.trim().isEmpty()) {
            binding.emptyText.setText(message);
        }
    }
}
