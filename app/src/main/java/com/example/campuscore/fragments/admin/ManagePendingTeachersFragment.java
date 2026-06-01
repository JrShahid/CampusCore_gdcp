package com.example.campuscore.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.PendingTeachersAdapter;
import com.example.campuscore.databinding.FragmentManagePendingTeachersBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.PendingTeacherModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.repositories.TeachingAssignmentsRepository;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

public class ManagePendingTeachersFragment extends Fragment {
    private FragmentManagePendingTeachersBinding binding;
    private TeachingAssignmentsRepository repository;
    private AcademicStructureRepository academicRepository;
    private PendingTeachersAdapter adapter;
    private final List<PendingTeacherModel> teachers = new ArrayList<>();
    private PendingTeacherModel editingTeacher;

    public static ManagePendingTeachersFragment newInstance() {
        return new ManagePendingTeachersFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManagePendingTeachersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new TeachingAssignmentsRepository();
        academicRepository = new AcademicStructureRepository();
        adapter = new PendingTeachersAdapter(teachers, new PendingTeachersAdapter.OnTeacherActionListener() {
            @Override
            public void onEditTeacher(PendingTeacherModel teacher) {
                editTeacher(teacher);
            }

            @Override
            public void onDeleteTeacher(PendingTeacherModel teacher) {
                confirmDeleteTeacher(teacher);
            }
        });
        binding.teachersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.teachersRecyclerView.setAdapter(adapter);
        binding.saveButton.setOnClickListener(v -> saveTeacher());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadTeachers);
        loadDepartments();
        loadTeachers();
    }

    private void loadDepartments() {
        academicRepository.fetchActiveDepartments(new FirestoreCallback<List<DepartmentModel>>() {
            @Override
            public void onSuccess(List<DepartmentModel> data) {
                bindDropdown(academicRepository.departmentIds(data));
            }

            @Override
            public void onError(String message) {
                bindDropdown(new ArrayList<>());
            }
        });
    }

    private void saveTeacher() {
        clearErrors();
        PendingTeacherModel teacher = new PendingTeacherModel(
                text(binding.employeeInput.getText()).toUpperCase(),
                text(binding.nameInput.getText()),
                binding.departmentInput.getText().toString().trim(),
                text(binding.designationInput.getText()),
                editingTeacher == null ? "" : editingTeacher.getEmail(),
                true
        );
        if (editingTeacher != null) {
            teacher.setUid(editingTeacher.getUid());
            teacher.setEmail(editingTeacher.getEmail());
        }
        if (!validate(teacher)) {
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }
        setLoading(true);
        repository.savePendingTeacher(teacher, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                clearForm();
                SnackbarUtils.show(binding.rootLayout, getString(R.string.teacher_saved));
                loadTeachers();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void editTeacher(PendingTeacherModel teacher) {
        editingTeacher = teacher;
        binding.employeeInput.setText(teacher.getEmployeeId());
        binding.employeeInput.setEnabled(teacher.getUid().isEmpty());
        binding.nameInput.setText(teacher.getName());
        binding.departmentInput.setText(teacher.getPrimaryDepartmentId(), false);
        binding.designationInput.setText(teacher.getDesignation());
        binding.saveButton.setText(R.string.update_teacher);
    }

    private void confirmDeleteTeacher(PendingTeacherModel teacher) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_teacher_title)
                .setMessage(R.string.confirm_delete_teacher_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTeacher(teacher))
                .show();
    }

    private void deleteTeacher(PendingTeacherModel teacher) {
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }
        setLoading(true);
        repository.deletePendingTeacher(teacher, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                if (editingTeacher != null && editingTeacher.getEmployeeId().equals(teacher.getEmployeeId())) {
                    clearForm();
                }
                SnackbarUtils.show(binding.rootLayout, getString(R.string.teacher_deleted));
                loadTeachers();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void loadTeachers() {
        setLoading(true);
        repository.fetchPendingTeachers(new FirestoreCallback<List<PendingTeacherModel>>() {
            @Override
            public void onSuccess(List<PendingTeacherModel> data) {
                setLoading(false);
                adapter.submitList(data);
                binding.emptyText.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                binding.emptyText.setVisibility(View.VISIBLE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private boolean validate(PendingTeacherModel teacher) {
        boolean valid = true;
        if (ValidationUtils.isBlank(teacher.getEmployeeId())) {
            binding.employeeLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(teacher.getName())) {
            binding.nameLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(teacher.getDesignation())) {
            binding.designationLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(teacher.getPrimaryDepartmentId())) {
            binding.departmentLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        binding.employeeLayout.setError(null);
        binding.nameLayout.setError(null);
        binding.designationLayout.setError(null);
        binding.departmentLayout.setError(null);
    }

    private void clearForm() {
        editingTeacher = null;
        binding.employeeInput.setText("");
        binding.employeeInput.setEnabled(true);
        binding.nameInput.setText("");
        binding.designationInput.setText("");
        binding.saveButton.setText(R.string.add_teacher);
        clearErrors();
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.saveButton.setEnabled(!loading);
    }

    private void bindDropdown(List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, values);
        binding.departmentInput.setAdapter(adapter);
        if (!values.isEmpty() && binding.departmentInput.getText().toString().trim().isEmpty()) {
            binding.departmentInput.setText(values.get(0), false);
        }
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
