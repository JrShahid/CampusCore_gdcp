package com.example.campuscore.repositories;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.NotesModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.utils.AcademicDataProvider;
import com.example.campuscore.utils.CloudinaryConstants;
import com.example.campuscore.utils.FirestoreCollections;
import com.example.campuscore.utils.FirestoreFields;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotesRepository {
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    private final OkHttpClient httpClient;
    private final Handler mainHandler;

    public NotesRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        httpClient = new OkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void uploadNote(byte[] pdfBytes, String title, String departmentName, String semesterValue,
                           String subjectCode, String subjectName, String teacherName, String fileName,
                           UploadNotesCallback callback) {
        uploadNote(pdfBytes, title, departmentName, semesterValue, subjectCode, subjectName, "", teacherName, fileName, callback);
    }

    private void uploadNote(byte[] pdfBytes, String title, String departmentName, String semesterValue,
                            String subjectCode, String subjectName, String assignmentId, String teacherName,
                            String fileName, UploadNotesCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        String noteId = firestore.collection(FirestoreCollections.NOTES).document().getId();
        UploadNotesCallback mainThreadCallback = new UploadNotesCallback() {
            @Override
            public void onProgress(int progressPercent) {
                mainHandler.post(() -> callback.onProgress(progressPercent));
            }

            @Override
            public void onSuccess() {
                mainHandler.post(callback::onSuccess);
            }

            @Override
            public void onError(String message) {
                mainHandler.post(() -> callback.onError(message));
            }
        };
        RequestBody pdfBody = new ProgressRequestBody(
                pdfBytes,
                MediaType.parse(CloudinaryConstants.MIME_TYPE_PDF),
                mainThreadCallback
        );
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", CloudinaryConstants.UNSIGNED_UPLOAD_PRESET)
                .addFormDataPart("file", fileName, pdfBody)
                .build();

        Request request = new Request.Builder()
                .url(CloudinaryConstants.RAW_UPLOAD_URL)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainThreadCallback.onError(readableError(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    mainThreadCallback.onError(parseCloudinaryError(responseBody));
                    return;
                }

                String secureUrl;
                try {
                    JSONObject jsonObject = new JSONObject(responseBody);
                    secureUrl = jsonObject.optString("secure_url", "");
                } catch (JSONException exception) {
                    mainThreadCallback.onError("Cloudinary returned an invalid response.");
                    return;
                }

                if (secureUrl.trim().isEmpty() || !secureUrl.startsWith("https://")) {
                    mainThreadCallback.onError("Cloudinary did not return a secure PDF URL.");
                    return;
                }

                Map<String, Object> noteMap = new HashMap<>();
                noteMap.put(FirestoreFields.NOTE_ID, noteId);
                noteMap.put(FirestoreFields.TITLE, title);
                noteMap.put(FirestoreFields.SUBJECT_CODE, subjectCode);
                noteMap.put(FirestoreFields.SUBJECT_NAME, subjectName);
                noteMap.put(FirestoreFields.ASSIGNMENT_ID, assignmentId);
                noteMap.put(FirestoreFields.DEPARTMENT, departmentName);
                noteMap.put(FirestoreFields.SEMESTER, semesterValue);
                noteMap.put(FirestoreFields.UPLOADED_BY_UID, currentUser.getUid());
                noteMap.put(FirestoreFields.UPLOADED_BY_NAME, teacherName);
                noteMap.put(FirestoreFields.PDF_URL, secureUrl);
                noteMap.put(FirestoreFields.FILE_NAME, fileName);
                noteMap.put(FirestoreFields.TIMESTAMP, FieldValue.serverTimestamp());

                firestore.collection(FirestoreCollections.NOTES)
                        .document(noteId)
                        .set(noteMap)
                        .addOnSuccessListener(unused -> mainThreadCallback.onSuccess())
                        .addOnFailureListener(error -> mainThreadCallback.onError(readableError(error)));
            }
        });
    }

    public void uploadNote(byte[] pdfBytes, String title, SubjectModel subject, String teacherName, String fileName,
                           UploadNotesCallback callback) {
        uploadNote(pdfBytes, title, subject.getDepartmentId(), subject.getSemester(), subject.getSubjectCode(),
                subject.getSubjectName(), teacherName, fileName, callback);
    }

    public void uploadNote(byte[] pdfBytes, String title, TeachingAssignmentModel assignment, String teacherName,
                           String fileName, UploadNotesCallback callback) {
        uploadNote(pdfBytes, title, assignment.getDepartmentId(), assignment.getSemester(), assignment.getSubjectCode(),
                assignment.getSubjectName(), assignment.getAssignmentId(), teacherName, fileName, callback);
    }

    public void fetchTeacherNotes(FirestoreCallback<List<NotesModel>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        firestore.collection(FirestoreCollections.NOTES)
                .whereEqualTo(FirestoreFields.UPLOADED_BY_UID, currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<NotesModel> notes = parseNotes(queryDocumentSnapshots.getDocuments());
                    sortNotes(notes);
                    callback.onSuccess(notes);
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchStudentNotes(String department, String semester, FirestoreCallback<List<NotesModel>> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }

        firestore.collection(FirestoreCollections.NOTES)
                .whereEqualTo(FirestoreFields.DEPARTMENT, department)
                .whereEqualTo(FirestoreFields.SEMESTER, semester)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<NotesModel> notes = parseNotes(queryDocumentSnapshots.getDocuments());
                    sortNotes(notes);
                    String departmentCode = AcademicDataProvider.departmentCode(department);
                    String legacyDepartment = AcademicDataProvider.departmentNameForCode(department);
                    String alternateDepartment = AcademicDataProvider.isDepartmentCode(department)
                            ? legacyDepartment
                            : departmentCode;
                    if (alternateDepartment.equals(department)) {
                        callback.onSuccess(notes);
                        return;
                    }
                    firestore.collection(FirestoreCollections.NOTES)
                            .whereEqualTo(FirestoreFields.DEPARTMENT, alternateDepartment)
                            .whereEqualTo(FirestoreFields.SEMESTER, semester)
                            .get()
                            .addOnSuccessListener(alternateSnapshots -> {
                                notes.addAll(parseNotes(alternateSnapshots.getDocuments()));
                                sortNotes(notes);
                                callback.onSuccess(notes);
                            })
                            .addOnFailureListener(error -> callback.onSuccess(notes));
                })
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    public void fetchStudentNotes(UserModel student, FirestoreCallback<List<NotesModel>> callback) {
        fetchStudentNotes(student.getDepartmentId(), student.getSemester(), callback);
    }

    public void deleteNote(NotesModel note, FirestoreCallback<Void> callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("Session expired. Please login again.");
            return;
        }
        if (note == null || note.getNoteId().trim().isEmpty()) {
            callback.onError("Unable to delete this note.");
            return;
        }

        firestore.collection(FirestoreCollections.NOTES)
                .document(note.getNoteId())
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> callback.onError(readableError(error)));
    }

    private List<NotesModel> parseNotes(List<DocumentSnapshot> documents) {
        List<NotesModel> notes = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            NotesModel note = document.toObject(NotesModel.class);
            if (note != null) {
                notes.add(note);
            }
        }
        return notes;
    }

    private void sortNotes(List<NotesModel> notes) {
        Collections.sort(notes, (first, second) -> {
            if (first.getTimestamp() == null || second.getTimestamp() == null) {
                return 0;
            }
            return second.getTimestamp().compareTo(first.getTimestamp());
        });
    }

    private String readableError(@NonNull Exception error) {
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) error;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Notes access was denied by Firestore rules.";
            }
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                return "Notes query needs a Firestore index. Check the Firebase Console for the suggested index.";
            }
        }
        if (error instanceof FirebaseNetworkException) {
            return "Network error while contacting Firebase. Please check your internet connection.";
        }
        if (error instanceof IOException) {
            return "Cloudinary upload failed. Please check your internet connection and try again.";
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Notes request failed. Please try again."
                : message;
    }

    private String parseCloudinaryError(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "Cloudinary upload failed. Please try again.";
        }
        try {
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONObject error = jsonObject.optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "");
                if (!message.trim().isEmpty()) {
                    return message;
                }
            }
        } catch (JSONException ignored) {
        }
        return "Cloudinary upload failed. Please try again.";
    }

    private static final class ProgressRequestBody extends RequestBody {
        private final byte[] data;
        private final MediaType mediaType;
        private final UploadNotesCallback callback;

        private ProgressRequestBody(byte[] data, MediaType mediaType, UploadNotesCallback callback) {
            this.data = data;
            this.mediaType = mediaType;
            this.callback = callback;
        }

        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public long contentLength() {
            return data.length;
        }

        @Override
        public void writeTo(@NonNull okio.BufferedSink sink) throws IOException {
            long total = data.length;
            int chunkSize = 8 * 1024;
            long written = 0L;
            while (written < total) {
                int toWrite = (int) Math.min(chunkSize, total - written);
                sink.write(data, (int) written, toWrite);
                written += toWrite;
                int progress = total == 0 ? 0 : (int) ((written * 100) / total);
                callback.onProgress(progress);
            }
        }
    }

    public interface UploadNotesCallback {
        void onProgress(int progressPercent);

        void onSuccess();

        void onError(String message);
    }
}
