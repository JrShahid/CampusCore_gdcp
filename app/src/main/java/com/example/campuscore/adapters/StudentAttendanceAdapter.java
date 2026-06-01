package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemStudentAttendanceBinding;
import com.example.campuscore.models.StudentAttendanceItem;

import java.util.List;

public class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.StudentAttendanceViewHolder> {
    private final List<StudentAttendanceItem> items;
    private boolean editable = true;

    public StudentAttendanceAdapter(List<StudentAttendanceItem> items) {
        this.items = items;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public StudentAttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentAttendanceBinding binding = ItemStudentAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new StudentAttendanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentAttendanceViewHolder holder, int position) {
        holder.bind(items.get(position), editable);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getUser().getUid().hashCode();
    }

    public void submitList(List<StudentAttendanceItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        notifyDataSetChanged();
    }

    static class StudentAttendanceViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentAttendanceBinding binding;

        StudentAttendanceViewHolder(ItemStudentAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(StudentAttendanceItem item, boolean editable) {
            binding.nameText.setText(item.getUser().getName());
            String rollNumber = item.getUser().getRollNumber().isEmpty()
                    ? binding.getRoot().getContext().getString(R.string.roll_number_missing)
                    : item.getUser().getRollNumber();
            binding.rollText.setText(binding.getRoot().getContext().getString(R.string.roll_number) + ": " + rollNumber);
            binding.presentSwitch.setOnCheckedChangeListener(null);
            binding.presentSwitch.setChecked(item.isPresent());
            binding.presentSwitch.setEnabled(editable);
            binding.presentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> item.setPresent(isChecked));
        }
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<StudentAttendanceItem> oldItems;
        private final List<StudentAttendanceItem> newItems;

        private DiffCallback(List<StudentAttendanceItem> oldItems, List<StudentAttendanceItem> newItems) {
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
            return oldItems.get(oldItemPosition).getUser().getUid()
                    .equals(newItems.get(newItemPosition).getUser().getUid());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            StudentAttendanceItem oldItem = oldItems.get(oldItemPosition);
            StudentAttendanceItem newItem = newItems.get(newItemPosition);
            return oldItem.isPresent() == newItem.isPresent()
                    && oldItem.getUser().getName().equals(newItem.getUser().getName())
                    && oldItem.getUser().getRollNumber().equals(newItem.getUser().getRollNumber());
        }
    }
}
