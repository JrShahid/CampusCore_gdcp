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
import com.example.campuscore.databinding.FragmentAddEditStudentBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.repositories.StudentRepository;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.AppRoles;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddEditStudentFragment extends Fragment {
    private static final String ARG_UID = "uid";
    private static final String ARG_NAME = "name";
    private static final String ARG_EMAIL = "email";
    private static final String ARG_ROLL = "roll";
    private static final String ARG_REGISTRATION = "registration";
    private static final String ARG_DEPARTMENT = "department";
    private static final String ARG_SEMESTER = "semester";
    private static final String ARG_SECTION = "section";
    private static final String ARG_BATCH = "batch";

    private FragmentAddEditStudentBinding binding;
    private StudentRepository repository;
    private AcademicStructureRepository academicRepository;
    private String currentUid = "";

    public static AddEditStudentFragment newInstance(@Nullable UserModel student) {
        AddEditStudentFragment fragment = new AddEditStudentFragment();
        Bundle args = new Bundle();
        if (student != null) {
            args.putString(ARG_UID, student.getUid());
            args.putString(ARG_NAME, student.getName());
            args.putString(ARG_EMAIL, student.getEmail());
            args.putString(ARG_ROLL, student.getRollNumber());
            args.putString(ARG_REGISTRATION, student.getRegistrationNumber());
            args.putString(ARG_DEPARTMENT, student.getDepartmentId());
            args.putString(ARG_SEMESTER, student.getSemester());
            args.putString(ARG_SECTION, student.getSection());
            args.putString(ARG_BATCH, student.getBatch());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditStudentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new StudentRepository();
        academicRepository = new AcademicStructureRepository();
        setupDropdowns();
        bindExistingStudent();
        binding.saveButton.setOnClickListener(v -> saveStudent());
        binding.cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        binding.departmentInput.setOnItemClickListener((parent, view1, position, id) -> updateSubjectPreview());
        binding.semesterInput.setOnItemClickListener((parent, view12, position, id) -> updateSubjectPreview());
        updateSubjectPreview();
    }

    private void setupDropdowns() {
        bindDropdown(binding.departmentInput, departmentFallbackIds());
        academicRepository.fetchActiveDepartments(new FirestoreCallback<List<DepartmentModel>>() {
            @Override
            public void onSuccess(List<DepartmentModel> data) {
                String selected = binding.departmentInput.getText().toString().trim();
                bindDropdown(binding.departmentInput, academicRepository.departmentIds(data));
                if (!selected.isEmpty()) {
                    binding.departmentInput.setText(selected, false);
                }
                updateSubjectPreview();
            }

            @Override
            public void onError(String message) {
                updateSubjectPreview();
            }
        });
        bindDropdown(binding.semesterInput, AcademicDataProvider.semesterValues());
        bindDropdown(binding.sectionInput, AcademicDataProvider.sectionValues());
        bindDropdown(binding.roleInput, Collections.singletonList(AppRoles.STUDENT));
        binding.roleInput.setEnabled(false);
    }

    private void bindExistingStudent() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_UID)) {
            binding.screenTitle.setText(R.string.add_student);
            return;
        }

        binding.screenTitle.setText(R.string.edit_student);
        currentUid = args.getString(ARG_UID, "");
        binding.nameInput.setText(args.getString(ARG_NAME, ""));
        binding.emailInput.setText(args.getString(ARG_EMAIL, ""));
        binding.rollInput.setText(args.getString(ARG_ROLL, ""));
        binding.registrationInput.setText(args.getString(ARG_REGISTRATION, ""));
        binding.departmentInput.setText(args.getString(ARG_DEPARTMENT, ""), false);
        binding.semesterInput.setText(args.getString(ARG_SEMESTER, ""), false);
        binding.sectionInput.setText(args.getString(ARG_SECTION, ""), false);
        binding.batchInput.setText(args.getString(ARG_BATCH, ""));
    }

    private void saveStudent() {
        clearErrors();
        UserModel student = new UserModel(
                currentUid,
                text(binding.nameInput.getText()),
                text(binding.emailInput.getText()),
                AppRoles.STUDENT,
                binding.departmentInput.getText().toString().trim(),
                binding.semesterInput.getText().toString().trim(),
                text(binding.rollInput.getText()),
                binding.sectionInput.getText().toString().trim(),
                text(binding.batchInput.getText()),
                text(binding.registrationInput.getText())
        );
        student.setDepartmentId(binding.departmentInput.getText().toString().trim());

        if (!validate(student)) {
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }

        setLoading(true);
        repository.saveStudent(student, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.student_saved));
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private boolean validate(UserModel student) {
        boolean valid = true;
        if (ValidationUtils.isBlank(student.getName())) {
            binding.nameLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (!ValidationUtils.isBlank(student.getEmail()) && !ValidationUtils.isValidEmail(student.getEmail())) {
            binding.emailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        }
        if (ValidationUtils.isBlank(student.getRollNumber())) {
            binding.rollLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(student.getRegistrationNumber())) {
            binding.registrationLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(student.getBatch())) {
            binding.batchLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(student.getDepartmentId())) {
            binding.departmentLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(student.getSemester())) {
            binding.semesterLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        if (ValidationUtils.isBlank(student.getSection())) {
            binding.sectionLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        return valid;
    }

    private void updateSubjectPreview() {
        academicRepository.fetchActiveSubjects(
                binding.departmentInput.getText().toString().trim(),
                binding.semesterInput.getText().toString().trim(),
                new FirestoreCallback<List<SubjectModel>>() {
                    @Override
                    public void onSuccess(List<SubjectModel> data) {
                        List<String> subjects = new ArrayList<>();
                        for (SubjectModel subject : data) {
                            subjects.add(subject.toString());
                        }
                        binding.subjectPreviewText.setText(subjects.isEmpty()
                                ? getString(R.string.no_subjects_mapped)
                                : getString(R.string.subjects_auto_mapped_format, android.text.TextUtils.join(", ", subjects)));
                    }

                    @Override
                    public void onError(String message) {
                        binding.subjectPreviewText.setText(R.string.no_subjects_mapped);
                    }
                });
    }

    private void clearErrors() {
        binding.nameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.rollLayout.setError(null);
        binding.registrationLayout.setError(null);
        binding.batchLayout.setError(null);
        binding.departmentLayout.setError(null);
        binding.semesterLayout.setError(null);
        binding.sectionLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.saveButton.setEnabled(!loading);
        binding.cancelButton.setEnabled(!loading);
    }

    private void bindDropdown(com.google.android.material.textfield.MaterialAutoCompleteTextView view, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, values);
        view.setAdapter(adapter);
        if (!values.isEmpty()) {
            view.setText(values.get(0), false);
        }
    }

    private List<String> departmentFallbackIds() {
        return new ArrayList<>(AcademicDataProvider.defaultDepartmentCodeMap().values());
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
