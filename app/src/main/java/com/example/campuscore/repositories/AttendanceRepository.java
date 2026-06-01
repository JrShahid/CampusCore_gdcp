package com.example.campuscore.repositories;

import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.AttendanceModel;
import com.example.campuscore.models.AttendanceSessionModel;
import com.example.campuscore.models.StudentAttendanceItem;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.AttendanceConstants;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.FirestoreCollections;
import com.example.campuscore.utils.FirestoreFields;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.Timestamp;
import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private static final long CORRECTION_WINDOW_MILLIS = 24L * 60L * 60L * 1000L;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public AttendanceRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void fetchStudentsForAttendance(String semester, String department, FirestoreCallback<List<UserModel>> callback) {
        firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo(FirestoreFields.ROLE, FirebaseUserRepository.ROLE_STUDENT)
                .whereEqualTo(FirestoreFields.SEMESTER, semester)
                .whereEqualTo(FirestoreFields.DEPARTMENT, department)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserModel> users = parseUsers(queryDocumentSnapshots);
                    String legacyDepartment = AcademicDataProvider.departmentNameForCode(department);
                    if (legacyDepartment.equals(department)) {
                        callback.onSuccess(users);
                        return;
                    }
                    firestore.collection(FirestoreCollections.USERS)
                            .whereEqualTo(FirestoreFields.ROLE, FirebaseUserRepository.ROLE_STUDENT)
                            .whereEqualTo(FirestoreFields.SEMESTER, semester)
                            .whereEqualTo(FirestoreFields.DEPARTMENT, legacyDepartment)
                            .get()
                            .addOnSuccessListener(legacySnapshots -> {
                                users.addAll(parseUsers(legacySnapshots));
                                callback.onSuccess(users);
                            })
                            .addOnFailureListener(error -> callback.onSuccess(users));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchStudentsForSubject(SubjectModel subject, FirestoreCallback<List<UserModel>> callback) {
        fetchCurrentTeacher(new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel teacher) {
                if (!teacher.getDepartmentId().equals(subject.getDepartmentId())
                        && !teacher.getDepartment().equals(subject.getDepartmentId())) {
                    callback.onError("This class is outside your academic scope.");
                    return;
                }
                if (teacher.hasAssignedSubjects() && !teacher.getAssignedSubjects().contains(subject.getSubjectCode())) {
                    callback.onError("This subject is not assigned to you.");
                    return;
                }
                firestore.collection(FirestoreCollections.USERS)
                        .whereEqualTo(FirestoreFields.ROLE, FirebaseUserRepository.ROLE_STUDENT)
                        .whereEqualTo(FirestoreFields.DEPARTMENT_ID, subject.getDepartmentId())
                        .whereEqualTo(FirestoreFields.SEMESTER, subject.getSemester())
                        .get()
                        .addOnSuccessListener(query -> callback.onSuccess(parseUsers(query)))
                        .addOnFailureListener(error -> callback.onError(readableError(error)));
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void saveAttendance(String subject, String semester, String department, List<StudentAttendanceItem> students,
                               FirestoreCallback<Void> callback) {
        saveAttendance(subject, semester, department, "", students, callback);
    }

    private void saveAttendance(String subject, String semester, String department, String assignmentId,
                                List<StudentAttendanceItem> students, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        WriteBatch batch = firestore.batch();
        for (StudentAttendanceItem item : students) {
            UserModel student = item.getUser();
            String attendanceId = buildAttendanceId(student.getUid(), subject, today);
            Map<String, Object> record = new HashMap<>();
            record.put(FirestoreFields.ATTENDANCE_ID, attendanceId);
            record.put(FirestoreFields.STUDENT_UID, student.getUid());
            record.put(FirestoreFields.STUDENT_NAME, student.getName());
            record.put(FirestoreFields.ROLL_NUMBER, student.getRollNumber());
            record.put(FirestoreFields.TEACHER_UID, currentUser.getUid());
            record.put(FirestoreFields.ASSIGNMENT_ID, assignmentId);
            record.put(FirestoreFields.SUBJECT, subject);
            record.put(FirestoreFields.SEMESTER, semester);
            record.put(FirestoreFields.DEPARTMENT, department);
            record.put(FirestoreFields.DATE, today);
            record.put(FirestoreFields.STATUS, item.isPresent()
                    ? AttendanceConstants.STATUS_PRESENT
                    : AttendanceConstants.STATUS_ABSENT);
            record.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
            batch.set(firestore.collection(FirestoreCollections.ATTENDANCE_RECORDS).document(attendanceId), record);
        }

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void saveAttendance(SubjectModel subject, List<StudentAttendanceItem> students, FirestoreCallback<Void> callback) {
        saveAttendance(subject.getSubjectCode(), subject.getSemester(), subject.getDepartmentId(), students, callback);
    }

    public void saveAttendance(TeachingAssignmentModel assignment, List<StudentAttendanceItem> students,
                               FirestoreCallback<Void> callback) {
        saveAttendanceSession(assignment, students, new FirestoreCallback<AttendanceSessionModel>() {
            @Override
            public void onSuccess(AttendanceSessionModel data) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void fetchSessionForAssignmentDate(TeachingAssignmentModel assignment, String date,
                                              FirestoreCallback<AttendanceSessionModel> callback) {
        String sessionId = buildSessionId(assignment.getAssignmentId(), date);
        firestore.collection(FirestoreCollections.ATTENDANCE_SESSIONS)
                .document(sessionId)
                .get()
                .addOnSuccessListener(document -> {
                    AttendanceSessionModel session = document.toObject(AttendanceSessionModel.class);
                    if (session != null && session.getSessionId().isEmpty()) {
                        session.setSessionId(document.getId());
                    }
                    if (session == null) {
                        Log.d(TAG, "No existing session for " + sessionId);
                    } else {
                        Log.d(TAG, "Duplicate session detected for " + sessionId);
                    }
                    callback.onSuccess(session);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchRecordsForSession(String sessionId, FirestoreCallback<List<AttendanceModel>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }
        firestore.collection(FirestoreCollections.ATTENDANCE_RECORDS)
                .whereEqualTo(FirestoreFields.SESSION_ID, sessionId)
                .whereEqualTo(FirestoreFields.TEACHER_UID, currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseAttendance(query)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void saveAttendanceSession(TeachingAssignmentModel assignment, List<StudentAttendanceItem> students,
                                      FirestoreCallback<AttendanceSessionModel> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        String today = today();
        fetchSessionForAssignmentDate(assignment, today, new FirestoreCallback<AttendanceSessionModel>() {
            @Override
            public void onSuccess(AttendanceSessionModel existingSession) {
                if (existingSession != null && !existingSession.isEditableNow()) {
                    Log.w(TAG, "Edit attempt after correction window for " + existingSession.getSessionId());
                    callback.onError("Attendance is finalized and can no longer be modified.");
                    return;
                }
                writeSessionAndRecords(assignment, students, existingSession, currentUser.getUid(), callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void writeSessionAndRecords(TeachingAssignmentModel assignment, List<StudentAttendanceItem> students,
                                        AttendanceSessionModel existingSession, String teacherUid,
                                        FirestoreCallback<AttendanceSessionModel> callback) {
        String date = today();
        String sessionId = existingSession == null
                ? buildSessionId(assignment.getAssignmentId(), date)
                : existingSession.getSessionId();
        int presentCount = 0;
        for (StudentAttendanceItem item : students) {
            if (item.isPresent()) {
                presentCount++;
            }
        }
        int absentCount = students.size() - presentCount;
        Timestamp editableUntil = existingSession != null && existingSession.getEditableUntil() != null
                ? existingSession.getEditableUntil()
                : new Timestamp(new Date(System.currentTimeMillis() + CORRECTION_WINDOW_MILLIS));

        Map<String, Object> session = new HashMap<>();
        session.put(FirestoreFields.SESSION_ID, sessionId);
        session.put(FirestoreFields.ASSIGNMENT_ID, assignment.getAssignmentId());
        session.put(FirestoreFields.TEACHER_UID, teacherUid);
        session.put(FirestoreFields.TEACHER_NAME, assignment.getTeacherName());
        session.put(FirestoreFields.EMPLOYEE_ID, assignment.getEmployeeId());
        session.put(FirestoreFields.SUBJECT_CODE, assignment.getSubjectCode());
        session.put(FirestoreFields.SUBJECT_NAME, assignment.getSubjectName());
        session.put(FirestoreFields.DEPARTMENT_ID, assignment.getDepartmentId());
        session.put(FirestoreFields.SEMESTER, assignment.getSemester());
        session.put(FirestoreFields.SECTION, assignment.getSection());
        session.put(FirestoreFields.DATE, date);
        session.put(FirestoreFields.TOTAL_STUDENTS, students.size());
        session.put(FirestoreFields.PRESENT_COUNT, presentCount);
        session.put(FirestoreFields.ABSENT_COUNT, absentCount);
        session.put(FirestoreFields.IS_LOCKED, true);
        session.put(FirestoreFields.EDITABLE_UNTIL, editableUntil);
        session.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
        if (existingSession == null) {
            session.put(FirestoreFields.SUBMITTED_AT, FieldValue.serverTimestamp());
            session.put(FirestoreFields.LAST_MODIFIED_AT, null);
            session.put(FirestoreFields.LAST_MODIFIED_BY, null);
        } else {
            if (existingSession.getSubmittedAt() != null) {
                session.put(FirestoreFields.SUBMITTED_AT, existingSession.getSubmittedAt());
            }
            session.put(FirestoreFields.LAST_MODIFIED_AT, FieldValue.serverTimestamp());
            session.put(FirestoreFields.LAST_MODIFIED_BY, teacherUid);
        }

        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection(FirestoreCollections.ATTENDANCE_SESSIONS).document(sessionId), session);
        for (StudentAttendanceItem item : students) {
            UserModel student = item.getUser();
            String attendanceId = buildAttendanceId(sessionId, student);
            Map<String, Object> record = new HashMap<>();
            record.put(FirestoreFields.ATTENDANCE_ID, attendanceId);
            record.put(FirestoreFields.SESSION_ID, sessionId);
            record.put(FirestoreFields.ASSIGNMENT_ID, assignment.getAssignmentId());
            record.put(FirestoreFields.STUDENT_UID, student.getUid());
            record.put(FirestoreFields.STUDENT_NAME, student.getName());
            record.put(FirestoreFields.ROLL_NUMBER, student.getRollNumber());
            record.put(FirestoreFields.REGISTRATION_NUMBER, student.getRegistrationNumber());
            record.put(FirestoreFields.TEACHER_UID, teacherUid);
            record.put(FirestoreFields.SUBJECT, assignment.getSubjectCode());
            record.put(FirestoreFields.SEMESTER, assignment.getSemester());
            record.put(FirestoreFields.DEPARTMENT, assignment.getDepartmentId());
            record.put(FirestoreFields.DATE, date);
            record.put(FirestoreFields.STATUS, item.isPresent()
                    ? AttendanceConstants.STATUS_PRESENT
                    : AttendanceConstants.STATUS_ABSENT);
            record.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
            batch.set(firestore.collection(FirestoreCollections.ATTENDANCE_RECORDS).document(attendanceId), record);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, existingSession == null ? "Created attendance session " + sessionId
                            : "Updated attendance session within correction window " + sessionId);
                    fetchSessionForAssignmentDate(assignment, date, callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchTeacherAttendanceSessions(FirestoreCallback<List<AttendanceSessionModel>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }
        firestore.collection(FirestoreCollections.ATTENDANCE_SESSIONS)
                .whereEqualTo(FirestoreFields.TEACHER_UID, currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseSessions(query)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void fetchCurrentTeacher(FirestoreCallback<UserModel> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }
        firestore.collection(FirestoreCollections.USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    UserModel teacher = snapshot.toObject(UserModel.class);
                    if (teacher == null) {
                        callback.onError("Teacher profile not found.");
                        return;
                    }
                    callback.onSuccess(teacher);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchTeacherAttendanceHistory(FirestoreCallback<List<AttendanceModel>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        firestore.collection(FirestoreCollections.ATTENDANCE_RECORDS)
                .whereEqualTo(FirestoreFields.TEACHER_UID, currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(parseAttendance(queryDocumentSnapshots)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchStudentAttendanceRecords(FirestoreCallback<List<AttendanceModel>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        firestore.collection(FirestoreCollections.ATTENDANCE_RECORDS)
                .whereEqualTo(FirestoreFields.STUDENT_UID, currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> callback.onSuccess(parseAttendance(queryDocumentSnapshots)))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private List<UserModel> parseUsers(QuerySnapshot snapshots) {
        List<UserModel> users = new ArrayList<>();
        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
            UserModel user = snapshot.toObject(UserModel.class);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    private List<AttendanceModel> parseAttendance(QuerySnapshot snapshots) {
        List<AttendanceModel> records = new ArrayList<>();
        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
            AttendanceModel model = snapshot.toObject(AttendanceModel.class);
            if (model != null) {
                records.add(model);
            }
        }
        Collections.sort(records, (first, second) -> second.getDate().compareTo(first.getDate()));
        return records;
    }

    private List<AttendanceSessionModel> parseSessions(QuerySnapshot snapshots) {
        List<AttendanceSessionModel> sessions = new ArrayList<>();
        for (DocumentSnapshot snapshot : snapshots.getDocuments()) {
            AttendanceSessionModel model = snapshot.toObject(AttendanceSessionModel.class);
            if (model != null) {
                if (model.getSessionId().isEmpty()) {
                    model.setSessionId(snapshot.getId());
                }
                sessions.add(model);
            }
        }
        Collections.sort(sessions, (first, second) -> second.getDate().compareTo(first.getDate()));
        return sessions;
    }

    private String buildAttendanceId(String studentUid, String subject, String date) {
        String normalizedSubject = subject.trim().replaceAll("[^A-Za-z0-9]+", "_");
        return studentUid + "_" + normalizedSubject + "_" + date;
    }

    private String buildAttendanceId(String sessionId, UserModel student) {
        String identity = !student.getRegistrationNumber().isEmpty()
                ? student.getRegistrationNumber()
                : student.getUid();
        return sessionId + "_" + identity.trim().replaceAll("[^A-Za-z0-9]+", "_");
    }

    public String buildSessionId(String assignmentId, String date) {
        return assignmentId.trim() + "_" + date;
    }

    public String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Attendance access was denied by Firestore rules.";
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return "Firestore query needs an index or additional setup. Please check the Firebase Console.";
            }
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Attendance request failed. Please try again."
                : message;
    }
}
