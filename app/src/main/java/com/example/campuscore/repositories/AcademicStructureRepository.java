package com.example.campuscore.repositories;

import androidx.annotation.NonNull;

import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.FirestoreCollections;
import com.example.campuscore.utils.FirestoreFields;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AcademicStructureRepository {
    private final FirebaseFirestore firestore;

    public AcademicStructureRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void fetchDepartments(FirestoreCallback<List<DepartmentModel>> callback) {
        firestore.collection(FirestoreCollections.DEPARTMENTS)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseDepartments(query, true)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "departments")));
    }

    public void fetchActiveDepartments(FirestoreCallback<List<DepartmentModel>> callback) {
        firestore.collection(FirestoreCollections.DEPARTMENTS)
                .whereEqualTo(FirestoreFields.IS_ACTIVE, true)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(withDepartmentFallback(parseDepartments(query, true))))
                .addOnFailureListener(error -> callback.onSuccess(fallbackDepartments()));
    }

    public void fetchActiveDepartmentsStrict(FirestoreCallback<List<DepartmentModel>> callback) {
        firestore.collection(FirestoreCollections.DEPARTMENTS)
                .whereEqualTo(FirestoreFields.IS_ACTIVE, true)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseDepartments(query, true)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "departments")));
    }

    public void saveDepartment(DepartmentModel department, String originalDepartmentId, FirestoreCallback<Void> callback) {
        String departmentId = normalizeCode(department.getDepartmentId());
        String originalId = normalizeCode(originalDepartmentId);
        if (departmentId.isEmpty()) {
            callback.onError("Department ID is required.");
            return;
        }
        firestore.collection(FirestoreCollections.DEPARTMENTS)
                .document(departmentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && !departmentId.equals(originalId)) {
                        callback.onError("Department ID already exists.");
                        return;
                    }
                    Map<String, Object> values = new HashMap<>();
                    values.put(FirestoreFields.DEPARTMENT_ID, departmentId);
                    values.put(FirestoreFields.DEPARTMENT_NAME, department.getDepartmentName().trim());
                    values.put(FirestoreFields.IS_ACTIVE, department.isActive());
                    values.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());

                    firestore.collection(FirestoreCollections.DEPARTMENTS)
                            .document(departmentId)
                            .set(values)
                            .addOnSuccessListener(unused -> {
                                if (!originalId.isEmpty() && !departmentId.equals(originalId)) {
                                    firestore.collection(FirestoreCollections.DEPARTMENTS).document(originalId).delete();
                                }
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(error -> callback.onError(readableError(error, "department")));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "department")));
    }

    public void deleteDepartment(DepartmentModel department, FirestoreCallback<Void> callback) {
        String departmentId = normalizeCode(department.getDepartmentId());
        if (departmentId.isEmpty()) {
            callback.onError("Department ID is required.");
            return;
        }
        firestore.collection(FirestoreCollections.DEPARTMENTS)
                .document(departmentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> callback.onError(readableError(error, "department")));
    }

    public void fetchSubjects(FirestoreCallback<List<SubjectModel>> callback) {
        firestore.collection(FirestoreCollections.SUBJECTS)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseSubjects(query, true)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "subjects")));
    }

    public void fetchActiveSubjects(String departmentId, String semester, FirestoreCallback<List<SubjectModel>> callback) {
        firestore.collection(FirestoreCollections.SUBJECTS)
                .whereEqualTo(FirestoreFields.DEPARTMENT_ID, departmentId)
                .whereEqualTo(FirestoreFields.SEMESTER, semester)
                .whereEqualTo(FirestoreFields.IS_ACTIVE, true)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(withSubjectFallback(parseSubjects(query, true), departmentId, semester)))
                .addOnFailureListener(error -> callback.onSuccess(fallbackSubjects(departmentId, semester)));
    }

    public void fetchActiveSubjectsStrict(String departmentId, String semester, FirestoreCallback<List<SubjectModel>> callback) {
        firestore.collection(FirestoreCollections.SUBJECTS)
                .whereEqualTo(FirestoreFields.DEPARTMENT_ID, departmentId)
                .whereEqualTo(FirestoreFields.SEMESTER, semester)
                .whereEqualTo(FirestoreFields.IS_ACTIVE, true)
                .get()
                .addOnSuccessListener(query -> callback.onSuccess(parseSubjects(query, true)))
                .addOnFailureListener(error -> callback.onError(readableError(error, "subjects")));
    }

    public void fetchTeacherSubjects(UserModel teacher, FirestoreCallback<List<SubjectModel>> callback) {
        String departmentId = teacher.getDepartmentId();
        if (!teacher.hasAssignedSubjects()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }
        firestore.collection(FirestoreCollections.SUBJECTS)
                .whereEqualTo(FirestoreFields.DEPARTMENT_ID, departmentId)
                .whereEqualTo(FirestoreFields.IS_ACTIVE, true)
                .get()
                .addOnSuccessListener(query -> {
                    List<SubjectModel> subjects = filterAssignedSubjects(parseSubjects(query, true), teacher.getAssignedSubjects());
                    callback.onSuccess(subjects);
                })
                .addOnFailureListener(error -> callback.onSuccess(filterAssignedSubjects(
                        fallbackSubjectsForDepartment(departmentId),
                        teacher.getAssignedSubjects()
                )));
    }

    public void saveSubject(SubjectModel subject, String originalSubjectCode, FirestoreCallback<Void> callback) {
        String subjectCode = normalizeCode(subject.getSubjectCode());
        String originalCode = normalizeCode(originalSubjectCode);
        if (subjectCode.isEmpty()) {
            callback.onError("Subject code is required.");
            return;
        }
        firestore.collection(FirestoreCollections.SUBJECTS)
                .document(subjectCode)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && !subjectCode.equals(originalCode)) {
                        callback.onError("Subject code already exists.");
                        return;
                    }
                    Map<String, Object> values = new HashMap<>();
                    values.put(FirestoreFields.SUBJECT_CODE, subjectCode);
                    values.put(FirestoreFields.SUBJECT_NAME, subject.getSubjectName().trim());
                    values.put(FirestoreFields.DEPARTMENT_ID, subject.getDepartmentId().trim());
                    values.put(FirestoreFields.SEMESTER, subject.getSemester().trim());
                    values.put(FirestoreFields.IS_ACTIVE, subject.isActive());
                    values.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());

                    firestore.collection(FirestoreCollections.SUBJECTS)
                            .document(subjectCode)
                            .set(values)
                            .addOnSuccessListener(unused -> {
                                if (!originalCode.isEmpty() && !subjectCode.equals(originalCode)) {
                                    firestore.collection(FirestoreCollections.SUBJECTS).document(originalCode).delete();
                                }
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(error -> callback.onError(readableError(error, "subject")));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "subject")));
    }

    public void deleteSubject(SubjectModel subject, FirestoreCallback<Void> callback) {
        String subjectCode = normalizeCode(subject.getSubjectCode());
        if (subjectCode.isEmpty()) {
            callback.onError("Subject code is required.");
            return;
        }
        firestore.collection(FirestoreCollections.TEACHING_ASSIGNMENTS)
                .whereEqualTo(FirestoreFields.SUBJECT_CODE, subjectCode)
                .get()
                .addOnSuccessListener(assignments -> {
                    WriteBatch batch = firestore.batch();
                    for (DocumentSnapshot assignment : assignments.getDocuments()) {
                        batch.update(assignment.getReference(),
                                FirestoreFields.IS_ACTIVE, false,
                                FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());
                    }
                    batch.delete(firestore.collection(FirestoreCollections.SUBJECTS).document(subjectCode));
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(null))
                            .addOnFailureListener(error -> callback.onError(readableError(error, "subject")));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error, "subject assignments")));
    }

    public List<DepartmentModel> filterDepartments(List<DepartmentModel> departments, String query) {
        String normalizedQuery = normalize(query);
        List<DepartmentModel> filtered = new ArrayList<>();
        for (DepartmentModel department : departments) {
            if (normalizedQuery.isEmpty()
                    || normalize(department.getDepartmentId()).contains(normalizedQuery)
                    || normalize(department.getDepartmentName()).contains(normalizedQuery)) {
                filtered.add(department);
            }
        }
        sortDepartments(filtered);
        return filtered;
    }

    public List<SubjectModel> filterSubjects(List<SubjectModel> subjects, String query, String departmentId, String semester) {
        String normalizedQuery = normalize(query);
        String normalizedDepartment = normalizeFilter(departmentId);
        String normalizedSemester = normalizeFilter(semester);
        List<SubjectModel> filtered = new ArrayList<>();
        for (SubjectModel subject : subjects) {
            boolean matchesSearch = normalizedQuery.isEmpty()
                    || normalize(subject.getSubjectCode()).contains(normalizedQuery)
                    || normalize(subject.getSubjectName()).contains(normalizedQuery);
            boolean matchesDepartment = normalizedDepartment.isEmpty()
                    || normalize(subject.getDepartmentId()).equals(normalizedDepartment);
            boolean matchesSemester = normalizedSemester.isEmpty()
                    || normalize(subject.getSemester()).equals(normalizedSemester);
            if (matchesSearch && matchesDepartment && matchesSemester) {
                filtered.add(subject);
            }
        }
        sortSubjects(filtered);
        return filtered;
    }

    public List<String> departmentIds(List<DepartmentModel> departments) {
        List<String> ids = new ArrayList<>();
        for (DepartmentModel department : departments) {
            ids.add(department.getDepartmentId());
        }
        return ids;
    }

    public List<String> departmentLabels(List<DepartmentModel> departments) {
        List<String> labels = new ArrayList<>();
        for (DepartmentModel department : departments) {
            labels.add(departmentLabel(department));
        }
        return labels;
    }

    public String departmentLabel(DepartmentModel department) {
        return department.getDepartmentId() + " - " + department.getDepartmentName();
    }

    public String departmentLabelForId(List<DepartmentModel> departments, String departmentId) {
        for (DepartmentModel department : departments) {
            if (department.getDepartmentId().equals(departmentId)) {
                return departmentLabel(department);
            }
        }
        return departmentId;
    }

    public String departmentIdFromLabel(String label) {
        if (label == null) {
            return "";
        }
        String value = label.trim();
        int separatorIndex = value.indexOf(" - ");
        return separatorIndex > 0 ? value.substring(0, separatorIndex).trim() : value;
    }

    private List<DepartmentModel> parseDepartments(QuerySnapshot query, boolean sorted) {
        List<DepartmentModel> departments = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            DepartmentModel department = new DepartmentModel();
            department.setDepartmentId(readString(document, FirestoreFields.DEPARTMENT_ID, document.getId()));
            department.setDepartmentName(readString(document, FirestoreFields.DEPARTMENT_NAME, ""));
            Boolean active = document.getBoolean(FirestoreFields.IS_ACTIVE);
            department.setActive(active == null || active);
            if (!department.getDepartmentId().isEmpty() && !department.getDepartmentName().isEmpty()) {
                departments.add(department);
            }
        }
        if (sorted) {
            sortDepartments(departments);
        }
        return departments;
    }

    private List<SubjectModel> parseSubjects(QuerySnapshot query, boolean sorted) {
        List<SubjectModel> subjects = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            SubjectModel subject = new SubjectModel();
            subject.setSubjectCode(readString(document, FirestoreFields.SUBJECT_CODE, document.getId()));
            subject.setSubjectName(readString(document, FirestoreFields.SUBJECT_NAME, ""));
            subject.setDepartmentId(readString(document, FirestoreFields.DEPARTMENT_ID, ""));
            subject.setSemester(readString(document, FirestoreFields.SEMESTER, ""));
            Boolean active = document.getBoolean(FirestoreFields.IS_ACTIVE);
            subject.setActive(active == null || active);
            if (!subject.getSubjectCode().isEmpty() && !subject.getSubjectName().isEmpty()) {
                subjects.add(subject);
            }
        }
        if (sorted) {
            sortSubjects(subjects);
        }
        return subjects;
    }

    private List<DepartmentModel> withDepartmentFallback(List<DepartmentModel> departments) {
        return departments.isEmpty() ? fallbackDepartments() : departments;
    }

    private List<SubjectModel> withSubjectFallback(List<SubjectModel> subjects, String departmentId, String semester) {
        return subjects.isEmpty() ? fallbackSubjects(departmentId, semester) : subjects;
    }

    private List<DepartmentModel> fallbackDepartments() {
        List<DepartmentModel> departments = new ArrayList<>();
        Map<String, String> defaults = AcademicDataProvider.defaultDepartmentCodeMap();
        for (String name : defaults.keySet()) {
            departments.add(new DepartmentModel(defaults.get(name), name, true));
        }
        return departments;
    }

    private List<SubjectModel> fallbackSubjects(String departmentId, String semester) {
        String departmentName = AcademicDataProvider.departmentNameForCode(departmentId);
        List<SubjectModel> subjects = new ArrayList<>();
        for (AcademicDataProvider.SubjectItem item : AcademicDataProvider.subjectsForDepartmentAndSemester(departmentName, semester)) {
            subjects.add(new SubjectModel(item.getCode(), item.getName(), departmentId, semester, true));
        }
        return subjects;
    }

    private List<SubjectModel> fallbackSubjectsForDepartment(String departmentId) {
        List<SubjectModel> subjects = new ArrayList<>();
        for (String semester : AcademicDataProvider.semesterValues()) {
            subjects.addAll(fallbackSubjects(departmentId, semester));
        }
        sortSubjects(subjects);
        return subjects;
    }

    private List<SubjectModel> filterAssignedSubjects(List<SubjectModel> subjects, List<String> assignedSubjects) {
        if (assignedSubjects == null || assignedSubjects.isEmpty()) {
            return subjects;
        }
        List<String> normalizedAssignments = new ArrayList<>();
        for (String subject : assignedSubjects) {
            normalizedAssignments.add(normalize(subject));
        }
        List<SubjectModel> filtered = new ArrayList<>();
        for (SubjectModel subject : subjects) {
            if (normalizedAssignments.contains(normalize(subject.getSubjectCode()))) {
                filtered.add(subject);
            }
        }
        sortSubjects(filtered);
        return filtered;
    }

    private void sortDepartments(List<DepartmentModel> departments) {
        Collections.sort(departments, (first, second) -> first.getDepartmentName().compareToIgnoreCase(second.getDepartmentName()));
    }

    private void sortSubjects(List<SubjectModel> subjects) {
        Collections.sort(subjects, (first, second) -> {
            int departmentCompare = first.getDepartmentId().compareToIgnoreCase(second.getDepartmentId());
            if (departmentCompare != 0) {
                return departmentCompare;
            }
            int semesterCompare = first.getSemester().compareToIgnoreCase(second.getSemester());
            if (semesterCompare != 0) {
                return semesterCompare;
            }
            return first.getSubjectCode().compareToIgnoreCase(second.getSubjectCode());
        });
    }

    private String readString(DocumentSnapshot document, String field, String fallback) {
        String value = document.getString(field);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US);
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
                return "Academic structure access was denied by Firestore rules.";
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return "Academic structure query needs a Firestore index. Check the Firebase Console.";
            }
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Unable to load " + target + ". Please try again."
                : message;
    }
}
