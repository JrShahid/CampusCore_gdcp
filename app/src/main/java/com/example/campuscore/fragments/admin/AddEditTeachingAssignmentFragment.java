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
import com.example.campuscore.databinding.FragmentAddEditTeachingAssignmentBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.repositories.TeachingAssignmentsRepository;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

public class AddEditTeachingAssignmentFragment extends Fragment {
    private static final String ARG_ID = "assignmentId";
    private static final String ARG_TEACHER_UID = "teacherUid";
    private static final String ARG_TEACHER_NAME = "teacherName";
    private static final String ARG_EMPLOYEE_ID = "employeeId";
    private static final String ARG_SUBJECT_CODE = "subjectCode";
    private static final String ARG_SUBJECT_NAME = "subjectName";
    private static final String ARG_DEPARTMENT_ID = "departmentId";
    private static final String ARG_DEPARTMENT_LABEL = "departmentLabel";
    private static final String ARG_SEMESTER = "semester";
    private static final String ARG_SECTION = "section";
    private static final String ARG_ACTIVE = "active";

    private FragmentAddEditTeachingAssignmentBinding binding;
    private TeachingAssignmentsRepository repository;
    private AcademicStructureRepository academicRepository;
    private final List<UserModel> teachers = new ArrayList<>();
    private final List<SubjectModel> subjects = new ArrayList<>();

    public static AddEditTeachingAssignmentFragment newInstance(@Nullable TeachingAssignmentModel assignment) {
        AddEditTeachingAssignmentFragment fragment = new AddEditTeachingAssignmentFragment();
        Bundle args = new Bundle();
        if (assignment != null) {
            args.putString(ARG_ID, assignment.getAssignmentId());
            args.putString(ARG_TEACHER_UID, assignment.getTeacherUid());
            args.putString(ARG_TEACHER_NAME, assignment.getTeacherName());
            args.putString(ARG_EMPLOYEE_ID, assignment.getEmployeeId());
            args.putString(ARG_SUBJECT_CODE, assignment.getSubjectCode());
            args.putString(ARG_SUBJECT_NAME, assignment.getSubjectName());
            args.putString(ARG_DEPARTMENT_ID, assignment.getDepartmentId());
            args.putString(ARG_DEPARTMENT_LABEL, assignment.getDepartmentLabel());
            args.putString(ARG_SEMESTER, assignment.getSemester());
            args.putString(ARG_SECTION, assignment.getSection());
            args.putBoolean(ARG_ACTIVE, assignment.isActive());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditTeachingAssignmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new TeachingAssignmentsRepository();
        academicRepository = new AcademicStructureRepository();
        bindDropdown(binding.semesterInput, AcademicDataProvider.semesterValues());
        bindDropdown(binding.sectionInput, AcademicDataProvider.sectionValues());
        binding.teacherInput.setOnItemClickListener((parent, v, position, id) -> selectedTeacher());
        binding.subjectInput.setOnItemClickListener((parent, v, position, id) -> selectedSubject());
        binding.departmentInput.setOnItemClickListener((parent, v, position, id) -> {
            binding.subjectInput.setText("", false);
            loadSubjects();
        });
        binding.semesterInput.setOnItemClickListener((parent, v, position, id) -> {
            binding.subjectInput.setText("", false);
            loadSubjects();
        });
        binding.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                binding.activeSwitch.setText(isChecked ? R.string.active : R.string.inactive));
        binding.saveButton.setOnClickListener(v -> saveAssignment());
        binding.cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        bindExisting();
        loadTeachers();
        loadDepartments();
    }

