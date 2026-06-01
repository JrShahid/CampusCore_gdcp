package com.example.campuscore.fragments.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.campuscore.R;
import com.example.campuscore.adapters.TeachingAssignmentsAdapter;
import com.example.campuscore.databinding.FragmentManageTeachingAssignmentsBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.repositories.TeachingAssignmentsRepository;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;

import java.util.ArrayList;
import java.util.List;

public class ManageTeachingAssignmentsFragment extends Fragment {
    private FragmentManageTeachingAssignmentsBinding binding;
    private TeachingAssignmentsRepository repository;
    private TeachingAssignmentsAdapter adapter;
    private final List<TeachingAssignmentModel> allAssignments = new ArrayList<>();
    private final List<TeachingAssignmentModel> visibleAssignments = new ArrayList<>();

    public static ManageTeachingAssignmentsFragment newInstance() {
        return new ManageTeachingAssignmentsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentManageTeachingAssignmentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new TeachingAssignmentsRepository();
        adapter = new TeachingAssignmentsAdapter(visibleAssignments, new TeachingAssignmentsAdapter.OnAssignmentActionListener() {
            @Override
            public void onEditAssignment(TeachingAssignmentModel assignment) {
                openEditAssignment(assignment);
            }

            @Override
            public void onDeleteAssignment(TeachingAssignmentModel assignment) {
                confirmDeleteAssignment(assignment);
            }
        });
        binding.assignmentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.assignmentsRecyclerView.setAdapter(adapter);
        binding.addAssignmentButton.setOnClickListener(v -> openAddAssignment());
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadAssignments);
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        loadAssignments();
    }

    private void loadAssignments() {
        setLoading(true);
        repository.fetchAssignments(new FirestoreCallback<List<TeachingAssignmentModel>>() {
            @Override
            public void onSuccess(List<TeachingAssignmentModel> data) {
                setLoading(false);
                allAssignments.clear();
                allAssignments.addAll(data);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                binding.emptyText.setVisibility(View.VISIBLE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void applyFilters() {
        List<TeachingAssignmentModel> filtered = repository.filterAssignments(
                allAssignments,
                binding.searchInput.getText() == null ? "" : binding.searchInput.getText().toString(),
                "",
                "",
                ""
        );
        adapter.submitList(filtered);
        binding.countText.setText(getString(R.string.assignments_count_format, filtered.size()));
        binding.emptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openAddAssignment() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditTeachingAssignmentFragment.newInstance(null))
                .addToBackStack(null)
                .commit();
    }

    private void openEditAssignment(TeachingAssignmentModel assignment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, AddEditTeachingAssignmentFragment.newInstance(assignment))
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteAssignment(TeachingAssignmentModel assignment) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete_assignment_title)
                .setMessage(R.string.confirm_delete_assignment_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteAssignment(assignment))
                .show();
    }

    private void deleteAssignment(TeachingAssignmentModel assignment) {
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }
        setLoading(true);
        repository.deleteAssignment(assignment, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.assignment_deleted));
                loadAssignments();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }
}
