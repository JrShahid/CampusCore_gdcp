package com.example.campuscore.repositories;

import androidx.annotation.NonNull;

import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.AppRoles;
import com.example.campuscore.utils.FirestoreCollections;
import com.example.campuscore.utils.FirestoreFields;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public StudentRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void fetchStudents(FirestoreCallback<List<UserModel>> callback) {
        firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo(FirestoreFields.ROLE, AppRoles.STUDENT)
                .get()
                .addOnSuccessListener(query -> {
                    List<UserModel> students = parseUserStudents(query);
                    firestore.collection(FirestoreCollections.PENDING_STUDENTS)
                            .get()
                            .addOnSuccessListener(pending -> {
                                students.addAll(parsePendingStudents(pending));
                                List<UserModel> deduped = dedupeStudents(students);
                                sortStudents(deduped);
                                callback.onSuccess(deduped);
                            })
                            .addOnFailureListener(error -> callback.onSuccess(students));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchStudentsForCurrentScope(FirestoreCallback<List<UserModel>> callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }
        firestore.collection(FirestoreCollections.USERS)
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    UserModel actor = snapshot.toObject(UserModel.class);
                    if (actor != null && AppRoles.TEACHER.equalsIgnoreCase(actor.getRole())) {
                        fetchStudentsForDepartment(actor.getDepartmentId(), callback);
                    } else {
                        fetchStudents(callback);
                    }
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchStudentsForDepartment(String departmentId, FirestoreCallback<List<UserModel>> callback) {
        firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo(FirestoreFields.ROLE, AppRoles.STUDENT)
                .whereEqualTo(FirestoreFields.DEPARTMENT_ID, departmentId)
                .get()
                .addOnSuccessListener(query -> {
                    List<UserModel> students = parseUserStudents(query);
                    firestore.collection(FirestoreCollections.USERS)
                            .whereEqualTo(FirestoreFields.ROLE, AppRoles.STUDENT)
                            .whereEqualTo(FirestoreFields.DEPARTMENT, departmentId)
                            .get()
                            .addOnSuccessListener(legacy -> {
                                students.addAll(parseUserStudents(legacy));
                                firestore.collection(FirestoreCollections.PENDING_STUDENTS)
                                        .whereEqualTo(FirestoreFields.DEPARTMENT_ID, departmentId)
                                        .get()
                                        .addOnSuccessListener(pending -> {
                                            students.addAll(parsePendingStudents(pending));
                                            List<UserModel> deduped = dedupeStudents(students);
                                            sortStudents(deduped);
                                            callback.onSuccess(deduped);
                                        })
                                        .addOnFailureListener(error -> {
                                            sortStudents(students);
                                            callback.onSuccess(students);
                                        });
                            })
                            .addOnFailureListener(error -> callback.onSuccess(students));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchStudentsForClass(String departmentId, String semester, String section,
                                      FirestoreCallback<List<UserModel>> callback) {
        firestore.collection(FirestoreCollections.USERS)
                .whereEqualTo(FirestoreFields.ROLE, AppRoles.STUDENT)
                .whereEqualTo(FirestoreFields.DEPARTMENT_ID, departmentId)
                .whereEqualTo(FirestoreFields.SEMESTER, semester)
                .whereEqualTo(FirestoreFields.SECTION, section)
                .get()
                .addOnSuccessListener(query -> {
                    List<UserModel> students = parseUserStudents(query);
                    firestore.collection(FirestoreCollections.PENDING_STUDENTS)
                            .whereEqualTo(FirestoreFields.DEPARTMENT_ID, departmentId)
                            .whereEqualTo(FirestoreFields.SEMESTER, semester)
                            .whereEqualTo(FirestoreFields.SECTION, section)
                            .get()
                            .addOnSuccessListener(pending -> {
                                students.addAll(parsePendingStudents(pending));
                                List<UserModel> deduped = dedupeStudents(students);
                                sortStudents(deduped);
                                callback.onSuccess(deduped);
                            })
                            .addOnFailureListener(error -> {
                                sortStudents(students);
                                callback.onSuccess(students);
                            });
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void saveStudent(UserModel student, FirestoreCallback<Void> callback) {
        String documentId = pendingStudentId(student.getRollNumber(), student.getRegistrationNumber());
        student.setRole(AppRoles.STUDENT);
        student.setDepartmentId(student.getDepartmentId());

        validateUnique(FirestoreFields.ROLL_NUMBER, student.getRollNumber(), documentId, student.getUid(),
                new FirestoreCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean rollAvailable) {
                if (!Boolean.TRUE.equals(rollAvailable)) {
                    callback.onError("Roll number is already assigned.");
                    return;
                }
                validateUnique(FirestoreFields.REGISTRATION_NUMBER, student.getRegistrationNumber(), documentId, student.getUid(),
                        new FirestoreCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean registrationAvailable) {
                                if (!Boolean.TRUE.equals(registrationAvailable)) {
                                    callback.onError("Registration number is already assigned.");
                                    return;
                                }
                                saveStudentDocuments(student, documentId, callback);
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

    private void saveStudentDocuments(UserModel student, String pendingDocumentId, FirestoreCallback<Void> callback) {
        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection(FirestoreCollections.PENDING_STUDENTS).document(pendingDocumentId), student);
        if (!student.getUid().isEmpty()) {
            batch.set(firestore.collection(FirestoreCollections.USERS).document(student.getUid()), student);
        }
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void deleteStudent(UserModel student, FirestoreCallback<Void> callback) {
        WriteBatch batch = firestore.batch();
        if (!student.getUid().isEmpty()) {
            batch.delete(firestore.collection(FirestoreCollections.USERS).document(student.getUid()));
        }
        batch.delete(firestore.collection(FirestoreCollections.PENDING_STUDENTS)
                .document(pendingStudentId(student.getRollNumber(), student.getRegistrationNumber())));
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void createUserFromPendingRecord(String uid, String email, String rollNumber, String registrationNumber,
                                            FirestoreCallback<UserModel> callback) {
        firestore.collection(FirestoreCollections.PENDING_STUDENTS)
                .whereEqualTo(FirestoreFields.ROLL_NUMBER, rollNumber)
                .whereEqualTo(FirestoreFields.REGISTRATION_NUMBER, registrationNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        callback.onError("Student record not found. Contact administration.");
                        return;
                    }
                    DocumentSnapshot pendingDocument = query.getDocuments().get(0);
                    UserModel student = pendingDocument.toObject(UserModel.class);
                    if (student == null) {
                        callback.onError("Student record not found. Contact administration.");
                        return;
                    }
                    if (!student.getUid().isEmpty() && !uid.equals(student.getUid())) {
                        callback.onError("This student identity is already linked to another account.");
                        return;
                    }
                    student.setUid(uid);
                    student.setEmail(email);
                    student.setRole(AppRoles.STUDENT);
                    if (student.getDepartmentId().isEmpty()) {
                        student.setDepartmentId(student.getDepartment());
                    }
                    WriteBatch batch = firestore.batch();
                    batch.set(firestore.collection(FirestoreCollections.USERS).document(uid), student);
                    batch.update(firestore.collection(FirestoreCollections.PENDING_STUDENTS).document(pendingDocument.getId()),
                            FirestoreFields.UID, uid,
                            FirestoreFields.EMAIL, email);
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess(student))
                            .addOnFailureListener(error -> callback.onError(readableError(error)));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public List<UserModel> filterStudents(List<UserModel> students, String query, String department,
                                          String semester, String section) {
        String normalizedQuery = normalize(query);
        String normalizedDepartment = normalizeFilter(department);
        String normalizedSemester = normalizeFilter(semester);
        String normalizedSection = normalizeFilter(section);

        List<UserModel> filtered = new ArrayList<>();
        for (UserModel student : students) {
            boolean matchesSearch = normalizedQuery.isEmpty()
                    || normalize(student.getName()).contains(normalizedQuery)
                    || normalize(student.getRollNumber()).contains(normalizedQuery)
                    || normalize(student.getRegistrationNumber()).contains(normalizedQuery);
            boolean matchesDepartment = normalizedDepartment.isEmpty()
                    || normalize(student.getDepartment()).equals(normalizedDepartment)
                    || normalize(student.getDepartmentId()).equals(normalizedDepartment);
            boolean matchesSemester = normalizedSemester.isEmpty()
                    || normalize(student.getSemester()).equals(normalizedSemester);
            boolean matchesSection = normalizedSection.isEmpty()
                    || normalize(student.getSection()).equals(normalizedSection);
            if (matchesSearch && matchesDepartment && matchesSemester && matchesSection) {
                filtered.add(student);
            }
        }
        Collections.sort(filtered, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return filtered;
    }

    private void validateUnique(String field, String value, String currentPendingId, String currentUid,
                                FirestoreCallback<Boolean> callback) {
        if (value == null || value.trim().isEmpty()) {
            callback.onSuccess(true);
            return;
        }

        firestore.collection(FirestoreCollections.PENDING_STUDENTS)
                .whereEqualTo(field, value.trim())
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot document : query.getDocuments()) {
                        if (!document.getId().equals(currentPendingId)) {
                            callback.onSuccess(false);
                            return;
                        }
                    }
                    firestore.collection(FirestoreCollections.USERS)
                            .whereEqualTo(FirestoreFields.ROLE, AppRoles.STUDENT)
                            .whereEqualTo(field, value.trim())
                            .get()
                            .addOnSuccessListener(users -> {
                                for (DocumentSnapshot document : users.getDocuments()) {
                                    if (!document.getId().equals(currentUid)) {
                                        callback.onSuccess(false);
                                        return;
                                    }
                                }
                                callback.onSuccess(true);
                            })
                            .addOnFailureListener(error -> callback.onError(readableError(error)));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private List<UserModel> parseUserStudents(QuerySnapshot query) {
        List<UserModel> students = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            UserModel student = document.toObject(UserModel.class);
            if (student != null) {
                if (student.getUid().isEmpty()) {
                    student.setUid(document.getId());
                }
                students.add(student);
            }
        }
        Collections.sort(students, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return students;
    }

    private List<UserModel> parsePendingStudents(QuerySnapshot query) {
        List<UserModel> students = new ArrayList<>();
        for (DocumentSnapshot document : query.getDocuments()) {
            UserModel student = document.toObject(UserModel.class);
            if (student != null) {
                if (student.getUid().isEmpty()) {
                    student.setUid("");
                }
                students.add(student);
            }
        }
        Collections.sort(students, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
        return students;
    }

    private List<UserModel> dedupeStudents(List<UserModel> students) {
        Map<String, UserModel> deduped = new LinkedHashMap<>();
        for (UserModel student : students) {
            String key = pendingStudentId(student.getRollNumber(), student.getRegistrationNumber()).toLowerCase(Locale.US);
            UserModel existing = deduped.get(key);
            if (existing == null || existing.getUid().isEmpty()) {
                deduped.put(key, student);
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private void sortStudents(List<UserModel> students) {
        Collections.sort(students, (first, second) -> first.getName().compareToIgnoreCase(second.getName()));
    }

    private String pendingStudentId(String rollNumber, String registrationNumber) {
        return rollNumber.trim()
                + "_"
                + registrationNumber.trim();
    }

    private String normalizeFilter(String value) {
        String normalized = normalize(value);
        return "all".equals(normalized) || normalized.startsWith("all ") ? "" : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private String readableError(@NonNull Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Student management access was denied by Firestore rules.";
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return "Student query needs a Firestore index. Check the Firebase Console.";
            }
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Student request failed. Please try again."
                : message;
    }
}
