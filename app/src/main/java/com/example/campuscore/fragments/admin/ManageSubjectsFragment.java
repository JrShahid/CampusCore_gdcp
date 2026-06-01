package com.example.campuscore.fragments.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.SubjectsAdapter;
import com.example.campuscore.databinding.FragmentManageSubjectsBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageSubjectsFragment extends Fragment {
    private FragmentManageSubjectsBinding binding;
    private AcademicStructureRepository repository;
    private SubjectsAdapter adapter;
    private final List<SubjectModel> allSubjects = new ArrayList<>();
    private final List<SubjectModel> visibleSubjects = new ArrayList<>();
    private final List<DepartmentModel> departments = new ArrayList<>();
    private boolean hasLoadedOnce;

    public static ManageSubjectsFragment newInstance() {
        return new ManageSubjectsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManageSubjectsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new AcademicStructureRepository();
        adapter = new SubjectsAdapter(visibleSubjects, new SubjectsAdapter.OnSubjectActionListener() {
            @Override
            public void onEditSubject(SubjectModel subject) {
                openEditSubject(subject);
            }

            @Override
            public void onDeleteSubject(SubjectModel subject) {
                confirmDeleteSubject(subject);
            }
        });
        binding.subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.subjectsRecyclerView.setAdapter(adapter);

        setupSemesterFilter();
        loadDepartmentFilter();
        binding.addSubjectButton.setOnClickListener(v -> openAddSubject());
        binding.retryButton.setOnClickListener(v -> loadSubjects());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadSubjects);
        binding.searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });
        binding.departmentFilterInput.setOnItemClickListener((parent, view1, position, id) -> applyFilters());
        binding.semesterFilterInput.setOnItemClickListener((parent, view12, position, id) -> applyFilters());

        loadSubjects();
    }

    private void setupSemesterFilter() {
        bindDropdown(binding.semesterFilterInput, withAll(AcademicDataProvider.semesterValues()));
    }

    private void loadDepartmentFilter() {
        repository.fetchActiveDepartments(new FirestoreCallback<List<DepartmentModel>>() {
            @Override
            public void onSuccess(List<DepartmentModel> data) {
                departments.clear();
                departments.addAll(data);
                adapter.setDepartmentLabels(departmentLabelMap());
                bindDropdown(binding.departmentFilterInput, withAll(repository.departmentLabels(departments)));
            }

            @Override
            public void onError(String message) {
                bindDropdown(binding.departmentFilterInput, withAll(new ArrayList<>()));
            }
        });
    }

    private void loadSubjects() {
        if (!NetworkUtils.isOnline(requireContext())) {
            showEmpty(getString(R.string.error_no_internet), true);
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        setLoading(true);
        repository.fetchSubjects(new FirestoreCallback<List<SubjectModel>>() {
            @Override
            public void onSuccess(List<SubjectModel> data) {
                hasLoadedOnce = true;
                setLoading(false);
                allSubjects.clear();
                allSubjects.addAll(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showEmpty(getString(R.string.unable_load_subjects), true);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void applyFilters() {
        List<SubjectModel> filtered = repository.filterSubjects(
                allSubjects,
                text(binding.searchInput.getText()),
                repository.departmentIdFromLabel(binding.departmentFilterInput.getText().toString()),
                binding.semesterFilterInput.getText().toString()
        );
        adapter.submitList(filtered);
        binding.countText.setText(getString(R.string.subjects_count_format, filtered.size()));
        if (filtered.isEmpty()) {
            showEmpty(allSubjects.isEmpty() ? getString(R.string.no_subjects_available) : getString(R.string.no_matching_results), false);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.retryButton.setVisibility(View.GONE);
        }
    }

    private void openAddSubject() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditSubjectFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }

    private void openEditSubject(SubjectModel subject) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditSubjectFragment.newInstance(subject))
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteSubject(SubjectModel subject) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_subject_title)
                .setMessage(R.string.confirm_delete_subject_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteSubject(subject))
                .show();
    }

    private void deleteSubject(SubjectModel subject) {
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }
        setLoading(true);
        repository.deleteSubject(subject, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.subject_deleted));
                loadSubjects();
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
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.retryButton.setVisibility(View.GONE);
    }

    private void showEmpty(String message, boolean showRetry) {
        binding.emptyText.setText(message);
        binding.emptyText.setVisibility(View.VISIBLE);
        binding.retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
    }

    private void bindDropdown(com.google.android.material.textfield.MaterialAutoCompleteTextView view, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, values);
        view.setAdapter(adapter);
        if (!values.isEmpty()) {
            view.setText(values.get(0), false);
        }
    }

    private Map<String, String> departmentLabelMap() {
        Map<String, String> labels = new HashMap<>();
        for (DepartmentModel department : departments) {
            labels.put(department.getDepartmentId(), repository.departmentLabel(department));
        }
        return labels;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && hasLoadedOnce) {
            loadDepartmentFilter();
            loadSubjects();
        }
    }

    private List<String> withAll(List<String> values) {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.all));
        options.addAll(values);
        return options;
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }
}
