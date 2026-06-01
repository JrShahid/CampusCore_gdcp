package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemDepartmentBinding;
import com.example.campuscore.models.DepartmentModel;

import java.util.List;

public class DepartmentsAdapter extends RecyclerView.Adapter<DepartmentsAdapter.DepartmentViewHolder> {
    private final List<DepartmentModel> items;
    private final OnDepartmentActionListener listener;

    public DepartmentsAdapter(List<DepartmentModel> items, OnDepartmentActionListener listener) {
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public DepartmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDepartmentBinding binding = ItemDepartmentBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new DepartmentViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull DepartmentViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getDepartmentId().hashCode();
    }

    public void submitList(List<DepartmentModel> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    static class DepartmentViewHolder extends RecyclerView.ViewHolder {
        private final ItemDepartmentBinding binding;
        private final OnDepartmentActionListener listener;

        DepartmentViewHolder(ItemDepartmentBinding binding, OnDepartmentActionListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(DepartmentModel department) {
            binding.nameText.setText(department.getDepartmentName());
            binding.codeText.setText(department.getDepartmentId());
            binding.statusText.setText(department.isActive()
                    ? binding.getRoot().getContext().getString(R.string.active)
                    : binding.getRoot().getContext().getString(R.string.inactive));
            binding.editButton.setOnClickListener(view -> listener.onEditDepartment(department));
            binding.deleteButton.setOnClickListener(view -> listener.onDeleteDepartment(department));
            binding.getRoot().setOnClickListener(view -> listener.onEditDepartment(department));
        }
    }

    public interface OnDepartmentActionListener {
        void onEditDepartment(DepartmentModel department);

        void onDeleteDepartment(DepartmentModel department);
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<DepartmentModel> oldItems;
        private final List<DepartmentModel> newItems;

        private DiffCallback(List<DepartmentModel> oldItems, List<DepartmentModel> newItems) {
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
            return oldItems.get(oldItemPosition).getDepartmentId()
                    .equals(newItems.get(newItemPosition).getDepartmentId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DepartmentModel oldItem = oldItems.get(oldItemPosition);
            DepartmentModel newItem = newItems.get(newItemPosition);
            return oldItem.getDepartmentName().equals(newItem.getDepartmentName())
                    && oldItem.isActive() == newItem.isActive();
        }
    }
}
