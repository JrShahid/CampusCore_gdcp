package com.example.campuscore.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.campuscore.R;
import com.example.campuscore.databinding.ItemCampusCoreUpdateBinding;
import com.example.campuscore.models.FeedItemModel;

import java.util.List;

public class CampusCoreUpdatesAdapter extends RecyclerView.Adapter<CampusCoreUpdatesAdapter.UpdateViewHolder> {
    private final List<FeedItemModel> items;
    private final OnUpdateClickListener listener;

    public CampusCoreUpdatesAdapter(List<FeedItemModel> items, OnUpdateClickListener listener) {
        this.items = items;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public UpdateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCampusCoreUpdateBinding binding = ItemCampusCoreUpdateBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new UpdateViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull UpdateViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).stableId().hashCode();
    }

    public void submitList(List<FeedItemModel> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(items, newItems));
        items.clear();
        items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    static class UpdateViewHolder extends RecyclerView.ViewHolder {
        private final ItemCampusCoreUpdateBinding binding;
        private final OnUpdateClickListener listener;

        UpdateViewHolder(ItemCampusCoreUpdateBinding binding, OnUpdateClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(FeedItemModel item) {
            binding.titleText.setText(item.getTitle());
            binding.descriptionText.setText(item.getDescription());
            binding.sourceText.setText(item.getSourceName());
            binding.categoryText.setText(item.getCategory());
            binding.dateText.setText(item.getPublishedDate().isEmpty()
                    ? binding.getRoot().getContext().getString(R.string.update_recent)
                    : item.getPublishedDate());

            if (item.getImageUrl().isEmpty()) {
                binding.thumbnailImage.setImageResource(R.drawable.ic_feature);
                binding.thumbnailImage.setBackgroundResource(R.drawable.bg_accent_soft);
            } else {
                Glide.with(binding.thumbnailImage)
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_feature)
                        .error(R.drawable.ic_feature)
                        .centerCrop()
                        .into(binding.thumbnailImage);
            }

            binding.getRoot().setOnClickListener(v -> listener.onOpenUpdate(item));
            binding.openButton.setOnClickListener(v -> listener.onOpenUpdate(item));
        }
    }

    public interface OnUpdateClickListener {
        void onOpenUpdate(FeedItemModel item);
    }

    private static final class DiffCallback extends DiffUtil.Callback {
        private final List<FeedItemModel> oldItems;
        private final List<FeedItemModel> newItems;

        private DiffCallback(List<FeedItemModel> oldItems, List<FeedItemModel> newItems) {
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
            return oldItems.get(oldItemPosition).stableId()
                    .equals(newItems.get(newItemPosition).stableId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            FeedItemModel oldItem = oldItems.get(oldItemPosition);
            FeedItemModel newItem = newItems.get(newItemPosition);
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getDescription().equals(newItem.getDescription())
                    && oldItem.getImageUrl().equals(newItem.getImageUrl())
                    && oldItem.getSourceName().equals(newItem.getSourceName())
                    && oldItem.getCategory().equals(newItem.getCategory())
                    && oldItem.getPublishedDate().equals(newItem.getPublishedDate());
        }
    }
}
