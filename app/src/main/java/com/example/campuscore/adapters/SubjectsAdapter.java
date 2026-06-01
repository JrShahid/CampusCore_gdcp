package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemSubjectBinding;
import com.example.campuscore.models.SubjectModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubjectsAdapter extends RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder> {
    private final List<SubjectModel> items;
    private final OnSubjectActionListener listener;
    private final Map<String, String> departmentLabels = new HashMap<>();

    public SubjectsAdapter(List<SubjectModel> items, OnSubjectActionListener listener) {
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSubjectBinding binding = ItemSubjectBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new SubjectViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        holder.bind(items.get(position), departmentLabels);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getSubjectCode().hashCode();
    }

    public void submitList(List<SubjectModel> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setDepartmentLabels(Map<String, String> labels) {
        departmentLabels.clear();
        departmentLabels.putAll(labels);
        notifyDataSetChanged();
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        private final ItemSubjectBinding binding;
        private final OnSubjectActionListener listener;

        SubjectViewHolder(ItemSubjectBinding binding, OnSubjectActionListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(SubjectModel subject, Map<String, String> departmentLabels) {
            binding.nameText.setText(subject.getSubjectName());
            binding.codeText.setText(subject.getSubjectCode());
            String departmentLabel = departmentLabels.containsKey(subject.getDepartmentId())
                    ? departmentLabels.get(subject.getDepartmentId())
                    : subject.getDepartmentId();
            binding.academicText.setText(binding.getRoot().getContext().getString(
                    R.string.subject_academic_format,
                    departmentLabel,
                    subject.getSemester()
            ));
            binding.statusText.setText(subject.isActive()
                    ? binding.getRoot().getContext().getString(R.string.active)
                    : binding.getRoot().getContext().getString(R.string.inactive));
            binding.editButton.setOnClickListener(view -> listener.onEditSubject(subject));
            binding.deleteButton.setOnClickListener(view -> listener.onDeleteSubject(subject));
            binding.getRoot().setOnClickListener(view -> listener.onEditSubject(subject));
        }
    }

    public interface OnSubjectActionListener {
        void onEditSubject(SubjectModel subject);

        void onDeleteSubject(SubjectModel subject);
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<SubjectModel> oldItems;
        private final List<SubjectModel> newItems;

        private DiffCallback(List<SubjectModel> oldItems, List<SubjectModel> newItems) {
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
            return oldItems.get(oldItemPosition).getSubjectCode()
                    .equals(newItems.get(newItemPosition).getSubjectCode());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            SubjectModel oldItem = oldItems.get(oldItemPosition);
            SubjectModel newItem = newItems.get(newItemPosition);
            return oldItem.getSubjectName().equals(newItem.getSubjectName())
                    && oldItem.getDepartmentId().equals(newItem.getDepartmentId())
                    && oldItem.getSemester().equals(newItem.getSemester())
                    && oldItem.isActive() == newItem.isActive();
        }
    }
}
