package com.example.campuscore.fragments.notes;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.NotesAdapter;
import com.example.campuscore.databinding.FragmentStudentNotesBinding;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.NotesModel;
import com.example.campuscore.models.UserModel;
import com.example.campuscore.repositories.NotesRepository;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.PdfOpenUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StudentNotesFragment extends Fragment {
    private FragmentStudentNotesBinding binding;
    private NotesRepository notesRepository;
    private FirebaseUserRepository userRepository;
    private final List<NotesModel> allNotes = new ArrayList<>();
    private final List<NotesModel> filteredNotes = new ArrayList<>();
    private NotesAdapter notesAdapter;

    public static StudentNotesFragment newInstance() {
        return new StudentNotesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentNotesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        notesRepository = new NotesRepository();
        userRepository = new FirebaseUserRepository();
        notesAdapter = new NotesAdapter(filteredNotes, this::openPdf);
        binding.notesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.notesRecyclerView.setAdapter(notesAdapter);
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadStudentNotes);
        binding.subjectFilterSpinner.setOnItemClickListener((parent, view1, position, id) -> applySubjectFilter());
        loadStudentNotes();
    }

    private void loadStudentNotes() {
        if (!NetworkUtils.isOnline(requireContext())) {
            binding.swipeRefreshLayout.setRefreshing(false);
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        userRepository.fetchCurrentUser(new FirestoreCallback<UserModel>() {
            @Override
            public void onSuccess(UserModel user) {
                notesRepository.fetchStudentNotes(user, new FirestoreCallback<List<NotesModel>>() {
                    @Override
                    public void onSuccess(List<NotesModel> data) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.swipeRefreshLayout.setRefreshing(false);
                        allNotes.clear();
                        allNotes.addAll(data);
                        setupSubjectFilter();
                        applySubjectFilter();
                    }

                    @Override
                    public void onError(String message) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.swipeRefreshLayout.setRefreshing(false);
                        binding.emptyText.setVisibility(allNotes.isEmpty() ? View.VISIBLE : View.GONE);
                        SnackbarUtils.show(binding.rootLayout, message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.emptyText.setVisibility(View.VISIBLE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void setupSubjectFilter() {
        Set<String> options = new LinkedHashSet<>();
        options.add(getString(R.string.subject_filter_all));
        for (NotesModel note : allNotes) {
            options.add(note.getSubjectCode());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<>(options)
        );
        binding.subjectFilterSpinner.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            binding.subjectFilterSpinner.setText(adapter.getItem(0), false);
        }
    }

    private void applySubjectFilter() {
        String selected = binding.subjectFilterSpinner.getText().toString().trim();
        List<NotesModel> nextNotes = new ArrayList<>();
        for (NotesModel note : allNotes) {
            if (selected.isEmpty()
                    || getString(R.string.subject_filter_all).equals(selected)
                    || selected.equals(note.getSubjectCode())) {
                nextNotes.add(note);
            }
        }
        notesAdapter.submitList(nextNotes);
        binding.emptyText.setVisibility(filteredNotes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openPdf(NotesModel note) {
        if (ValidationUtils.isBlank(note.getPdfUrl())) {
            Log.e(PdfOpenUtils.TAG, "Student note click stopped: empty pdfUrl noteId=" + note.getNoteId());
            SnackbarUtils.show(binding.rootLayout, getString(R.string.notes_open_error));
            return;
        }
        try {
            Log.d(PdfOpenUtils.TAG, "Student note click noteId=" + note.getNoteId() + " pdfUrl=" + note.getPdfUrl());
            PdfOpenUtils.openRemotePdf(requireContext(), note.getNoteId(), note.getPdfUrl(), new PdfOpenUtils.OpenPdfCallback() {
                @Override
                public void onDownloadStart() {
                    binding.progressBar.setVisibility(View.VISIBLE);
                    SnackbarUtils.show(binding.rootLayout, getString(R.string.download_ready));
                }

                @Override
                public void onViewerLaunch() {
                    binding.progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError() {
                    binding.progressBar.setVisibility(View.GONE);
                    SnackbarUtils.show(binding.rootLayout, getString(R.string.notes_open_error));
                }
            });
        } catch (Exception error) {
            Log.e(PdfOpenUtils.TAG, "Student note click exception noteId=" + note.getNoteId(), error);
            binding.progressBar.setVisibility(View.GONE);
            SnackbarUtils.show(binding.rootLayout, getString(R.string.notes_open_error));
        }
    }
}