    private void loadTeachers() {
        repository.fetchAssignableTeachers(new FirestoreCallback<List<UserModel>>() {
            @Override
            public void onSuccess(List<UserModel> data) {
                teachers.clear();
                teachers.addAll(data);
                List<String> labels = new ArrayList<>();
                for (UserModel teacher : teachers) {
                    labels.add(teacherLabel(teacher));
                }
                bindDropdown(binding.teacherInput, labels);
                bindExistingTeacherLabelIfUnchanged();
            }

            @Override
            public void onError(String message) {
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void loadDepartments() {
        academicRepository.fetchActiveDepartmentsStrict(new FirestoreCallback<List<DepartmentModel>>() {
            @Override
            public void onSuccess(List<DepartmentModel> data) {
                bindDropdown(binding.departmentInput, academicRepository.departmentIds(data));
                loadSubjects();
            }

            @Override
            public void onError(String message) {
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void loadSubjects() {
        String requestedDepartment = binding.departmentInput.getText().toString().trim();
        String requestedSemester = binding.semesterInput.getText().toString().trim();
        academicRepository.fetchActiveSubjectsStrict(requestedDepartment,
                requestedSemester, new FirestoreCallback<List<SubjectModel>>() {
                    @Override
                    public void onSuccess(List<SubjectModel> data) {
                        if (!requestedDepartment.equals(binding.departmentInput.getText().toString().trim())
                                || !requestedSemester.equals(binding.semesterInput.getText().toString().trim())) {
                            return;
                        }
                        subjects.clear();
                        subjects.addAll(data);
                        List<String> labels = new ArrayList<>();
                        for (SubjectModel subject : subjects) {
                            labels.add(subject.toString());
                        }
                        bindDropdown(binding.subjectInput, labels);
                    }

                    @Override
                    public void onError(String message) {
                        SnackbarUtils.show(binding.rootLayout, message);
                    }
                });
    }

    private void bindExisting() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_ID)) {
            binding.screenTitle.setText(R.string.add_teaching_assignment);
            return;
        }
        binding.screenTitle.setText(R.string.edit_teaching_assignment);
        binding.teacherInput.setText(existingTeacherLabel(args), false);
        binding.departmentInput.setText(args.getString(ARG_DEPARTMENT_ID, ""), false);
        binding.semesterInput.setText(args.getString(ARG_SEMESTER, ""), false);
        binding.subjectInput.setText(args.getString(ARG_SUBJECT_CODE, "") + " - " + args.getString(ARG_SUBJECT_NAME, ""), false);
        binding.sectionInput.setText(args.getString(ARG_SECTION, ""), false);
        binding.activeSwitch.setChecked(args.getBoolean(ARG_ACTIVE, true));
    }

    private void bindExistingTeacherLabelIfUnchanged() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_ID)) {
            return;
        }
        String current = binding.teacherInput.getText().toString().trim();
        String fallback = existingTeacherFallback(args);
        if (current.equals(fallback)) {
            binding.teacherInput.setText(existingTeacherLabel(args), false);
        }
    }

    private void saveAssignment() {
        UserModel teacher = selectedTeacher();
        SubjectModel subject = selectedSubject();
        String selectionError = selectionError(teacher, subject);
        if (!selectionError.isEmpty()) {
            SnackbarUtils.show(binding.rootLayout, selectionError);
            return;
        }
        TeachingAssignmentModel assignment = new TeachingAssignmentModel(
                getArguments() == null ? "" : getArguments().getString(ARG_ID, ""),
                teacher.getUid(),
                teacher.getName(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                binding.departmentInput.getText().toString().trim(),
                binding.semesterInput.getText().toString().trim(),
                binding.sectionInput.getText().toString().trim(),
                binding.activeSwitch.isChecked()
        );
        assignment.setEmployeeId(teacher.getEmployeeId());
        String missingMessage = missingAssignmentFieldMessage(assignment);
        if (!missingMessage.isEmpty()) {
            SnackbarUtils.show(binding.rootLayout, missingMessage);
            return;
        }
        setLoading(true);
        repository.saveAssignment(assignment, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.assignment_saved));
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private String selectionError(@Nullable UserModel teacher, @Nullable SubjectModel subject) {
        if (teacher == null) {
            return teachers.isEmpty()
                    ? "Add a teacher record before creating assignments."
                    : "Select a valid teacher from the list.";
        }
        if (subject == null) {
            return subjects.isEmpty()
                    ? "No active subjects found for this department and semester."
                    : "Select a valid subject from the list.";
        }
        return "";
    }

    private String missingAssignmentFieldMessage(TeachingAssignmentModel assignment) {
        if (ValidationUtils.isBlank(assignment.getTeacherName())) {
            return "Selected teacher is missing a name.";
        }
        if (ValidationUtils.isBlank(assignment.getEmployeeId())) {
            return "Selected teacher is missing an employee ID. Update the teacher record first.";
        }
        if (ValidationUtils.isBlank(assignment.getDepartmentId())) {
            return "Select a valid department.";
        }
        if (ValidationUtils.isBlank(assignment.getSemester())) {
            return "Select a valid semester.";
        }
        if (ValidationUtils.isBlank(assignment.getSubjectCode())
                || ValidationUtils.isBlank(assignment.getSubjectName())) {
            return "Select a valid subject.";
        }
        if (ValidationUtils.isBlank(assignment.getSection())) {
            return "Select a valid section.";
        }
        return "";
    }

    private UserModel selectedTeacher() {
        String selected = binding.teacherInput.getText().toString().trim();
        for (UserModel teacher : teachers) {
            if (selected.equals(teacherLabel(teacher))
                    || (!teacher.getEmployeeId().isEmpty() && selected.contains(teacher.getEmployeeId()))
                    || (!teacher.getUid().isEmpty() && selected.contains(teacher.getName()))) {
                return teacher;
            }
        }
        return teachers.size() == 1 ? teachers.get(0) : null;
    }

    private String teacherLabel(UserModel teacher) {
        String signupState = teacher.getUid().isEmpty() ? "Pending" : "Active";
        String identity = teacher.getEmployeeId().isEmpty() ? "UID linked" : teacher.getEmployeeId();
        return teacher.getName() + " - " + identity + " (" + signupState + ")";
    }

    private String existingTeacherLabel(Bundle args) {
        String teacherUid = args.getString(ARG_TEACHER_UID, "");
        String employeeId = args.getString(ARG_EMPLOYEE_ID, "");
        for (UserModel teacher : teachers) {
            if ((!teacherUid.isEmpty() && teacherUid.equals(teacher.getUid()))
                    || (!employeeId.isEmpty() && employeeId.equalsIgnoreCase(teacher.getEmployeeId()))) {
                return teacherLabel(teacher);
            }
        }
        return existingTeacherFallback(args);
    }

    private String existingTeacherFallback(Bundle args) {
        String employeeId = args.getString(ARG_EMPLOYEE_ID, "");
        String teacherName = args.getString(ARG_TEACHER_NAME, "");
        return employeeId.isEmpty() ? teacherName : teacherName + " - " + employeeId;
    }

    private SubjectModel selectedSubject() {
        String selected = binding.subjectInput.getText().toString().trim();
        for (SubjectModel subject : subjects) {
            if (selected.startsWith(subject.getSubjectCode())) {
                return subject;
            }
        }
        return subjects.size() == 1 ? subjects.get(0) : null;
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
}
