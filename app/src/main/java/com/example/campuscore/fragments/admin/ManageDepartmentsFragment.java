package com.example.campuscore.fragments.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.DepartmentsAdapter;
import com.example.campuscore.databinding.FragmentManageDepartmentsBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;

import java.util.ArrayList;
import java.util.List;

public class ManageDepartmentsFragment extends Fragment {
    private FragmentManageDepartmentsBinding binding;
    private AcademicStructureRepository repository;
    private DepartmentsAdapter adapter;
    private final List<DepartmentModel> allDepartments = new ArrayList<>();
    private final List<DepartmentModel> visibleDepartments = new ArrayList<>();
    private boolean hasLoadedOnce;

    public static ManageDepartmentsFragment newInstance() {
        return new ManageDepartmentsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManageDepartmentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new AcademicStructureRepository();
        adapter = new DepartmentsAdapter(visibleDepartments, new DepartmentsAdapter.OnDepartmentActionListener() {
            @Override
            public void onEditDepartment(DepartmentModel department) {
                openEditDepartment(department);
            }

            @Override
            public void onDeleteDepartment(DepartmentModel department) {
                confirmDeleteDepartment(department);
            }
        });
        binding.departmentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.departmentsRecyclerView.setAdapter(adapter);

        binding.addDepartmentButton.setOnClickListener(v -> openAddDepartment());
        binding.retryButton.setOnClickListener(v -> loadDepartments());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadDepartments);
        binding.searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                applyFilters();
            }
        });

        loadDepartments();
    }

    private void loadDepartments() {
        if (!NetworkUtils.isOnline(requireContext())) {
            showEmpty(getString(R.string.error_no_internet), true);
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        setLoading(true);
        repository.fetchDepartments(new FirestoreCallback<List<DepartmentModel>>() {
            @Override
            public void onSuccess(List<DepartmentModel> data) {
                hasLoadedOnce = true;
                setLoading(false);
                allDepartments.clear();
                allDepartments.addAll(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showEmpty(getString(R.string.unable_load_departments), true);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void applyFilters() {
        List<DepartmentModel> filtered = repository.filterDepartments(allDepartments, text(binding.searchInput.getText()));
        adapter.submitList(filtered);
        binding.countText.setText(getString(R.string.departments_count_format, filtered.size()));
        if (filtered.isEmpty()) {
            showEmpty(allDepartments.isEmpty() ? getString(R.string.no_departments_found) : getString(R.string.no_matching_results), false);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.retryButton.setVisibility(View.GONE);
        }
    }

    private void openAddDepartment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditDepartmentFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }

    private void openEditDepartment(DepartmentModel department) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditDepartmentFragment.newInstance(department))
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteDepartment(DepartmentModel department) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_department_title)
                .setMessage(R.string.confirm_delete_department_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteDepartment(department))
                .show();
    }

    private void deleteDepartment(DepartmentModel department) {
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }
        setLoading(true);
        repository.deleteDepartment(department, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.department_deleted));
                loadDepartments();
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

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && hasLoadedOnce) {
            loadDepartments();
        }
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
