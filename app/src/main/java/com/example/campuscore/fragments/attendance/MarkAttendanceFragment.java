package com.example.campuscore.fragments.attendance;

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
import com.example.campuscore.adapters.AttendanceSessionHistoryAdapter;
import com.example.campuscore.adapters.StudentAttendanceAdapter;
import com.example.campuscore.databinding.FragmentMarkAttendanceBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.AttendanceModel;
import com.example.campuscore.models.AttendanceSessionModel;
import com.example.campuscore.models.StudentAttendanceItem;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.repositories.AttendanceRepository;
import com.example.campuscore.repositories.TeachingAssignmentsRepository;
import com.example.campuscore.services.TeachingAssignmentNormalizer;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarkAttendanceFragment extends Fragment {
    private FragmentMarkAttendanceBinding binding;
    private AttendanceRepository repository;
    private TeachingAssignmentsRepository assignmentsRepository;
    private final List<StudentAttendanceItem> studentItems = new ArrayList<>();
    private final List<AttendanceSessionModel> historyItems = new ArrayList<>();
    private StudentAttendanceAdapter studentAttendanceAdapter;
    private AttendanceSessionHistoryAdapter historyAdapter;
    private final List<TeachingAssignmentModel> currentAssignments = new ArrayList<>();
    private AttendanceSessionModel currentSession;
    private boolean attendanceEditable = true;

    public static MarkAttendanceFragment newInstance() {
        return new MarkAttendanceFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMarkAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new AttendanceRepository();
        assignmentsRepository = new TeachingAssignmentsRepository();
        setupDropdowns();
        setupLists();
        binding.loadStudentsButton.setOnClickListener(v -> loadStudents());
        binding.saveAttendanceButton.setText(R.string.review_attendance);
        binding.saveAttendanceButton.setOnClickListener(v -> reviewAttendance());
        binding.refreshHistoryButton.setOnClickListener(v -> loadHistory());
        binding.downloadAttendanceButton.setOnClickListener(v ->
                SnackbarUtils.show(binding.rootLayout, getString(R.string.available_soon)));
        binding.retryAssignmentsButton.setOnClickListener(v -> setupDropdowns());
        updateFilterContext();
        loadHistory();
    }

    private void setupDropdowns() {
        binding.subjectSpinner.setThreshold(0);
        binding.subjectSpinner.setOnClickListener(v -> binding.subjectSpinner.showDropDown());
        binding.subjectSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !currentAssignments.isEmpty()) {
                binding.subjectSpinner.showDropDown();
            }
        });
        binding.subjectSpinner.setOnItemClickListener((parent, view, position, id) -> {
            currentSession = null;
            attendanceEditable = true;
            studentAttendanceAdapter.submitList(new ArrayList<>());
            updateFilterContext();
            loadHistory();
        });
        binding.semesterSpinner.setVisibility(View.GONE);
        binding.departmentSpinner.setVisibility(View.GONE);
        binding.retryAssignmentsButton.setVisibility(View.GONE);
        assignmentsRepository.fetchTeacherAssignments(new FirestoreCallback<List<TeachingAssignmentModel>>() {
            @Override
            public void onSuccess(List<TeachingAssignmentModel> data) {
                bindAssignments(data);
            }

            @Override
            public void onError(String message) {
                binding.studentsEmptyText.setText(getString(R.string.assignment_data_synchronizing));
                binding.studentsEmptyText.setVisibility(View.VISIBLE);
                binding.retryAssignmentsButton.setVisibility(View.VISIBLE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void bindAssignments(List<TeachingAssignmentModel> data) {
        currentAssignments.clear();
        currentAssignments.addAll(data);
        List<String> labels = new ArrayList<>();
        for (TeachingAssignmentModel item : currentAssignments) {
            labels.add(assignmentsRepository.buildAssignmentLabel(item));
        }
        boolean hasAssignments = !currentAssignments.isEmpty();
        boolean needsSync = false;
        for (TeachingAssignmentModel item : currentAssignments) {
            if (TeachingAssignmentNormalizer.needsRepair(item)) {
                needsSync = true;
                break;
            }
        }
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                labels
        );
        binding.subjectSpinner.setAdapter(subjectAdapter);
        if (subjectAdapter.getCount() > 0) {
            binding.subjectSpinner.setText(subjectAdapter.getItem(0), false);
        }
        binding.subjectSpinner.setEnabled(hasAssignments);
        binding.loadStudentsButton.setEnabled(hasAssignments);
        binding.saveAttendanceButton.setEnabled(hasAssignments);
        if (!hasAssignments) {
            binding.studentsEmptyText.setText(getString(R.string.no_assigned_classes_yet));
        } else if (needsSync) {
            binding.studentsEmptyText.setText(getString(R.string.assignment_data_synchronizing));
        } else {
            binding.studentsEmptyText.setText(getString(R.string.no_students_found));
        }
        binding.studentsEmptyText.setVisibility(!hasAssignments || needsSync ? View.VISIBLE : View.GONE);
        binding.retryAssignmentsButton.setVisibility(!hasAssignments || needsSync ? View.VISIBLE : View.GONE);
        updateFilterContext();
    }

    private void setupLists() {
        studentAttendanceAdapter = new StudentAttendanceAdapter(studentItems);
        historyAdapter = new AttendanceSessionHistoryAdapter(historyItems);

        binding.studentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.studentsRecyclerView.setAdapter(studentAttendanceAdapter);

        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);
    }

    private void loadStudents() {
        if (currentAssignments.isEmpty()) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.no_assigned_classes_yet));
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.query_requires_network));
            return;
        }

        updateFilterContext();
        setFormLoading(true);
        assignmentsRepository.fetchStudentsForAssignment(selectedAssignment(), new FirestoreCallback<List<UserModel>>() {
            @Override
            public void onSuccess(List<UserModel> data) {
                setFormLoading(false);
                List<StudentAttendanceItem> nextItems = new ArrayList<>();
                for (UserModel user : data) {
                    nextItems.add(new StudentAttendanceItem(user, true));
                }
                loadExistingSessionState(nextItems);
            }

            @Override
            public void onError(String message) {
                setFormLoading(false);
                binding.studentsEmptyText.setVisibility(studentItems.isEmpty() ? View.VISIBLE : View.GONE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void loadExistingSessionState(List<StudentAttendanceItem> nextItems) {
        repository.fetchSessionForAssignmentDate(selectedAssignment(), repository.today(),
                new FirestoreCallback<AttendanceSessionModel>() {
                    @Override
                    public void onSuccess(AttendanceSessionModel session) {
                        currentSession = session;
                        if (session == null) {
                            attendanceEditable = true;
                            bindStudentItems(nextItems);
                            return;
                        }
                        attendanceEditable = session.isEditableNow();
                        repository.fetchRecordsForSession(session.getSessionId(), new FirestoreCallback<List<AttendanceModel>>() {
                            @Override
                            public void onSuccess(List<AttendanceModel> records) {
                                applyExistingStatuses(nextItems, records);
                                bindStudentItems(nextItems);
                            }

                            @Override
                            public void onError(String message) {
                                bindStudentItems(nextItems);
                                SnackbarUtils.show(binding.rootLayout, message);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        attendanceEditable = true;
                        bindStudentItems(nextItems);
                        SnackbarUtils.show(binding.rootLayout, message);
                    }
                });
    }

    private void applyExistingStatuses(List<StudentAttendanceItem> items, List<AttendanceModel> records) {
        Map<String, AttendanceModel> byIdentity = new HashMap<>();
        for (AttendanceModel record : records) {
            byIdentity.put(attendanceIdentity(record.getStudentUid(), record.getRegistrationNumber(), record.getRollNumber()), record);
        }
        for (StudentAttendanceItem item : items) {
            UserModel user = item.getUser();
            AttendanceModel record = byIdentity.get(attendanceIdentity(user.getUid(), user.getRegistrationNumber(), user.getRollNumber()));
            if (record != null) {
                item.setPresent(com.example.campuscore.utils.AttendanceConstants.STATUS_PRESENT.equalsIgnoreCase(record.getStatus()));
            }
        }
    }

    private void bindStudentItems(List<StudentAttendanceItem> nextItems) {
        studentAttendanceAdapter.setEditable(attendanceEditable);
        studentAttendanceAdapter.submitList(nextItems);
        binding.studentsEmptyText.setVisibility(studentItems.isEmpty() ? View.VISIBLE : View.GONE);
        binding.studentsInfoText.setText(studentItems.isEmpty()
                ? getString(R.string.no_students_found)
                : getString(R.string.students_loaded_format, studentItems.size()));
        updateSessionStatusText();
        setFormLoading(false);
    }

    private void updateSessionStatusText() {
        if (currentSession == null) {
            return;
        }
        if (attendanceEditable) {
            binding.studentsInfoText.setText(getString(R.string.attendance_editable_until,
                    formatDateTime(currentSession.getEditableUntil())));
        } else {
            binding.studentsInfoText.setText(getString(R.string.attendance_finalized) + "\n"
                    + getString(R.string.attendance_finalized_help));
        }
    }

    private void reviewAttendance() {
        if (currentAssignments.isEmpty()) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.no_assigned_classes_yet));
            return;
        }
        if (!attendanceEditable) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.attendance_finalized_help));
            return;
        }
        if (studentItems.isEmpty()) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.save_requires_students));
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.query_requires_network));
            return;
        }

        int present = 0;
        for (StudentAttendanceItem item : studentItems) {
            if (item.isPresent()) {
                present++;
            }
        }
        int absent = studentItems.size() - present;
        TeachingAssignmentModel assignment = selectedAssignment();
        String message = "Subject:\n" + assignment.getSubjectName()
                + "\n\nClass:\n" + assignment.getDepartmentId() + " Semester " + assignment.getSemester()
                + " Section " + assignment.getSection()
                + "\n\nDate:\n" + displayToday()
                + "\n\nPresent:\n" + present
                + "\n\nAbsent:\n" + absent
                + "\n\n" + getString(R.string.attendance_confirmation_message);
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.review_attendance)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm_submit_attendance, (dialog, which) -> saveAttendance())
                .show();
    }

    private void saveAttendance() {
        setFormLoading(true);
        repository.saveAttendance(selectedAssignment(), studentItems,
                new FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        setFormLoading(false);
                        SnackbarUtils.show(binding.rootLayout, getString(R.string.attendance_saved));
                        loadHistory();
                        loadStudents();
                    }

                    @Override
                    public void onError(String message) {
                        setFormLoading(false);
                        SnackbarUtils.show(binding.rootLayout, message);
                    }
                });
    }

    private void loadHistory() {
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.query_requires_network));
            return;
        }

        binding.historyProgressBar.setVisibility(View.VISIBLE);
        repository.fetchTeacherAttendanceSessions(new FirestoreCallback<List<AttendanceSessionModel>>() {
            @Override
            public void onSuccess(List<AttendanceSessionModel> data) {
                binding.historyProgressBar.setVisibility(View.GONE);
                List<AttendanceSessionModel> nextItems = new ArrayList<>();
                for (AttendanceSessionModel session : data) {
                    if (selectedSubject().equals(session.getSubjectCode())
                            && selectedSemester().equals(session.getSemester())
                            && selectedDepartment().equals(session.getDepartmentId())) {
                        nextItems.add(session);
                    }
                }
                historyAdapter.submitList(nextItems);
                binding.historyEmptyText.setVisibility(historyItems.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.historyProgressBar.setVisibility(View.GONE);
                binding.historyEmptyText.setVisibility(historyItems.isEmpty() ? View.VISIBLE : View.GONE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void setFormLoading(boolean loading) {
        binding.formProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        boolean canUseAttendance = !loading && !currentAssignments.isEmpty();
        binding.subjectSpinner.setEnabled(canUseAttendance);
        binding.loadStudentsButton.setEnabled(canUseAttendance);
        binding.saveAttendanceButton.setEnabled(canUseAttendance && attendanceEditable);
    }

    private void updateFilterContext() {
        binding.filterContextText.setText(getString(
                R.string.filter_context_format,
                selectedSubject(),
                selectedSemester(),
                selectedDepartment()
        ));
    }

    private String selectedSubject() {
        return selectedAssignment().getSubjectCode();
    }

    private String selectedSemester() {
        return selectedAssignment().getSemester();
    }

    private String selectedDepartment() {
        return selectedAssignment().getDepartmentId();
    }

    private TeachingAssignmentModel selectedAssignment() {
        String selected = binding.subjectSpinner.getText().toString().trim();
        for (TeachingAssignmentModel assignment : currentAssignments) {
            if (assignmentsRepository.buildAssignmentLabel(assignment).equals(selected)) {
                return assignment;
            }
        }
        return currentAssignments.isEmpty()
                ? new TeachingAssignmentModel()
                : currentAssignments.get(0);
    }

    private String attendanceIdentity(String uid, String registrationNumber, String rollNumber) {
        if (registrationNumber != null && !registrationNumber.trim().isEmpty()) {
            return "reg:" + registrationNumber.trim().toLowerCase(Locale.US);
        }
        if (uid != null && !uid.trim().isEmpty()) {
            return "uid:" + uid.trim().toLowerCase(Locale.US);
        }
        return "roll:" + (rollNumber == null ? "" : rollNumber.trim().toLowerCase(Locale.US));
    }

    private String displayToday() {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new java.util.Date());
    }

    private String formatDateTime(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) {
            return "";
        }
        return new SimpleDateFormat("dd MMM yyyy - hh:mm a", Locale.getDefault()).format(timestamp.toDate());
    }
}
