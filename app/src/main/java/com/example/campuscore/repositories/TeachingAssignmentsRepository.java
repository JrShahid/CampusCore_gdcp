package com.example.campuscore.repositories;

import androidx.annotation.NonNull;

import android.util.Log;

import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.PendingTeacherModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.services.TeachingAssignmentNormalizer;
import com.example.campuscore.utils.AppRoles;
import com.example.campuscore.utils.FirestoreCollections;
import com.example.campuscore.utils.FirestoreFields;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeachingAssignmentsRepository {
    private static final String TAG = "TeachingAssignmentsRepo";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public TeachingAssignmentsRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void fetchTeacherAssignments(FirestoreCallback<List<TeachingAssignmentModel>> callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .whereEqualTo(FirestoreFields.TEACHER_UID, auth.getCurrentUser().getUid())
                .whereEqualTo(FirestoreFields.IS_ACTIVE, true)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseAssignments(query, true)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "teaching assignments")));
    }

    public void fetchAssignments(FirestoreCallback<List<TeachingAssignmentModel>> callback) {
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseAssignments(query, true)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "teaching assignments")));
    }

    public void saveAssignment(TeachingAssignmentModel assignment, FirestoreCallback<Void> callback) {
        validateAndNormalizeAssignment(assignment, new FirestoreCallback<TeachingAssignmentModel>() {
            @Override
            public void onSuccess(TeachingAssignmentModel normalizedAssignment) {
                writeAssignment(normalizedAssignment, callback);
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "Assignment validation failed: " + message);
                callback.onError(message);
            }
        });
    }

    private void writeAssignment(TeachingAssignmentModel assignment, FirestoreCallback<Void> callback) {
        String assignmentId = assignment.getAssignmentId().isEmpty()
                ? buildAssignmentId(assignment)
                : assignment.getAssignmentId();
        assignment.setAssignmentId(assignmentId);
        if (auth.getCurrentUser() != null) {
            assignment.setAssignedBy(auth.getCurrentUser().getUid());
        }
        Map<String, Object> values = new HashMap<>();
        values.put(FirestoreFields.ASSIGNMENT_ID, assignment.getAssignmentId());
        values.put(FirestoreFields.TEACHER_UID, assignment.getTeacherUid());
        values.put(FirestoreFields.EMPLOYEE_ID, assignment.getEmployeeId());
        values.put(FirestoreFields.TEACHER_NAME, assignment.getTeacherName());
        values.put(FirestoreFields.SUBJECT_CODE, assignment.getSubjectCode());
        values.put(FirestoreFields.SUBJECT_NAME, assignment.getSubjectName());
        values.put(FirestoreFields.DEPARTMENT_ID, assignment.getDepartmentId());
        values.put(FirestoreFields.DEPARTMENT_LABEL, assignment.getDepartmentLabel());
        values.put(FirestoreFields.SEMESTER, assignment.getSemester());
        values.put(FirestoreFields.SECTION, assignment.getSection());
        values.put(FirestoreFields.IS_ACTIVE, assignment.isActive());
        values.put(FirestoreFields.ASSIGNED_BY, assignment.getAssignedBy());
        values.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());

        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .document(assignmentId)
                .set(values)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Saved normalized teaching assignment " + assignmentId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "teaching assignment")));
    }

    private void validateAndNormalizeAssignment(TeachingAssignmentModel assignment,
                                                FirestoreCallback<TeachingAssignmentModel> callback) {
        assignment.setEmployeeId(TeachingAssignmentNormalizer.normalizeEmployeeId(assignment.getEmployeeId()));
        String basicError = TeachingAssignmentNormalizer.validateComplete(assignment);
        if (!basicError.isEmpty()) {
            callback.onError(basicError);
            return;
        }

        loadTeacherForAssignment(assignment, new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel teacher) {
                loadSubjectForAssignment(assignment, new FirestoreCallback<SubjectModel>() {
                    @Override
                    public void onSuccess(SubjectModel subject) {
                        loadDepartmentForAssignment(assignment.getDepartmentId(), new FirestoreCallback<DepartmentModel>() {
                            @Override
                            public void onSuccess(DepartmentModel department) {
                                if (!subject.getDepartmentId().equalsIgnoreCase(department.getDepartmentId())) {
                                    callback.onError("Selected subject does not belong to the selected department.");
                                    return;
                                }
                                if (!subject.getSemester().equalsIgnoreCase(assignment.getSemester())) {
                                    callback.onError("Selected subject does not belong to the selected semester.");
                                    return;
                                }
                                TeachingAssignmentModel normalized = TeachingAssignmentNormalizer.normalizeForSave(
                                        assignment, teacher, subject, department);
                                normalized.setSection(assignment.getSection().trim().toUpperCase(Locale.US));
                                String completenessError = TeachingAssignmentNormalizer.validateComplete(normalized);
                                if (!completenessError.isEmpty()) {
                                    callback.onError(completenessError);
                                    return;
                                }
                                callback.onSuccess(normalized);
                            }

                            @Override
                            public void onError(String message) {
                                callback.onError(message);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void loadTeacherForAssignment(TeachingAssignmentModel assignment, FirestoreCallback<UserModel> callback) {
        if (!assignment.getTeacherUid().isEmpty()) {
            firestore.collection(FirestoreCollections.USERS)
                    .document(assignment.getTeacherUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        UserModel teacher = document.toObject(UserModel.class);
                        if (document.exists() && isCompleteTeacher(teacher)) {
                            if (teacher.getUid().isEmpty()) {
                                teacher.setUid(document.getId());
                            }
                            callback.onSuccess(teacher);
                            return;
                        }
                        Log.w(TAG, "Teacher UID resolution incomplete for assignment save: " + assignment.getTeacherUid());
                        loadPendingTeacherForAssignment(assignment, callback);
                    })
                    .addOnFailureListener(error -> callback.onError(readableError(error, "teacher profile")));
            return;
        }
        loadPendingTeacherForAssignment(assignment, callback);
    }

    private void loadPendingTeacherForAssignment(TeachingAssignmentModel assignment, FirestoreCallback<UserModel> callback) {
        String employeeId = TeachingAssignmentNormalizer.normalizeEmployeeId(assignment.getEmployeeId());
        if (employeeId.isEmpty()) {
            callback.onError("Teacher employee ID is required for teaching assignments.");
            return;
        }
        firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                .document(employeeId)
                .get()
                .addOnSuccessListener(document -> {
                    PendingTeacherModel pending = document.toObject(PendingTeacherModel.class);
                    if (!document.exists() || pending == null || !pending.isActive()) {
                        callback.onError("Teacher record does not exist or is inactive.");
                        return;
                    }
                    if (pending.getName().trim().isEmpty() || pending.getEmployeeId().trim().isEmpty()) {
                        callback.onError("Teacher record is missing name or employee ID.");
                        return;
                    }
                    UserModel teacher = new UserModel(
                            pending.getUid(),
                            pending.getName(),
                            pending.getEmail(),
                            AppRoles.TEACHER,
                            pending.getPrimaryDepartmentId(),
                            "",
                            ""
                    );
                    teacher.setEmployeeId(pending.getEmployeeId());
                    teacher.setPrimaryDepartmentId(pending.getPrimaryDepartmentId());
                    teacher.setDepartmentId(pending.getPrimaryDepartmentId());
                    teacher.setDesignation(pending.getDesignation());
                    callback.onSuccess(teacher);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "pending teacher")));
    }

    private void loadSubjectForAssignment(TeachingAssignmentModel assignment, FirestoreCallback<SubjectModel> callback) {
        if (assignment.getSubjectCode().isEmpty()) {
            callback.onError("Subject code is required.");
            return;
        }
        firestore.collection(FirestoreCollections.SUBJECTS)
                .document(assignment.getSubjectCode().trim().toUpperCase(Locale.US))
                .get()
                .addOnSuccessListener(document -> {
                    SubjectModel subject = document.toObject(SubjectModel.class);
                    if (!document.exists() || subject == null || !subject.isActive()) {
                        callback.onError("Subject record does not exist or is inactive.");
                        return;
                    }
                    if (subject.getSubjectCode().isEmpty()) {
                        subject.setSubjectCode(document.getId());
                    }
                    if (subject.getSubjectName().trim().isEmpty()) {
                        callback.onError("Subject record is missing a subject name.");
                        return;
                    }
                    callback.onSuccess(subject);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "subject")));
    }

    private void loadDepartmentForAssignment(String departmentId, FirestoreCallback<DepartmentModel> callback) {
        if (departmentId == null || departmentId.trim().isEmpty()) {
            callback.onError("Department is required.");
            return;
        }
        firestore.collection(FirestoreCollections.DEPARTMENTS)
                .document(departmentId.trim())
                .get()
                .addOnSuccessListener(document -> {
                    DepartmentModel department = document.toObject(DepartmentModel.class);
                    if (!document.exists() || department == null || !department.isActive()) {
                        callback.onError("Department record does not exist or is inactive.");
                        return;
                    }
                    if (department.getDepartmentId().isEmpty()) {
                        department.setDepartmentId(document.getId());
                    }
                    callback.onSuccess(department);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "department")));
    }

    private boolean isCompleteTeacher(UserModel teacher) {
        return teacher != null
                && AppRoles.TEACHER.equalsIgnoreCase(teacher.getRole())
                && !teacher.getName().trim().isEmpty()
                && !teacher.getEmployeeId().trim().isEmpty();
    }

    public void deleteAssignment(TeachingAssignmentModel assignment, FirestoreCallback<Void> callback) {
        String assignmentId = assignment.getAssignmentId();
        if (assignmentId.isEmpty()) {
            callback.onError("Teaching assignment ID is required.");
            return;
        }
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .document(assignmentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> callback.onError(readableError(error, "teaching assignment")));
    }

    public void repairTeachingAssignments(FirestoreCallback<Integer> callback) {
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .get()
                .addOnSuccessListener(query -> {
                    List<TeachingAssignmentModel> assignments = parseAssignments(query, false);
                    if (assignments.isEmpty()) {
                        callback.onSuccess(0);
                        return;
                    }
                    final int[] completed = {0};
                    final int[] repaired = {0};
                    for (TeachingAssignmentModel assignment : assignments) {
                        repairAssignmentInBackground(assignment, new FirestoreCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean didRepair) {
                                if (Boolean.TRUE.equals(didRepair)) {
                                    repaired[0]++;
                                }
                                completed[0]++;
                                if (completed[0] == assignments.size()) {
                                    callback.onSuccess(repaired[0]);
                                }
                            }

                            @Override
                            public void onError(String message) {
                                Log.w(TAG, "Assignment repair failed: " + message);
                                completed[0]++;
                                if (completed[0] == assignments.size()) {
                                    callback.onSuccess(repaired[0]);
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "teaching assignment repair")));
    }

    public void fetchPendingTeachers(FirestoreCallback<List<PendingTeacherModel>> callback) {
        firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parsePendingTeachers(query)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "pending teachers")));
    }

    public void savePendingTeacher(PendingTeacherModel teacher, FirestoreCallback<Void> callback) {
        String employeeId = teacher.getEmployeeId().trim().toUpperCase(Locale.US);
        teacher.setEmployeeId(employeeId);
        Map<String, Object> values = new HashMap<>();
        values.put(FirestoreFields.EMPLOYEE_ID, teacher.getEmployeeId());
        values.put(FirestoreFields.NAME, teacher.getName());
        values.put(FirestoreFields.PRIMARY_DEPARTMENT_ID, teacher.getPrimaryDepartmentId());
        values.put(FirestoreFields.DESIGNATION, teacher.getDesignation());
        values.put(FirestoreFields.EMAIL, teacher.getEmail());
        values.put(FirestoreFields.UID, teacher.getUid());
        values.put(FirestoreFields.IS_ACTIVE, teacher.isActive());
        values.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());

        firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                .document(employeeId)
                .get()
                .addOnSuccessListener(existing -> {
                    PendingTeacherModel existingTeacher = existing.toObject(PendingTeacherModel.class);
                    if (existing.exists()
                            && existingTeacher != null
                            && !existingTeacher.getUid().isEmpty()
                            && !existingTeacher.getUid().equals(teacher.getUid())) {
                        callback.onError("This teacher is already linked to an account.");
                        return;
                    }
                    firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                            .document(employeeId)
                            .set(values)
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(error -> callback.onError(readableError(error, "pending teacher")));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "pending teacher")));
    }

    public void deletePendingTeacher(PendingTeacherModel teacher, FirestoreCallback<Void> callback) {
        String employeeId = teacher.getEmployeeId().trim().toUpperCase(Locale.US);
        firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                .document(employeeId)
                .get()
                .addOnSuccessListener(existing -> {
                    PendingTeacherModel existingTeacher = existing.toObject(PendingTeacherModel.class);
                    if (existingTeacher != null && !existingTeacher.getUid().isEmpty()) {
                        deactivateAssignmentsAndDeleteTeacher(employeeId, existingTeacher.getUid(), callback);
                        return;
                    }
                    firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                            .document(employeeId)
                            .delete()
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(error -> callback.onError(readableError(error, "pending teacher")));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "pending teacher")));
    }

    private void deactivateAssignmentsAndDeleteTeacher(String employeeId, String teacherUid,
                                                       FirestoreCallback<Void> callback) {
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .whereEqualTo(FirestoreFields.EMPLOYEE_ID, employeeId)
                .get()
                .addOnSuccessListener(employeeAssignments ->
                        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                                .whereEqualTo(FirestoreFields.TEACHER_UID, teacherUid)
                                .get()
                                .addOnSuccessListener(uidAssignments -> {
                                    List<DocumentSnapshot> assignments = mergeAssignmentDocuments(employeeAssignments, uidAssignments);
                                    WriteBatch batch = firestore.batch();
                                    for (DocumentSnapshot assignment : assignments) {
                                        batch.update(assignment.getReference(),
                                                FirestoreFields.IS_ACTIVE, false,
                                                FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
                                        Log.d(TAG, "Deactivated assignment during teacher delete: " + assignment.getId());
                                    }
                                    batch.delete(firestore.collection(FirestoreCollections.PENDING_TEACHERS).document(employeeId));
                                    batch.commit()
                                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                                            .addOnFailureListener(error -> callback.onError(readableError(error, "teacher delete")));
                                })
                                .addOnFailureListener(error -> callback.onError(readableError(error, "teacher assignments"))))
                .addOnFailureListener(error -> callback.onError(readableError(error, "teacher assignments")));
    }

    public void createTeacherFromPendingRecord(String uid, String email, String employeeId,
                                               FirestoreCallback<UserModel> callback) {
        String normalizedEmployeeId = employeeId.trim().toUpperCase(Locale.US);
        firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                .document(normalizedEmployeeId)
                .get()
                .addOnSuccessListener(document -> {
                    PendingTeacherModel pending = document.toObject(PendingTeacherModel.class);
                    if (!document.exists() || pending == null || !pending.isActive()) {
                        callback.onError("Teacher record not found. Contact administration.");
                        return;
                    }
                    UserModel teacher = new UserModel(uid, pending.getName(), email, AppRoles.TEACHER,
                            pending.getPrimaryDepartmentId(), "", "");
                    teacher.setEmployeeId(pending.getEmployeeId());
                    teacher.setPrimaryDepartmentId(pending.getPrimaryDepartmentId());
                    teacher.setDepartmentId(pending.getPrimaryDepartmentId());
                    teacher.setDesignation(pending.getDesignation());
                    linkTeacherProfile(uid, email, normalizedEmployeeId, pending, teacher, callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "teacher signup")));
    }

    public void linkAssignmentsForTeacher(String uid, String employeeId, String teacherName,
                                          FirestoreCallback<List<String>> callback) {
        String normalizedEmployeeId = employeeId.trim().toUpperCase(Locale.US);
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .whereEqualTo(FirestoreFields.EMPLOYEE_ID, normalizedEmployeeId)
                .get()
                .addOnSuccessListener(employeeQuery ->
                        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                                .whereEqualTo(FirestoreFields.TEACHER_UID, uid)
                                .get()
                                .addOnSuccessListener(uidQuery -> linkAssignmentDocuments(
                                        mergeAssignmentDocuments(employeeQuery, uidQuery),
                                        uid,
                                        normalizedEmployeeId,
                                        teacherName,
                                        callback))
                                .addOnFailureListener(error -> callback.onError(readableError(error, "assignment linking"))))
                .addOnFailureListener(error -> callback.onError(readableError(error, "assignment linking")));
    }

    private List<DocumentSnapshot> mergeAssignmentDocuments(QuerySnapshot first, QuerySnapshot second) {
        Map<String, DocumentSnapshot> documents = new LinkedHashMap<>();
        for (DocumentSnapshot document : first.getDocuments()) {
            documents.put(document.getId(), document);
        }
        for (DocumentSnapshot document : second.getDocuments()) {
            documents.put(document.getId(), document);
        }
        return new ArrayList<>(documents.values());
    }

    private void linkAssignmentDocuments(List<DocumentSnapshot> documents, String uid, String normalizedEmployeeId,
                                         String teacherName, FirestoreCallback<List<String>> callback) {
                    WriteBatch batch = firestore.batch();
                    List<String> assignedSubjects = new ArrayList<>();
                    for (DocumentSnapshot document : documents) {
                        TeachingAssignmentModel assignment = document.toObject(TeachingAssignmentModel.class);
                        if (assignment == null || !assignment.isActive()) {
                            continue;
                        }
                        String existingUid = assignment.getTeacherUid();
                        if (!existingUid.isEmpty() && !uid.equals(existingUid)) {
                            callback.onError("Employee ID is already linked to another teacher assignment.");
                            return;
                        }
                        if (!assignedSubjects.contains(assignment.getSubjectCode())) {
                            assignedSubjects.add(assignment.getSubjectCode());
                        }
                        batch.update(document.getReference(),
                                FirestoreFields.TEACHER_UID, uid,
                                FirestoreFields.EMPLOYEE_ID, normalizedEmployeeId,
                                FirestoreFields.TEACHER_NAME, teacherName,
                                FirestoreFields.DEPARTMENT_LABEL, assignment.getDepartmentLabel(),
                                FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
                        Log.d(TAG, "Claimed assignment metadata for " + document.getId());
                    }
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(assignedSubjects))
                            .addOnFailureListener(error -> callback.onError(readableError(error, "assignment linking")));
    }

    private void linkTeacherProfile(String uid, String email, String normalizedEmployeeId, PendingTeacherModel pending,
                                    UserModel teacher, FirestoreCallback<UserModel> callback) {
        if (!pending.getUid().isEmpty() && !uid.equals(pending.getUid())) {
            callback.onError("Employee ID is already linked to another account.");
            return;
        }
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .whereEqualTo(FirestoreFields.EMPLOYEE_ID, normalizedEmployeeId)
                .get()
                .addOnSuccessListener(assignments -> {
                    List<String> assignedSubjects = new ArrayList<>();
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot assignmentDocument : assignments.getDocuments()) {
                        TeachingAssignmentModel assignment = assignmentDocument.toObject(TeachingAssignmentModel.class);
                        if (assignment == null || !assignment.isActive()) {
                            continue;
                        }
                        String existingUid = assignment.getTeacherUid();
                        if (!existingUid.isEmpty() && !uid.equals(existingUid)) {
                            callback.onError("Employee ID is already linked to another teacher assignment.");
                            return;
                        }
                        if (!assignedSubjects.contains(assignment.getSubjectCode())) {
                            assignedSubjects.add(assignment.getSubjectCode());
                        }
                        batch.update(assignmentDocument.getReference(),
                                FirestoreFields.TEACHER_UID, uid,
                                FirestoreFields.EMPLOYEE_ID, normalizedEmployeeId,
                                FirestoreFields.TEACHER_NAME, pending.getName(),
                                FirestoreFields.DEPARTMENT_LABEL, assignment.getDepartmentLabel(),
                                FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
                        Log.d(TAG, "Linked teacher signup metadata for " + assignmentDocument.getId());
                    }
                    teacher.setAssignedSubjects(assignedSubjects);
                    batch.set(firestore.collection(FirestoreCollections.USERS).document(uid), teacher);
                    batch.update(firestore.collection(FirestoreCollections.PENDING_TEACHERS).document(normalizedEmployeeId),
                            FirestoreFields.UID, uid,
                            FirestoreFields.EMAIL, email);
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(teacher))
                            .addOnFailureListener(error -> callback.onError(readableError(error, "teacher signup")));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "teacher assignments")));
    }

    public void fetchTeachers(FirestoreCallback<List<UserModel>> callback) {
        firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo(FirestoreFields.ROLE, AppRoles.TEACHER)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseUsers(query)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "teachers")));
    }

    public void fetchAssignableTeachers(FirestoreCallback<List<UserModel>> callback) {
        firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo(FirestoreFields.ROLE, AppRoles.TEACHER)
                .get()
                .addOnSuccessListener(query -> {
                    List<UserModel> teachers = parseUsers(query);
                    firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                            .get()
                            .addOnSuccessListener(pending -> {
                                List<PendingTeacherModel> pendingTeachers = parsePendingTeachers(pending);
                                keepTeachersWithAdminRecords(teachers, pendingTeachers);
                                mergePendingTeachers(teachers, pendingTeachers);
                                Collections.sort(teachers, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
                                callback.onSuccess(teachers);
                            })
                            .addOnFailureListener(error -> callback.onSuccess(teachers));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "teachers")));
    }

    public void fetchStudentsForAssignment(TeachingAssignmentModel assignment, FirestoreCallback<List<UserModel>> callback) {
        firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo(FirestoreFields.ROLE, AppRoles.STUDENT)
                .whereEqualTo(FirestoreFields.DEPARTMENT_ID, assignment.getDepartmentId())
                .whereEqualTo(FirestoreFields.SEMESTER, assignment.getSemester())
                .whereEqualTo(FirestoreFields.SECTION, assignment.getSection())
                .get()
                .addOnSuccessListener(query -> {
                    List<UserModel> students = parseUsers(query);
                    firestore.collection(FirestoreCollections.USERS)
                            .whereEqualTo(FirestoreFields.ROLE, AppRoles.STUDENT)
                            .whereEqualTo(FirestoreFields.DEPARTMENT, assignment.getDepartmentId())
                            .whereEqualTo(FirestoreFields.SEMESTER, assignment.getSemester())
                            .whereEqualTo(FirestoreFields.SECTION, assignment.getSection())
                            .get()
                            .addOnSuccessListener(legacyUsers -> {
                                students.addAll(parseUsers(legacyUsers));
                                firestore.collection(FirestoreCollections.PENDING_STUDENTS)
                                        .whereEqualTo(FirestoreFields.DEPARTMENT_ID, assignment.getDepartmentId())
                                        .whereEqualTo(FirestoreFields.SEMESTER, assignment.getSemester())
                                        .whereEqualTo(FirestoreFields.SECTION, assignment.getSection())
                                        .get()
                                        .addOnSuccessListener(pending -> {
                                            students.addAll(parsePendingStudentsForAttendance(pending));
                                            callback.onSuccess(dedupeStudentsForAttendance(students));
                                        })
                                        .addOnFailureListener(error -> callback.onSuccess(dedupeStudentsForAttendance(students)));
                            })
                            .addOnFailureListener(error -> callback.onSuccess(dedupeStudentsForAttendance(students)));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "assigned students")));
    }

    public List<TeachingAssignmentModel> filterAssignments(List<TeachingAssignmentModel> assignments, String query,
                                                           String departmentId, String semester, String section) {
        String normalizedQuery = normalize(query);
        String normalizedDepartment = normalizeFilter(departmentId);
        String normalizedSemester = normalizeFilter(semester);
        String normalizedSection = normalizeFilter(section);
        List<TeachingAssignmentModel> filtered = new ArrayList<>();
        for (TeachingAssignmentModel assignment : assignments) {
            boolean matchesQuery = normalizedQuery.isEmpty()
                    || normalize(assignment.getTeacherName()).contains(normalizedQuery)
                    || normalize(assignment.getSubjectCode()).contains(normalizedQuery)
                    || normalize(assignment.getSubjectName()).contains(normalizedQuery);
            boolean matchesDepartment = normalizedDepartment.isEmpty()
                    || normalize(assignment.getDepartmentId()).equals(normalizedDepartment);
            boolean matchesSemester = normalizedSemester.isEmpty()
                    || normalize(assignment.getSemester()).equals(normalizedSemester);
            boolean matchesSection = normalizedSection.isEmpty()
                    || normalize(assignment.getSection()).equals(normalizedSection);
            if (matchesQuery && matchesDepartment && matchesSemester && matchesSection) {
                filtered.add(assignment);
            }
        }
        sortAssignments(filtered);
        return filtered;
    }

    public String buildAssignmentLabel(TeachingAssignmentModel assignment) {
        return assignment.getSubjectCode() + " - " + assignment.getDepartmentId()
                + " " + assignment.getSemester() + " Section " + assignment.getSection();
    }

    public TeachingAssignmentModel assignmentFromSubject(UserModel teacher, SubjectModel subject) {
        TeachingAssignmentModel assignment = new TeachingAssignmentModel("", teacher.getUid(), teacher.getName(), subject.getSubjectCode(),
                subject.getSubjectName(), subject.getDepartmentId(), subject.getSemester(), "", true);
        assignment.setEmployeeId(teacher.getEmployeeId());
        return assignment;
    }

    private List<TeachingAssignmentModel> parseAssignments(QuerySnapshot query, boolean repairMissingMetadata) {
        List<TeachingAssignmentModel> assignments = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            TeachingAssignmentModel assignment = document.toObject(TeachingAssignmentModel.class);
            if (assignment != null) {
                if (assignment.getAssignmentId().isEmpty()) {
                    assignment.setAssignmentId(document.getId());
                }
                if (repairMissingMetadata && TeachingAssignmentNormalizer.needsRepair(assignment)) {
                    Log.d(TAG, "Missing assignment metadata detected for " + assignment.getAssignmentId());
                    repairAssignmentInBackground(assignment, new FirestoreCallback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean didRepair) {
                            if (Boolean.TRUE.equals(didRepair)) {
                                Log.d(TAG, "Assignment repaired in background: " + assignment.getAssignmentId());
                            }
                        }

                        @Override
                        public void onError(String message) {
                            Log.w(TAG, "Background assignment repair skipped: " + message);
                        }
                    });
                }
                assignments.add(assignment);
            }
        }
        sortAssignments(assignments);
        return assignments;
    }

    private void repairAssignmentInBackground(TeachingAssignmentModel assignment, FirestoreCallback<Boolean> callback) {
        if (!TeachingAssignmentNormalizer.needsRepair(assignment)) {
            callback.onSuccess(false);
            return;
        }
        loadTeacherForRepair(assignment, new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel teacher) {
                loadSubjectForRepair(assignment, new FirestoreCallback<SubjectModel>() {
                    @Override
                    public void onSuccess(SubjectModel subject) {
                        loadDepartmentForRepair(assignment, new FirestoreCallback<DepartmentModel>() {
                            @Override
                            public void onSuccess(DepartmentModel department) {
                                Map<String, Object> values = TeachingAssignmentNormalizer.repairValues(
                                        assignment, teacher, subject, department);
                                if (values.isEmpty()) {
                                    callback.onSuccess(false);
                                    return;
                                }
                                values.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
                                firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                                        .document(assignment.getAssignmentId())
                                        .set(values, SetOptions.merge())
                                        .addOnSuccessListener(unused -> callback.onSuccess(true))
                                        .addOnFailureListener(error -> callback.onError(readableError(error, "assignment repair")));
                            }

                            @Override
                            public void onError(String message) {
                                callback.onError(message);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void loadTeacherForRepair(TeachingAssignmentModel assignment, FirestoreCallback<UserModel> callback) {
        if (!assignment.getTeacherUid().isEmpty()) {
            firestore.collection(FirestoreCollections.USERS)
                    .document(assignment.getTeacherUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        UserModel teacher = document.toObject(UserModel.class);
                        if (teacher != null) {
                            if (teacher.getUid().isEmpty()) {
                                teacher.setUid(document.getId());
                            }
                            callback.onSuccess(teacher);
                            return;
                        }
                        callback.onError("Teacher profile missing for assignment " + assignment.getAssignmentId());
                    })
                    .addOnFailureListener(error -> callback.onError(readableError(error, "teacher profile repair")));
            return;
        }
        if (assignment.getEmployeeId().isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        firestore.collection(FirestoreCollections.PENDING_TEACHERS)
                .document(TeachingAssignmentNormalizer.normalizeEmployeeId(assignment.getEmployeeId()))
                .get()
                .addOnSuccessListener(document -> {
                    PendingTeacherModel pending = document.toObject(PendingTeacherModel.class);
                    if (pending == null) {
                        callback.onSuccess(null);
                        return;
                    }
                    UserModel teacher = new UserModel(
                            pending.getUid(),
                            pending.getName(),
                            pending.getEmail(),
                            AppRoles.TEACHER,
                            pending.getPrimaryDepartmentId(),
                            "",
                            ""
                    );
                    teacher.setEmployeeId(pending.getEmployeeId());
                    callback.onSuccess(teacher);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "pending teacher repair")));
    }

    private void loadSubjectForRepair(TeachingAssignmentModel assignment, FirestoreCallback<SubjectModel> callback) {
        if (assignment.getSubjectCode().isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        firestore.collection(FirestoreCollections.SUBJECTS)
                .document(assignment.getSubjectCode())
                .get()
                .addOnSuccessListener(document -> {
                    SubjectModel subject = document.toObject(SubjectModel.class);
                    if (subject != null && subject.getSubjectCode().isEmpty()) {
                        subject.setSubjectCode(document.getId());
                    }
                    callback.onSuccess(subject);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "subject repair")));
    }

    private void loadDepartmentForRepair(TeachingAssignmentModel assignment, FirestoreCallback<DepartmentModel> callback) {
        if (assignment.getDepartmentId().isEmpty()) {
            callback.onSuccess(null);
            return;
        }
        firestore.collection(FirestoreCollections.DEPARTMENTS)
                .document(assignment.getDepartmentId())
                .get()
                .addOnSuccessListener(document -> {
                    DepartmentModel department = document.toObject(DepartmentModel.class);
                    if (department != null && department.getDepartmentId().isEmpty()) {
                        department.setDepartmentId(document.getId());
                    }
                    callback.onSuccess(department);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "department repair")));
    }

    private List<PendingTeacherModel> parsePendingTeachers(QuerySnapshot query) {
        List<PendingTeacherModel> teachers = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            PendingTeacherModel teacher = document.toObject(PendingTeacherModel.class);
            if (teacher != null) {
                teachers.add(teacher);
            }
        }
        Collections.sort(teachers, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return teachers;
    }

    private List<UserModel> parseUsers(QuerySnapshot query) {
        List<UserModel> users = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            UserModel user = document.toObject(UserModel.class);
            if (user != null) {
                if (user.getUid().isEmpty()) {
                    user.setUid(document.getId());
                }
                users.add(user);
            }
        }
        Collections.sort(users, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return users;
    }

    private List<UserModel> parsePendingStudentsForAttendance(QuerySnapshot query) {
        List<UserModel> users = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            UserModel user = document.toObject(UserModel.class);
            if (user != null) {
                if (user.getUid().isEmpty()) {
                    user.setUid(document.getId());
                }
                users.add(user);
            }
        }
        return users;
    }

    private List<UserModel> dedupeStudentsForAttendance(List<UserModel> students) {
        Map<String, UserModel> deduped = new LinkedHashMap<>();
        for (UserModel student : students) {
            String key = student.getRollNumber().trim().toLowerCase(Locale.US)
                    + "_"
                    + student.getRegistrationNumber().trim().toLowerCase(Locale.US);
            UserModel existing = deduped.get(key);
            if (existing == null || isPendingAttendanceStudent(existing)) {
                deduped.put(key, student);
            }
        }
        List<UserModel> result = new ArrayList<>(deduped.values());
        Collections.sort(result, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return result;
    }

    private boolean isPendingAttendanceStudent(UserModel student) {
        return !student.getUid().isEmpty() && student.getUid().contains("_");
    }

    private void mergePendingTeachers(List<UserModel> teachers, List<PendingTeacherModel> pendingTeachers) {
        for (PendingTeacherModel pending : pendingTeachers) {
            if (!pending.isActive()) {
                continue;
            }
            boolean alreadyPresent = false;
            for (UserModel teacher : teachers) {
                if (isSameTeacherRecord(teacher, pending)) {
                    enrichTeacherFromPending(teacher, pending);
                    alreadyPresent = true;
                    break;
                }
            }
            if (alreadyPresent) {
                continue;
            }
            UserModel teacher = new UserModel(
                    pending.getUid(),
                    pending.getName(),
                    pending.getEmail(),
                    AppRoles.TEACHER,
                    pending.getPrimaryDepartmentId(),
                    "",
                    ""
            );
            teacher.setEmployeeId(pending.getEmployeeId());
            teacher.setPrimaryDepartmentId(pending.getPrimaryDepartmentId());
            teacher.setDepartmentId(pending.getPrimaryDepartmentId());
            teacher.setDesignation(pending.getDesignation());
            teachers.add(teacher);
        }
    }

    private void keepTeachersWithAdminRecords(List<UserModel> teachers, List<PendingTeacherModel> pendingTeachers) {
        List<UserModel> retained = new ArrayList<>();
        for (UserModel teacher : teachers) {
            for (PendingTeacherModel pending : pendingTeachers) {
                if (pending.isActive() && isSameTeacherRecord(teacher, pending)) {
                    retained.add(teacher);
                    break;
                }
            }
        }
        teachers.clear();
        teachers.addAll(retained);
    }

    private boolean isSameTeacherRecord(UserModel teacher, PendingTeacherModel pending) {
        return (!teacher.getEmployeeId().isEmpty()
                && teacher.getEmployeeId().equalsIgnoreCase(pending.getEmployeeId()))
                || (!teacher.getUid().isEmpty()
                && teacher.getUid().equals(pending.getUid()))
                || (!teacher.getEmail().isEmpty()
                && teacher.getEmail().equalsIgnoreCase(pending.getEmail()));
    }

    private void enrichTeacherFromPending(UserModel teacher, PendingTeacherModel pending) {
        if (teacher.getEmployeeId().isEmpty()) {
            teacher.setEmployeeId(pending.getEmployeeId());
        }
        if (teacher.getName().isEmpty()) {
            teacher.setName(pending.getName());
        }
        if (teacher.getDepartmentId().isEmpty()) {
            teacher.setDepartmentId(pending.getPrimaryDepartmentId());
            teacher.setDepartment(pending.getPrimaryDepartmentId());
        }
        if (teacher.getPrimaryDepartmentId().isEmpty()) {
            teacher.setPrimaryDepartmentId(pending.getPrimaryDepartmentId());
        }
        if (teacher.getDesignation().isEmpty()) {
            teacher.setDesignation(pending.getDesignation());
        }
    }

    private void sortAssignments(List<TeachingAssignmentModel> assignments) {
        Collections.sort(assignments, (first, second) -> buildAssignmentLabel(first).compareToIgnoreCase(buildAssignmentLabel(second)));
    }

    private String buildAssignmentId(TeachingAssignmentModel assignment) {
        String owner = assignment.getTeacherUid().isEmpty() ? assignment.getEmployeeId() : assignment.getTeacherUid();
        return owner + "_" + assignment.getSubjectCode() + "_"
                + assignment.getDepartmentId() + "_" + assignment.getSemester().replaceAll("[^A-Za-z0-9]+", "")
                + "_" + assignment.getSection();
    }

    private String normalizeFilter(String value) {
        String normalized = normalize(value);
        return "all".equals(normalized) || normalized.startsWith("all ") ? "" : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private String readableError(@NonNull Exception error, String target) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Access denied while loading " + target + ".";
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return "Firestore needs an index for " + target + ". Check Firebase Console.";
            }
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Unable to load " + target + ". Please try again."
                : message;
    }
}
