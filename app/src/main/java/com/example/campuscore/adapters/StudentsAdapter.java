package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemStudentManagementBinding;
import com.example.campuscore.models.UserModel;

import java.util.List;

public class StudentsAdapter extends RecyclerView.Adapter<StudentsAdapter.StudentViewHolder> {
    private final List<UserModel> items;
    private final OnStudentActionListener listener;

    public StudentsAdapter(List<UserModel> items, OnStudentActionListener listener) {
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentManagementBinding binding = ItemStudentManagementBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new StudentViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getUid().hashCode();
    }

    public void submitList(List<UserModel> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentManagementBinding binding;
        private final OnStudentActionListener listener;

        StudentViewHolder(ItemStudentManagementBinding binding, OnStudentActionListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(UserModel student) {
            binding.nameText.setText(student.getName());
            binding.rollText.setText(binding.getRoot().getContext().getString(
                    R.string.student_roll_registration_format,
                    student.getRollNumber(),
                    student.getRegistrationNumber()
            ));
            binding.academicText.setText(binding.getRoot().getContext().getString(
                    R.string.student_academic_format,
                    student.getDepartmentId(),
                    student.getSemester(),
                    student.getSection()
            ));
            binding.emailText.setText(student.getEmail());
            binding.editButton.setOnClickListener(view -> listener.onEditStudent(student));
            binding.deleteButton.setOnClickListener(view -> listener.onDeleteStudent(student));
            binding.getRoot().setOnClickListener(view -> listener.onEditStudent(student));
        }
    }

    public interface OnStudentActionListener {
        void onEditStudent(UserModel student);

        void onDeleteStudent(UserModel student);
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<UserModel> oldItems;
        private final List<UserModel> newItems;

        private DiffCallback(List<UserModel> oldItems, List<UserModel> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldItems.get(oldItemPosition).getUid().equals(newItems.get(newItemPosition).getUid());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            UserModel oldItem = oldItems.get(oldItemPosition);
            UserModel newItem = newItems.get(newItemPosition);
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getEmail().equals(newItem.getEmail())
                    && oldItem.getRollNumber().equals(newItem.getRollNumber())
                    && oldItem.getRegistrationNumber().equals(newItem.getRegistrationNumber())
                    && oldItem.getDepartmentId().equals(newItem.getDepartmentId())
                    && oldItem.getSemester().equals(newItem.getSemester())
                    && oldItem.getSection().equals(newItem.getSection())
                    && oldItem.getBatch().equals(newItem.getBatch());
        }
    }
}
