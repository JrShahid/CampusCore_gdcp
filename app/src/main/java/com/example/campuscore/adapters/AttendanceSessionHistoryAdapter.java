package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemAttendanceHistoryBinding;
import com.example.campuscore.models.AttendanceSessionModel;

import java.util.List;

public class AttendanceSessionHistoryAdapter extends RecyclerView.Adapter<AttendanceSessionHistoryAdapter.SessionViewHolder> {
    private final List<AttendanceSessionModel> items;

    public AttendanceSessionHistoryAdapter(List<AttendanceSessionModel> items) {
        this.items = items;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SessionViewHolder(ItemAttendanceHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override public int getItemCount() { return items.size(); }
    @Override public long getItemId(int position) { return items.get(position).getSessionId().hashCode(); }

    public void submitList(List<AttendanceSessionModel> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diff.dispatchUpdatesTo(this);
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        private final ItemAttendanceHistoryBinding binding;

        SessionViewHolder(ItemAttendanceHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AttendanceSessionModel item) {
            binding.titleText.setText(item.displaySubject());
            binding.subtitleText.setText(item.getDate() + " - " + item.getDepartmentId()
                    + " Sem " + item.getSemester() + " Section " + item.getSection()
                    + " - " + item.getPresentCount() + "/" + item.getTotalStudents() + " present");
            binding.statusText.setText(item.isEditableNow() ? R.string.editable : R.string.finalized);
            binding.statusText.setBackgroundResource(item.isEditableNow() ? R.drawable.bg_success_chip : R.drawable.bg_danger_chip);
            binding.statusText.setTextColor(binding.getRoot().getContext().getColor(
                    item.isEditableNow() ? R.color.success_green : R.color.danger_red));
        }
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<AttendanceSessionModel> oldItems;
        private final List<AttendanceSessionModel> newItems;

        private DiffCallback(List<AttendanceSessionModel> oldItems, List<AttendanceSessionModel> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override public int getOldListSize() { return oldItems.size(); }
        @Override public int getNewListSize() { return newItems.size(); }
        @Override public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldItems.get(oldPos).getSessionId().equals(newItems.get(newPos).getSessionId());
        }
        @Override public boolean areContentsTheSame(int oldPos, int newPos) {
            AttendanceSessionModel oldItem = oldItems.get(oldPos);
            AttendanceSessionModel newItem = newItems.get(newPos);
            return oldItem.getPresentCount() == newItem.getPresentCount()
                    && oldItem.getAbsentCount() == newItem.getAbsentCount()
                    && oldItem.isEditableNow() == newItem.isEditableNow();
        }
    }
}
