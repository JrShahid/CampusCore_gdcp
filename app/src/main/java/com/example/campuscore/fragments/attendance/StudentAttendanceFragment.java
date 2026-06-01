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
import com.example.campuscore.adapters.AttendanceHistoryAdapter;
import com.example.campuscore.adapters.SubjectAttendanceSummaryAdapter;
import com.example.campuscore.databinding.FragmentStudentAttendanceBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.AttendanceModel;
import com.example.campuscore.models.SubjectAttendanceSummary;
import com.example.campuscore.repositories.AttendanceRepository;
import com.example.campuscore.utils.AttendanceConstants;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StudentAttendanceFragment extends Fragment {
    private FragmentStudentAttendanceBinding binding;
    private AttendanceRepository repository;
    private final List<AttendanceModel> allRecords = new ArrayList<>();
    private final List<AttendanceModel> filteredHistory = new ArrayList<>();
    private final List<SubjectAttendanceSummary> subjectSummaries = new ArrayList<>();
    private AttendanceHistoryAdapter historyAdapter;
    private SubjectAttendanceSummaryAdapter summaryAdapter;

    public static StudentAttendanceFragment newInstance() {
        return new StudentAttendanceFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new AttendanceRepository();
        historyAdapter = new AttendanceHistoryAdapter(filteredHistory, false);
        summaryAdapter = new SubjectAttendanceSummaryAdapter(subjectSummaries);

        binding.historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.historyRecyclerView.setAdapter(historyAdapter);

        binding.subjectSummaryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.subjectSummaryRecyclerView.setAdapter(summaryAdapter);

        binding.subjectFilterSpinner.setOnItemClickListener((parent, view1, position, id) -> applyFilters());
        binding.downloadAttendanceButton.setOnClickListener(v ->
                SnackbarUtils.show(binding.rootLayout, getString(R.string.available_soon)));
        loadAttendance();
    }

    private void loadAttendance() {
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.query_requires_network));
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        repository.fetchStudentAttendanceRecords(new FirestoreCallback<List<AttendanceModel>>() {
            @Override
            public void onSuccess(List<AttendanceModel> data) {
                binding.progressBar.setVisibility(View.GONE);
                allRecords.clear();
                allRecords.addAll(data);
                sortLatestFirst(allRecords);
                setupSubjectFilter(allRecords);
                rebuildSubjectSummary(allRecords);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.emptyText.setVisibility(View.VISIBLE);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private void setupSubjectFilter(List<AttendanceModel> records) {
        Set<String> subjects = new LinkedHashSet<>();
        subjects.add(getString(R.string.all_subjects));
        for (AttendanceModel record : records) {
            if (!record.getSubject().isEmpty()) {
                subjects.add(record.getSubject());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<>(subjects)
        );
        binding.subjectFilterSpinner.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            binding.subjectFilterSpinner.setText(adapter.getItem(0), false);
        }
    }

    private void rebuildSubjectSummary(List<AttendanceModel> records) {
        Map<String, int[]> counters = new LinkedHashMap<>();
        for (AttendanceModel record : records) {
            int[] counter = counters.containsKey(record.getSubject()) ? counters.get(record.getSubject()) : new int[]{0, 0};
            counter[1] = counter[1] + 1;
            if (AttendanceConstants.STATUS_PRESENT.equalsIgnoreCase(record.getStatus())) {
                counter[0] = counter[0] + 1;
            }
            counters.put(record.getSubject(), counter);
        }

        List<SubjectAttendanceSummary> nextSummaries = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : counters.entrySet()) {
            int present = entry.getValue()[0];
            int total = entry.getValue()[1];
            double percentage = total == 0 ? 0d : (present * 100d) / total;
            nextSummaries.add(new SubjectAttendanceSummary(entry.getKey(), present, total, percentage));
        }
        summaryAdapter.submitList(nextSummaries);
    }

    private void applyFilters() {
        String selectedSubject = binding.subjectFilterSpinner.getText().toString().trim();
        List<AttendanceModel> nextHistory = new ArrayList<>();
        int presentCount = 0;
        int totalCount = 0;

        for (AttendanceModel record : allRecords) {
            boolean include = selectedSubject.isEmpty()
                    || getString(R.string.all_subjects).equals(selectedSubject)
                    || selectedSubject.equals(record.getSubject());
            if (include) {
                nextHistory.add(record);
                totalCount++;
                if (AttendanceConstants.STATUS_PRESENT.equalsIgnoreCase(record.getStatus())) {
                    presentCount++;
                }
            }
        }

        sortLatestFirst(nextHistory);
        historyAdapter.submitList(nextHistory);
        updateSummaryCard(presentCount, totalCount);
        binding.emptyText.setVisibility(filteredHistory.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void updateSummaryCard(int presentCount, int totalCount) {
        double percentage = totalCount == 0 ? 0d : (presentCount * 100d) / totalCount;
        binding.percentageText.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));
        binding.ratioText.setText(getString(R.string.attendance_ratio_format, presentCount, totalCount));
    }

    private void sortLatestFirst(List<AttendanceModel> items) {
        Collections.sort(items, (first, second) -> second.getDate().compareTo(first.getDate()));
    }
}
