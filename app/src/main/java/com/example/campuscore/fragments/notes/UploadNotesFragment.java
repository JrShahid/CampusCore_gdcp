package com.example.campuscore.fragments.notes;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.NotesAdapter;
import com.example.campuscore.databinding.FragmentUploadNotesBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.NotesModel;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.repositories.NotesRepository;
import com.example.campuscore.repositories.TeachingAssignmentsRepository;
import com.example.campuscore.utils.CloudinaryConstants;
import com.example.campuscore.utils.IntentConstants;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.PdfOpenUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class UploadNotesFragment extends Fragment {
    private FragmentUploadNotesBinding binding;
    private NotesRepository repository;
    private TeachingAssignmentsRepository assignmentsRepository;
    private final List<NotesModel> uploadedNotes = new ArrayList<>();
    private NotesAdapter notesAdapter;
    private Uri selectedPdfUri;
    private String selectedFileName = "";
    private byte[] selectedPdfBytes;
    private final List<TeachingAssignmentModel> currentAssignments = new ArrayList<>();

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    SnackbarUtils.show(binding.rootLayout, getString(R.string.file_selection_cancelled));
                    return;
                }
                validateAndStoreSelectedPdf(uri);
            });

    public static UploadNotesFragment newInstance() {
        return new UploadNotesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new NotesRepository();
        assignmentsRepository = new TeachingAssignmentsRepository();
        notesAdapter = new NotesAdapter(uploadedNotes, new NotesAdapter.OnNoteActionListener() {
            @Override
            public void onOpenNote(NotesModel note) {
                openPdf(note);
            }

            @Override
            public void onDeleteNote(NotesModel note) {
                confirmDeleteNote(note);
            }
        }, true);
        binding.notesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.notesRecyclerView.setAdapter(notesAdapter);

        setupAcademicSelectors();
        binding.selectPdfButton.setOnClickListener(v -> openFilePicker());
        binding.uploadButton.setOnClickListener(v -> uploadSelectedPdf());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadUploadedNotes);
        loadUploadedNotes();
    }

    private void setupAcademicSelectors() {
        binding.subjectSpinner.setThreshold(0);
        binding.subjectSpinner.setOnClickListener(v -> {
            if (!currentAssignments.isEmpty()) {
                binding.subjectSpinner.showDropDown();
            }
        });
        binding.subjectSpinner.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !currentAssignments.isEmpty()) {
                binding.subjectSpinner.showDropDown();
            }
        });
        binding.departmentSpinner.setVisibility(View.GONE);
        binding.semesterSpinner.setVisibility(View.GONE);
        assignmentsRepository.fetchTeacherAssignments(new FirestoreCallback<List<TeachingAssignmentModel>>() {
            @Override
            public void onSuccess(List<TeachingAssignmentModel> data) {
                bindAssignmentDropdown(data);
            }

            @Override
            public void onError(String message) {
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void bindAssignmentDropdown(List<TeachingAssignmentModel> data) {
        currentAssignments.clear();
        currentAssignments.addAll(data);
        List<String> labels = new ArrayList<>();
        for (TeachingAssignmentModel item : currentAssignments) {
            labels.add(assignmentsRepository.buildAssignmentLabel(item));
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
        boolean hasAssignments = !currentAssignments.isEmpty();
        binding.subjectSpinner.setEnabled(hasAssignments);
        binding.uploadButton.setEnabled(hasAssignments);
        binding.emptyText.setText(currentAssignments.isEmpty()
                ? getString(R.string.no_teaching_assignments_help)
                : getString(R.string.teacher_notes_empty));
        binding.emptyText.setVisibility(currentAssignments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openFilePicker() {
        filePickerLauncher.launch(new String[]{CloudinaryConstants.MIME_TYPE_PDF});
    }

    private void validateAndStoreSelectedPdf(Uri uri) {
        String mimeType = requireContext().getContentResolver().getType(uri);
        PdfFileMeta meta = readPdfMeta(uri);
        boolean looksLikePdf = CloudinaryConstants.MIME_TYPE_PDF.equalsIgnoreCase(mimeType)
                || meta.displayName.toLowerCase().endsWith(".pdf");
        if (!looksLikePdf) {
            clearSelectedPdf();
            SnackbarUtils.show(binding.rootLayout, getString(R.string.pdf_only_error));
            return;
        }

        if (meta.sizeBytes > CloudinaryConstants.MAX_PDF_SIZE_BYTES) {
            clearSelectedPdf();
            SnackbarUtils.show(binding.rootLayout, getString(R.string.pdf_size_error));
            return;
        }

        byte[] bytes;
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                clearSelectedPdf();
                SnackbarUtils.show(binding.rootLayout, getString(R.string.pdf_only_error));
                return;
            }
            bytes = getBytes(inputStream);
        } catch (IOException error) {
            clearSelectedPdf();
            SnackbarUtils.show(binding.rootLayout, "Unable to read the selected PDF.");
            return;
        }
        if (bytes.length > CloudinaryConstants.MAX_PDF_SIZE_BYTES) {
            clearSelectedPdf();
            SnackbarUtils.show(binding.rootLayout, getString(R.string.pdf_size_error));
            return;
        }

        selectedPdfUri = uri;
        selectedFileName = meta.displayName;
        selectedPdfBytes = bytes;
        binding.fileNameText.setText(meta.displayName);
        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
        }
    }

    private void uploadSelectedPdf() {
        binding.titleLayout.setError(null);
        String title = binding.titleInput.getText() == null ? "" : binding.titleInput.getText().toString().trim();
        if (ValidationUtils.isBlank(title)) {
            binding.titleLayout.setError(getString(R.string.notes_title_required));
            return;
        }
        if (selectedPdfUri == null || selectedPdfBytes == null) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.select_valid_file_first));
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.upload_requires_network));
            return;
        }

        TeachingAssignmentModel assignment = selectedAssignment();
        setUploadingState(true, 0);
        repository.uploadNote(
                selectedPdfBytes,
                title,
                assignment,
                teacherName(),
                selectedFileName,
                new NotesRepository.UploadNotesCallback() {
                    @Override
                    public void onProgress(int progressPercent) {
                        setUploadingState(true, progressPercent);
                    }

                    @Override
                    public void onSuccess() {
                        setUploadingState(false, 100);
                        resetFormAfterUpload();
                        SnackbarUtils.show(binding.rootLayout, getString(R.string.notes_uploaded));
                        loadUploadedNotes();
                    }

                    @Override
                    public void onError(String message) {
                        setUploadingState(false, 0);
                        SnackbarUtils.show(binding.rootLayout, message);
                    }
                }
        );
    }

    private void loadUploadedNotes() {
        if (!NetworkUtils.isOnline(requireContext())) {
            binding.swipeRefreshLayout.setRefreshing(false);
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }

        binding.listProgressBar.setVisibility(View.VISIBLE);
        repository.fetchTeacherNotes(new FirestoreCallback<List<NotesModel>>() {
            @Override
            public void onSuccess(List<NotesModel> data) {
                binding.listProgressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                notesAdapter.submitList(data);
                binding.emptyText.setVisibility(uploadedNotes.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                binding.listProgressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.emptyText.setVisibility(uploadedNotes.isEmpty() ? View.VISIBLE : View.GONE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void openPdf(NotesModel note) {
        if (ValidationUtils.isBlank(note.getPdfUrl())) {
            Log.e(PdfOpenUtils.TAG, "Teacher note click stopped: empty pdfUrl noteId=" + note.getNoteId());
            SnackbarUtils.show(binding.rootLayout, getString(R.string.notes_open_error));
            return;
        }
        try {
            Log.d(PdfOpenUtils.TAG, "Teacher note click noteId=" + note.getNoteId() + " pdfUrl=" + note.getPdfUrl());
            PdfOpenUtils.openRemotePdf(requireContext(), note.getNoteId(), note.getPdfUrl(), new PdfOpenUtils.OpenPdfCallback() {
                @Override
                public void onDownloadStart() {
                    binding.listProgressBar.setVisibility(View.VISIBLE);
                    SnackbarUtils.show(binding.rootLayout, getString(R.string.download_ready));
                }

                @Override
                public void onViewerLaunch() {
                    binding.listProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    binding.listProgressBar.setVisibility(View.GONE);
                    SnackbarUtils.show(binding.rootLayout, getString(R.string.notes_open_error));
                }
            });
        } catch (Exception error) {
            Log.e(PdfOpenUtils.TAG, "Teacher note click exception noteId=" + note.getNoteId(), error);
            binding.listProgressBar.setVisibility(View.GONE);
            SnackbarUtils.show(binding.rootLayout, getString(R.string.notes_open_error));
        }
    }

    private void confirmDeleteNote(NotesModel note) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_note_title)
                .setMessage(R.string.confirm_delete_note_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteNote(note))
                .show();
    }

    private void deleteNote(NotesModel note) {
        binding.listProgressBar.setVisibility(View.VISIBLE);
        repository.deleteNote(note, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                binding.listProgressBar.setVisibility(View.GONE);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.note_deleted));
                loadUploadedNotes();
            }

            @Override
            public void onError(String message) {
                binding.listProgressBar.setVisibility(View.GONE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void setUploadingState(boolean loading, int progress) {
        binding.uploadButton.setEnabled(!loading && !currentAssignments.isEmpty());
        binding.selectPdfButton.setEnabled(!loading);
        binding.subjectSpinner.setEnabled(!loading && !currentAssignments.isEmpty());
        binding.uploadProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.uploadProgressText.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.uploadProgressBar.setProgress(progress);
        binding.uploadProgressText.setText(getString(R.string.upload_progress_format, progress));
    }

    private void resetFormAfterUpload() {
        binding.titleInput.setText("");
        clearSelectedPdf();
    }

    private void clearSelectedPdf() {
        selectedPdfUri = null;
        selectedFileName = "";
        selectedPdfBytes = null;
        binding.fileNameText.setText(R.string.no_file_selected);
    }

    private TeachingAssignmentModel selectedAssignment() {
        String selected = binding.subjectSpinner.getText().toString().trim();
        for (TeachingAssignmentModel item : currentAssignments) {
            if (assignmentsRepository.buildAssignmentLabel(item).equals(selected)) {
                return item;
            }
        }
        return currentAssignments.isEmpty() ? new TeachingAssignmentModel() : currentAssignments.get(0);
    }

    private String teacherName() {
        String value = requireActivity().getIntent().getStringExtra(IntentConstants.EXTRA_USER_NAME);
        return value == null || value.trim().isEmpty() ? "Teacher" : value;
    }

    private PdfFileMeta readPdfMeta(Uri uri) {
        Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
        String name = "notes.pdf";
        long size = 0L;
        if (cursor != null) {
            try {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex);
                    }
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return new PdfFileMeta(name, size);
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8 * 1024];
        int read;
        while ((read = inputStream.read(data)) != -1) {
            buffer.write(data, 0, read);
        }
        return buffer.toByteArray();
    }

    private static class PdfFileMeta {
        private final String displayName;
        private final long sizeBytes;

        private PdfFileMeta(String displayName, long sizeBytes) {
            this.displayName = displayName;
            this.sizeBytes = sizeBytes;
        }
    }
}
