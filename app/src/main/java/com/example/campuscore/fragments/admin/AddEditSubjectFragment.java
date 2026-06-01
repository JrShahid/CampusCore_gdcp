package com.example.campuscore.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campuscore.R;
import com.example.campuscore.databinding.FragmentAddEditSubjectBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

public class AddEditSubjectFragment extends Fragment {
    private static final String ARG_CODE = "subjectCode";
    private static final String ARG_NAME = "subjectName";
    private static final String ARG_DEPARTMENT_ID = "departmentId";
    private static final String ARG_SEMESTER = "semester";
    private static final String ARG_ACTIVE = "active";

    private FragmentAddEditSubjectBinding binding;
    private AcademicStructureRepository repository;
    private final List<DepartmentModel> departments = new ArrayList<>();
    private String originalSubjectCode = "";

    public static AddEditSubjectFragment newInstance(@Nullable SubjectModel subject) {
        AddEditSubjectFragment fragment = new AddEditSubjectFragment();
        Bundle args = new Bundle();
        if (subject != null) {
            args.putString(ARG_CODE, subject.getSubjectCode());
            args.putString(ARG_NAME, subject.getSubjectName());
            args.putString(ARG_DEPARTMENT_ID, subject.getDepartmentId());
            args.putString(ARG_SEMESTER, subject.getSemester());
            args.putBoolean(ARG_ACTIVE, subject.isActive());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditSubjectBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new AcademicStructureRepository();
        setupDropdowns();
        bindExistingSubject();
        binding.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateActiveText());
        updateActiveText();
        binding.saveButton.setOnClickListener(v -> saveSubject());
        binding.cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void setupDropdowns() {
        bindDropdown(binding.semesterInput, AcademicDataProvider.semesterValues());
        repository.fetchActiveDepartments(new FirestoreCallback<List<DepartmentModel>>() {
            @Override
            public void onSuccess(List<DepartmentModel> data) {
                departments.clear();
                departments.addAll(data);
                bindDropdown(binding.departmentInput, repository.departmentLabels(departments));
                bindExistingSubject();
            }

            @Override
            public void onError(String message) {
                bindDropdown(binding.departmentInput, new ArrayList<>());
            }
        });
    }

    private void bindExistingSubject() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_CODE)) {
            binding.screenTitle.setText(R.string.add_subject);
            return;
        }
        binding.screenTitle.setText(R.string.edit_subject);
        originalSubjectCode = args.getString(ARG_CODE, "");
        binding.subjectCodeInput.setText(originalSubjectCode);
        binding.subjectCodeInput.setEnabled(false);
        binding.subjectNameInput.setText(args.getString(ARG_NAME, ""));
        binding.departmentInput.setText(repository.departmentLabelForId(departments, args.getString(ARG_DEPARTMENT_ID, "")), false);
        binding.semesterInput.setText(args.getString(ARG_SEMESTER, ""), false);
        binding.activeSwitch.setChecked(args.getBoolean(ARG_ACTIVE, true));
        updateActiveText();
    }

    private void saveSubject() {
        clearErrors();
        SubjectModel subject = new SubjectModel(
                text(binding.subjectCodeInput.getText()).toUpperCase(),
                text(binding.subjectNameInput.getText()),
                repository.departmentIdFromLabel(binding.departmentInput.getText().toString().trim()),
                binding.semesterInput.getText().toString().trim(),
                binding.activeSwitch.isChecked()
        );
        if (!validate(subject)) {
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }

        setLoading(true);
        repository.saveSubject(subject, originalSubjectCode, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.subject_saved));
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private boolean validate(SubjectModel subject) {
        boolean valid = true;
        if (ValidationUtils.isBlank(subject.getSubjectCode())) {
            binding.subjectCodeLayout.setError(getString(R.string.error_required));
            valid = false;
        } else if (!subject.getSubjectCode().matches("[A-Z0-9_-]{2,20}")) {
            binding.subjectCodeLayout.setError(getString(R.string.error_invalid_academic_code));
            valid = false;
        }
        if (ValidationUtils.isBlank(subject.getSubjectName())) {
            binding.subjectNameLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(subject.getDepartmentId())) {
            binding.departmentLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (!AcademicDataProvider.semesterValues().contains(subject.getSemester())) {
            binding.semesterLayout.setError(getString(R.string.invalid_semester_selection));
            valid = false;
        }
        return valid;
    }

    private void updateActiveText() {
        binding.activeSwitch.setText(binding.activeSwitch.isChecked()
                ? getString(R.string.active)
                : getString(R.string.inactive));
    }

    private void clearErrors() {
        binding.subjectCodeLayout.setError(null);
        binding.subjectNameLayout.setError(null);
        binding.departmentLayout.setError(null);
        binding.semesterLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.saveButton.setEnabled(!loading);
        binding.cancelButton.setEnabled(!loading);
    }

    private void bindDropdown(com.google.android.material.textfield.MaterialAutoCompleteTextView view, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, values);
        view.setAdapter(adapter);
        if (!values.isEmpty() && view.getText().toString().trim().isEmpty()) {
            view.setText(values.get(0), false);
        }
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
