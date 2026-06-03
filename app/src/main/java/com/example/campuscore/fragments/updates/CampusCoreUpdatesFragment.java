package com.example.campuscore.fragments.updates;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.CampusCoreUpdatesAdapter;
import com.example.campuscore.databinding.FragmentCampusCoreUpdatesBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.FeedItemModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.network.UpdatesFeedProvider;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.repositories.CampusCoreUpdatesRepository;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.UpdatesConstants;
import com.example.campuscore.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

public class CampusCoreUpdatesFragment extends Fragment {
    private FragmentCampusCoreUpdatesBinding binding;
    private CampusCoreUpdatesRepository updatesRepository;
    private AcademicStructureRepository academicRepository;
    private FirebaseUserRepository userRepository;
    private CampusCoreUpdatesAdapter updatesAdapter;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final List<FeedItemModel> allUpdates = new ArrayList<>();
    private final List<FeedItemModel> visibleUpdates = new ArrayList<>();
    private String currentDepartment = "CS";
    private String currentSemester = "Semester 1";

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadUpdates(false);
            refreshHandler.postDelayed(this, UpdatesConstants.AUTO_REFRESH_INTERVAL_MS);
        }
    };

    public static CampusCoreUpdatesFragment newInstance() {
        return new CampusCoreUpdatesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCampusCoreUpdatesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        updatesRepository = new CampusCoreUpdatesRepository();
        academicRepository = new AcademicStructureRepository();
        userRepository = new FirebaseUserRepository();
        updatesAdapter = new CampusCoreUpdatesAdapter(visibleUpdates, this::openArticle);


        binding.updatesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.updatesRecyclerView.setAdapter(updatesAdapter);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> loadUpdates(true));
        binding.retryButton.setOnClickListener(v -> loadUpdates(true));
        binding.categoryFilterSpinner.setOnItemClickListener((parent, view1, position, id) -> applyFilters());
        binding.subjectFilterSpinner.setOnItemClickListener((parent, view12, position, id) -> applyFilters());

        setupCategoryFilter();
        loadUserContext();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHandler.removeCallbacks(autoRefreshRunnable);
        refreshHandler.postDelayed(autoRefreshRunnable, UpdatesConstants.AUTO_REFRESH_INTERVAL_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    public void onDestroyView() {
        refreshHandler.removeCallbacks(autoRefreshRunnable);
        if (updatesRepository != null) {
            updatesRepository.shutdown();
        }
        binding = null;
        super.onDestroyView();
    }

    private void loadUserContext() {
        userRepository.fetchCurrentUser(new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel user) {
                if (!ValidationUtils.isBlank(user.getDepartmentId())) {
                    currentDepartment = user.getDepartmentId();
                }
                if (!ValidationUtils.isBlank(user.getSemester())) {
                    currentSemester = user.getSemester();
                }
                setupSubjectFilter(currentDepartment, currentSemester);
                loadUpdates(true);
            }

            @Override
            public void onError(String message) {
                setupSubjectFilter(currentDepartment, currentSemester);
                SnackbarUtils.show(binding.rootLayout, message);
                loadUpdates(true);
            }
        });
    }

    private void setupCategoryFilter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                UpdatesFeedProvider.categories()
        );
        binding.categoryFilterSpinner.setAdapter(adapter);
        binding.categoryFilterSpinner.setText(UpdatesConstants.CATEGORY_ALL, false);
    }

    private void setupSubjectFilter(String departmentId, String semester) {
        academicRepository.fetchActiveSubjects(departmentId, semester, new FirestoreCallback<List<SubjectModel>>() {
            @Override
            public void onSuccess(List<SubjectModel> data) {
                List<String> subjects = new ArrayList<>();
                subjects.add(getString(R.string.all_subjects));
                for (SubjectModel item : data) {
                    subjects.add(item.toString());
                }
                bindSubjectFilter(subjects);
            }

            @Override
            public void onError(String message) {
                bindSubjectFilter(fallbackSubjectOptions(departmentId));
            }
        });
    }

    private List<String> fallbackSubjectOptions(String departmentId) {
        List<String> subjects = new ArrayList<>();
        subjects.add(getString(R.string.all_subjects));
        for (AcademicDataProvider.SubjectItem item : AcademicDataProvider.subjectsForDepartment(
                AcademicDataProvider.departmentNameForCode(departmentId))) {
            subjects.add(item.toString());
        }
        return subjects;
    }

    private void bindSubjectFilter(List<String> subjects) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                subjects
        );
        binding.subjectFilterSpinner.setAdapter(adapter);
        binding.subjectFilterSpinner.setText(getString(R.string.all_subjects), false);
    }

//    private List<String> fallbackSubjectOptions(String departmentId) {
//        List<String> subjects = new ArrayList<>();
//        subjects.add(getString(R.string.all_subjects));
//        for (AcademicDataProvider.SubjectItem item : AcademicDataProvider.subjectsForDepartment(
//                AcademicDataProvider.departmentNameForCode(departmentId))) {
//            subjects.add(item.toString());
//        }
//        return subjects;
//    }

//    private void bindSubjectFilter(List<String> subjects) {
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(
//                requireContext(),
//                android.R.layout.simple_list_item_1,
//                subjects
//        );
//        binding.subjectFilterSpinner.setAdapter(adapter);
//        binding.subjectFilterSpinner.setText(getString(R.string.all_subjects), false);
//    }

    private void loadUpdates(boolean showLoading) {
        if (binding == null) {
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            binding.swipeRefreshLayout.setRefreshing(false);
            showEmptyState(getString(R.string.updates_no_internet), true);
            SnackbarUtils.show(binding.rootLayout, getString(R.string.updates_no_internet));
            return;
        }

        if (showLoading) {
            setLoading(true);
        }
        updatesRepository.fetchUpdates(currentDepartment, new FirestoreCallback<List<FeedItemModel>>() {
            @Override
            public void onSuccess(List<FeedItemModel> data) {
                if (binding == null) {
                    return;
                }
                setLoading(false);
                allUpdates.clear();
                allUpdates.addAll(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                if (binding == null) {
                    return;
                }
                setLoading(false);
                showEmptyState(getString(R.string.updates_fetch_error), true);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void applyFilters() {
        if (binding == null) {
            return;
        }
        String category = binding.categoryFilterSpinner.getText().toString().trim();
        String subject = binding.subjectFilterSpinner.getText().toString().trim();
        List<FeedItemModel> filtered = updatesRepository.filterUpdates(allUpdates, category, subject);
        updatesAdapter.submitList(filtered);
        binding.countText.setText(getString(R.string.updates_count_format, filtered.size()));
        if (filtered.isEmpty()) {
            showEmptyState(getString(R.string.no_updates_available), false);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.retryButton.setVisibility(View.GONE);
        }
    }

    private void openArticle(FeedItemModel item) {
        if (ValidationUtils.isBlank(item.getArticleUrl())
                || !item.getArticleUrl().startsWith("http")) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.broken_article_link));
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getArticleUrl()));
            startActivity(intent);
        } catch (Exception error) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.broken_article_link));
        }
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.retryButton.setVisibility(View.GONE);
        if (loading) {
            binding.emptyText.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message, boolean showRetry) {
        binding.emptyText.setText(message);
        binding.emptyText.setVisibility(View.VISIBLE);
        binding.retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }
}
