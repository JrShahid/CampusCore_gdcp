package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemPendingTeacherBinding;
import com.example.campuscore.models.PendingTeacherModel;

import java.util.List;

public class PendingTeachersAdapter extends RecyclerView.Adapter<PendingTeachersAdapter.PendingTeacherViewHolder> {
    private final List<PendingTeacherModel> items;
    private final OnTeacherActionListener listener;

    public PendingTeachersAdapter(List<PendingTeacherModel> items, OnTeacherActionListener listener) {
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public PendingTeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PendingTeacherViewHolder(ItemPendingTeacherBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingTeacherViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getEmployeeId().hashCode();
    }

    public void submitList(List<PendingTeacherModel> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    static class PendingTeacherViewHolder extends RecyclerView.ViewHolder {
        private final ItemPendingTeacherBinding binding;
        private final OnTeacherActionListener listener;

        PendingTeacherViewHolder(ItemPendingTeacherBinding binding, OnTeacherActionListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(PendingTeacherModel item) {
            binding.nameText.setText(item.getName());
            binding.detailText.setText(item.getEmployeeId() + " - " + item.getPrimaryDepartmentId() + " - " + item.getDesignation());
            binding.statusText.setText(item.isActive()
                    ? binding.getRoot().getContext().getString(R.string.active)
                    : binding.getRoot().getContext().getString(R.string.inactive));
            binding.editButton.setOnClickListener(view -> listener.onEditTeacher(item));
            binding.deleteButton.setOnClickListener(view -> listener.onDeleteTeacher(item));
            binding.getRoot().setOnClickListener(view -> listener.onEditTeacher(item));
        }
    }

    public interface OnTeacherActionListener {
        void onEditTeacher(PendingTeacherModel teacher);

        void onDeleteTeacher(PendingTeacherModel teacher);
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<PendingTeacherModel> oldItems;
        private final List<PendingTeacherModel> newItems;

        private DiffCallback(List<PendingTeacherModel> oldItems, List<PendingTeacherModel> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override public int getOldListSize() { return oldItems.size(); }
        @Override public int getNewListSize() { return newItems.size(); }
        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldItems.get(oldPos).getEmployeeId().equals(newItems.get(newPos).getEmployeeId());
        }
        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            PendingTeacherModel oldItem = oldItems.get(oldPos);
            PendingTeacherModel newItem = newItems.get(newPos);
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getPrimaryDepartmentId().equals(newItem.getPrimaryDepartmentId())
                    && oldItem.getDesignation().equals(newItem.getDesignation())
                    && oldItem.isActive() == newItem.isActive();
        }
    }
}
