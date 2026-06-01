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
import com.example.campuscore.adapters.StudentsAdapter;
import com.example.campuscore.databinding.FragmentManageStudentsBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.repositories.StudentRepository;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;

import java.util.ArrayList;
import java.util.List;

public class ManageStudentsFragment extends Fragment {
    private FragmentManageStudentsBinding binding;
    private StudentRepository repository;
    private AcademicStructureRepository academicRepository;
    private StudentsAdapter adapter;
    private final List<UserModel> allStudents = new ArrayList<>();
    private final List<UserModel> visibleStudents = new ArrayList<>();

    public static ManageStudentsFragment newInstance() {
        return new ManageStudentsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManageStudentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new StudentRepository();
        academicRepository = new AcademicStructureRepository();
        adapter = new StudentsAdapter(visibleStudents, new StudentsAdapter.OnStudentActionListener() {
            @Override
            public void onEditStudent(UserModel student) {
                openEditStudent(student);
            }

            @Override
            public void onDeleteStudent(UserModel student) {
                confirmDeleteStudent(student);
            }
        });
        binding.studentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.studentsRecyclerView.setAdapter(adapter);

        setupFilters();
        binding.addStudentButton.setOnClickListener(v -> openAddStudent());
        binding.retryButton.setOnClickListener(v -> loadStudents());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadStudents);
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadStudents();
    }

    private void setupFilters() {
        bindDropdown(binding.departmentFilterInput, withAll(new ArrayList<>(AcademicDataProvider.defaultDepartmentCodeMap().values())));
        academicRepository.fetchActiveDepartments(new FirestoreCallback<List<DepartmentModel>>() {
            @Override
            public void onSuccess(List<DepartmentModel> data) {
                bindDropdown(binding.departmentFilterInput, withAll(academicRepository.departmentIds(data)));
            }

            @Override
            public void onError(String message) {
            }
        });
        bindDropdown(binding.semesterFilterInput, withAll(AcademicDataProvider.semesterValues()));
        bindDropdown(binding.sectionFilterInput, withAll(AcademicDataProvider.sectionValues()));
        binding.departmentFilterInput.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        binding.semesterFilterInput.setOnItemClickListener((parent, view, position, id) -> applyFilters());
        binding.sectionFilterInput.setOnItemClickListener((parent, view, position, id) -> applyFilters());
    }

    private void loadStudents() {
        if (!NetworkUtils.isOnline(requireContext())) {
            showEmpty(getString(R.string.error_no_internet), true);
            binding.swipeRefreshLayout.setRefreshing(false);
            return;
        }

        setLoading(true);
        repository.fetchStudentsForCurrentScope(new FirestoreCallback<List<UserModel>>() {
            @Override
            public void onSuccess(List<UserModel> data) {
                setLoading(false);
                allStudents.clear();
                allStudents.addAll(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showEmpty(getString(R.string.unable_load_students), true);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void applyFilters() {
        List<UserModel> filtered = repository.filterStudents(
                allStudents,
                text(binding.searchInput.getText()),
                binding.departmentFilterInput.getText().toString(),
                binding.semesterFilterInput.getText().toString(),
                binding.sectionFilterInput.getText().toString()
        );
        adapter.submitList(filtered);
        binding.countText.setText(getString(R.string.students_count_format, filtered.size()));
        if (filtered.isEmpty()) {
            showEmpty(allStudents.isEmpty() ? getString(R.string.no_students_found) : getString(R.string.no_matching_students), false);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.retryButton.setVisibility(View.GONE);
        }
    }

    private void openAddStudent() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditStudentFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }

    private void openEditStudent(UserModel student) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditStudentFragment.newInstance(student))
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteStudent(UserModel student) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_student_title)
                .setMessage(R.string.confirm_delete_student_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteStudent(student))
                .show();
    }

    private void deleteStudent(UserModel student) {
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }
        setLoading(true);
        repository.deleteStudent(student, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.student_deleted));
                loadStudents();
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

    private List<String> withAll(List<String> values) {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.all));
        options.addAll(values);
        return options;
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
