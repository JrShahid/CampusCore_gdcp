package com.example.campuscore.fragments.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.campuscore.R;
import com.example.campuscore.databinding.FragmentAddEditDepartmentBinding;
import com.example.campuscore.firebase.FirestoreCallback;
import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.repositories.AcademicStructureRepository;
import com.example.campuscore.utils.NetworkUtils;
import com.example.campuscore.utils.SnackbarUtils;
import com.example.campuscore.utils.ValidationUtils;

public class AddEditDepartmentFragment extends Fragment {
    private static final String ARG_ID = "departmentId";
    private static final String ARG_NAME = "departmentName";
    private static final String ARG_ACTIVE = "active";

    private FragmentAddEditDepartmentBinding binding;
    private AcademicStructureRepository repository;
    private String originalDepartmentId = "";

    public static AddEditDepartmentFragment newInstance(@Nullable DepartmentModel department) {
        AddEditDepartmentFragment fragment = new AddEditDepartmentFragment();
        Bundle args = new Bundle();
        if (department != null) {
            args.putString(ARG_ID, department.getDepartmentId());
            args.putString(ARG_NAME, department.getDepartmentName());
            args.putBoolean(ARG_ACTIVE, department.isActive());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditDepartmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        repository = new AcademicStructureRepository();
        bindExistingDepartment();
        binding.activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updateActiveText());
        updateActiveText();
        binding.saveButton.setOnClickListener(v -> saveDepartment());
        binding.cancelButton.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void bindExistingDepartment() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey(ARG_ID)) {
            binding.screenTitle.setText(R.string.add_department);
            return;
        }
        binding.screenTitle.setText(R.string.edit_department);
        originalDepartmentId = args.getString(ARG_ID, "");
        binding.departmentIdInput.setText(originalDepartmentId);
        binding.departmentIdInput.setEnabled(false);
        binding.departmentNameInput.setText(args.getString(ARG_NAME, ""));
        binding.activeSwitch.setChecked(args.getBoolean(ARG_ACTIVE, true));
        updateActiveText();
    }

    private void saveDepartment() {
        clearErrors();
        DepartmentModel department = new DepartmentModel(
                text(binding.departmentIdInput.getText()).toUpperCase(),
                text(binding.departmentNameInput.getText()),
                binding.activeSwitch.isChecked()
        );
        if (!validate(department)) {
            return;
        }
        if (!NetworkUtils.isOnline(requireContext())) {
            SnackbarUtils.show(binding.rootLayout, getString(R.string.error_no_internet));
            return;
        }

        setLoading(true);
        repository.saveDepartment(department, originalDepartmentId, new FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, getString(R.string.department_saved));
                requireActivity().getSupportFragmentManager().popBackStack();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                SnackbarUtils.show(binding.rootLayout, message);
            }
        });
    }

    private boolean validate(DepartmentModel department) {
        boolean valid = true;
        if (ValidationUtils.isBlank(department.getDepartmentId())) {
            binding.departmentIdLayout.setError(getString(R.string.error_required));
            valid = false;
        } else if (!department.getDepartmentId().matches("[A-Z0-9_-]{2,20}")) {
            binding.departmentIdLayout.setError(getString(R.string.error_invalid_academic_code));
            valid = false;
        }
        if (ValidationUtils.isBlank(department.getDepartmentName())) {
            binding.departmentNameLayout.setError(getString(R.string.error_required));
            valid = false;
        }
        return valid;
    }

    private void updateActiveText() {
        binding.activeSwitch.setText(binding.activeSwitch.isChecked()
                ? getString(R.string.active)
                : getString(R.string.inactive));
    }

    private void clearErrors() {
        binding.departmentIdLayout.setError(null);
        binding.departmentNameLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.saveButton.setEnabled(!loading);
        binding.cancelButton.setEnabled(!loading);
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
