package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemTeachingAssignmentBinding;
import com.example.campuscore.models.TeachingAssignmentModel;

import java.util.List;

public class TeachingAssignmentsAdapter extends RecyclerView.Adapter<TeachingAssignmentsAdapter.AssignmentViewHolder> {
    private final List<TeachingAssignmentModel> items;
    private final OnAssignmentActionListener listener;

    public TeachingAssignmentsAdapter(List<TeachingAssignmentModel> items, OnAssignmentActionListener listener) {
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AssignmentViewHolder(ItemTeachingAssignmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull AssignmentViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getAssignmentId().hashCode();
    }

    public void submitList(List<TeachingAssignmentModel> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    static class AssignmentViewHolder extends RecyclerView.ViewHolder {
        private final ItemTeachingAssignmentBinding binding;
        private final OnAssignmentActionListener listener;

        AssignmentViewHolder(ItemTeachingAssignmentBinding binding, OnAssignmentActionListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(TeachingAssignmentModel item) {
            binding.titleText.setText(item.displayLabel());
            String teacher = item.getTeacherName().isEmpty() ? item.getEmployeeId() : item.getTeacherName();
            binding.teacherText.setText(teacher);
            binding.statusText.setText(item.isActive()
                    ? binding.getRoot().getContext().getString(R.string.active)
                    : binding.getRoot().getContext().getString(R.string.inactive));
            binding.editButton.setOnClickListener(view -> listener.onEditAssignment(item));
            binding.deleteButton.setOnClickListener(view -> listener.onDeleteAssignment(item));
            binding.getRoot().setOnClickListener(view -> listener.onEditAssignment(item));
        }
    }

    public interface OnAssignmentActionListener {
        void onEditAssignment(TeachingAssignmentModel assignment);

        void onDeleteAssignment(TeachingAssignmentModel assignment);
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<TeachingAssignmentModel> oldItems;
        private final List<TeachingAssignmentModel> newItems;

        private DiffCallback(List<TeachingAssignmentModel> oldItems, List<TeachingAssignmentModel> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override public int getOldListSize() { return oldItems.size(); }
        @Override public int getNewListSize() { return newItems.size(); }
        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldItems.get(oldPos).getAssignmentId().equals(newItems.get(newPos).getAssignmentId());
        }
        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            TeachingAssignmentModel oldItem = oldItems.get(oldPos);
            TeachingAssignmentModel newItem = newItems.get(newPos);
            return oldItem.displayLabel().equals(newItem.displayLabel())
                    && oldItem.getTeacherUid().equals(newItem.getTeacherUid())
                    && oldItem.getTeacherName().equals(newItem.getTeacherName())
                    && oldItem.getEmployeeId().equals(newItem.getEmployeeId())
                    && oldItem.isActive() == newItem.isActive();
        }
    }
}
