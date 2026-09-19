package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import com.akin.wallet.R;
import com.akin.wallet.model.PlatformIcons;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Filter;
import android.widget.Filterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Platform picker rows for the Social Account screen (icon + name + URL).
 * Tapping a row selects the platform.
 */

public class PlatformSelectionAdapter extends RecyclerView.Adapter<PlatformSelectionAdapter.PlatformViewHolder> implements Filterable {

    public interface OnPlatformSelectedListener {
        void onPlatformSelected(int iconRes, String name, String url);
    }

    private final List<PlatformIcons.Option> platforms;
    private final List<PlatformIcons.Option> platformsFull;
    private final OnPlatformSelectedListener listener;
    private OnCountChangedListener countListener;

    public void setOnCountChangedListener(OnCountChangedListener countListener) {
        this.countListener = countListener;
    }

    public PlatformSelectionAdapter(List<PlatformIcons.Option> platforms, OnPlatformSelectedListener listener) {
        // Owned copy: filtering mutates the displayed list, which must never
        // leak back into the caller's catalog.
        this.platforms = platforms != null ? new ArrayList<>(platforms) : new ArrayList<>();
        this.platformsFull = new ArrayList<>(this.platforms);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlatformViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_platform_option, parent, false);
        return new PlatformViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlatformViewHolder holder, int position) {
        PlatformIcons.Option item = platforms.get(position);
        holder.icon.setImageResource(item.getIconRes());
        holder.name.setText(item.getName());
        holder.url.setText(item.getUrl());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlatformSelected(item.getIconRes(), item.getName(), item.getUrl());
            }
        });
    }

    @Override
    public int getItemCount() {
        return platforms.size();
    }

    @Override
    public Filter getFilter() {
        return platformFilter;
    }

    private final Filter platformFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<PlatformIcons.Option> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(platformsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim();
                for (PlatformIcons.Option item : platformsFull) {
                    if (item.getName().toLowerCase(Locale.ROOT).contains(filterPattern)
                            || item.getUrl().toLowerCase(Locale.ROOT).contains(filterPattern)) {
                        filtered.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filtered;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            platforms.clear();
            platforms.addAll((List<PlatformIcons.Option>) results.values);
            notifyDataSetChanged();
            if (countListener != null) {
                countListener.onCountChanged(platforms.size());
            }
        }
    };

    static class PlatformViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView url;

        PlatformViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.platform_icon);
            name = itemView.findViewById(R.id.platform_name);
            url = itemView.findViewById(R.id.platform_url);
        }
    }
}
