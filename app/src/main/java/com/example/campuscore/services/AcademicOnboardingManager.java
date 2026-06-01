package com.example.campuscore.services;

import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.repositories.StudentRepository;
import com.example.campuscore.repositories.TeachingAssignmentsRepository;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class AcademicOnboardingManager {
    private final FirebaseAuth auth;
    private final StudentRepository studentRepository;
    private final TeachingAssignmentsRepository teachingAssignmentsRepository;

    public AcademicOnboardingManager() {
        auth = FirebaseAuth.getInstance();
        studentRepository = new StudentRepository();
        teachingAssignmentsRepository = new TeachingAssignmentsRepository();
    }

    public void signupStudent(String email, String password, String rollNumber, String registrationNumber,
                              FirestoreCallback<UserModel> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Unable to create account. Please try again.");
                        return;
                    }
                    activateStudent(firebaseUser, email, rollNumber, registrationNumber, callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void signupTeacher(String email, String password, String employeeId,
                              FirestoreCallback<UserModel> callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Unable to create account. Please try again.");
                        return;
                    }
                    activateTeacher(firebaseUser, email, employeeId, callback);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private void activateStudent(FirebaseUser firebaseUser, String email, String rollNumber, String registrationNumber,
                                 FirestoreCallback<UserModel> callback) {
        studentRepository.createUserFromPendingRecord(firebaseUser.getUid(), email, rollNumber, registrationNumber,
                new FirestoreCallback<UserModel>() {
                    @Override
                    public void onSuccess(UserModel student) {
                        sendVerification(firebaseUser, student, callback);
                    }

                    @Override
                    public void onError(String message) {
                        cleanupAuthUser(firebaseUser, message, callback);
                    }
                });
    }

    private void activateTeacher(FirebaseUser firebaseUser, String email, String employeeId,
                                 FirestoreCallback<UserModel> callback) {
        teachingAssignmentsRepository.createTeacherFromPendingRecord(firebaseUser.getUid(), email, employeeId,
                new FirestoreCallback<UserModel>() {
                    @Override
                    public void onSuccess(UserModel teacher) {
                        sendVerification(firebaseUser, teacher, callback);
                    }

                    @Override
                    public void onError(String message) {
                        cleanupAuthUser(firebaseUser, message, callback);
                    }
                });
    }

    private void sendVerification(FirebaseUser firebaseUser, UserModel user, FirestoreCallback<UserModel> callback) {
        firebaseUser.sendEmailVerification()
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(error -> callback.onSuccess(user));
    }

    private void cleanupAuthUser(FirebaseUser firebaseUser, String message, FirestoreCallback<UserModel> callback) {
        firebaseUser.delete()
                .addOnCompleteListener(task -> {
                    auth.signOut();
                    callback.onError(message);
                });
    }

    private String readableError(Exception error) {
        if (error instanceof FirebaseAuthInvalidCredentialsException) {
            return "Check your email and password, then try again.";
        }
        if (error instanceof FirebaseAuthUserCollisionException) {
            return "This email is already registered. Please login instead.";
        }
        if (error instanceof FirebaseNetworkException) {
            return "Network error while contacting Firebase. Please check your internet connection.";
        }
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Firestore access was denied. Publish the updated CampusCore Firestore rules first.";
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return "Firestore needs an index for onboarding. Check Firebase Console.";
            }
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Account linking failed. Please try again."
                : message;
    }
}
