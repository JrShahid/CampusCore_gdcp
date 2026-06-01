package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemNoteBinding;
import com.example.campuscore.models.NotesModel;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private final List<NotesModel> items;
    private final OnNoteActionListener listener;
    private final boolean showDeleteAction;

    public NotesAdapter(List<NotesModel> items, OnNoteActionListener listener) {
        this(items, listener, false);
    }

    public NotesAdapter(List<NotesModel> items, OnNoteActionListener listener, boolean showDeleteAction) {
        this.items = items;
        this.listener = listener;
        this.showDeleteAction = showDeleteAction;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding, listener, showDeleteAction);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getNoteId().hashCode();
    }

    public void submitList(List<NotesModel> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        private final ItemNoteBinding binding;
        private final OnNoteActionListener listener;
        private final boolean showDeleteAction;

        NoteViewHolder(ItemNoteBinding binding, OnNoteActionListener listener, boolean showDeleteAction) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            this.showDeleteAction = showDeleteAction;
        }

        void bind(NotesModel note) {
            binding.titleText.setText(note.getTitle());
            binding.subjectText.setText(binding.getRoot().getContext().getString(
                    R.string.subject_name_format,
                    note.getSubjectCode(),
                    note.getSubjectName()
            ));
            binding.dateText.setText(binding.getRoot().getContext().getString(
                    R.string.uploaded_on_format,
                    formatTimestamp(note.getTimestamp())
            ));
            binding.openButton.setOnClickListener(v -> listener.onOpenNote(note));
            binding.deleteButton.setVisibility(showDeleteAction ? View.VISIBLE : View.GONE);
            binding.deleteButton.setOnClickListener(v -> listener.onDeleteNote(note));
        }

        private String formatTimestamp(Timestamp timestamp) {
            if (timestamp == null) {
                return "Just now";
            }
            Date date = timestamp.toDate();
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
        }
    }

    public interface OnNoteActionListener {
        void onOpenNote(NotesModel note);

        default void onDeleteNote(NotesModel note) {
        }
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<NotesModel> oldItems;
        private final List<NotesModel> newItems;

        private DiffCallback(List<NotesModel> oldItems, List<NotesModel> newItems) {
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
            return oldItems.get(oldItemPosition).getNoteId()
                    .equals(newItems.get(newItemPosition).getNoteId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            NotesModel oldItem = oldItems.get(oldItemPosition);
            NotesModel newItem = newItems.get(newItemPosition);
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getSubjectCode().equals(newItem.getSubjectCode())
                    && oldItem.getSubjectName().equals(newItem.getSubjectName())
                    && oldItem.getPdfUrl().equals(newItem.getPdfUrl())
                    && oldItem.getFileName().equals(newItem.getFileName());
        }
    }
}
