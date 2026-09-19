package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import com.akin.wallet.R;
import com.akin.wallet.model.PlatformIcons;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
public class SocialPlatformAdapter extends RecyclerView.Adapter<SocialPlatformAdapter.PlatformViewHolder> implements Filterable {

    public interface OnPlatformSelectedListener {
        void onPlatformSelected(int iconRes, String name, String url);
    }

    private final List<PlatformIcons.Option> visiblePlatforms;
    private final List<PlatformIcons.Option> allPlatforms;
    private final OnPlatformSelectedListener listener;

    public SocialPlatformAdapter(List<PlatformIcons.Option> platforms, OnPlatformSelectedListener listener) {
        // Owned copy: filtering mutates the displayed list, which must never
        // leak back into the caller's catalog.
        this.visiblePlatforms = platforms != null ? new ArrayList<>(platforms) : new ArrayList<>();
        this.allPlatforms = new ArrayList<>(this.visiblePlatforms);
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
        PlatformIcons.Option platform = visiblePlatforms.get(position);
        holder.icon.setImageResource(platform.getIconRes());
        holder.name.setText(platform.getName());
        holder.url.setText(platform.getUrl());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlatformSelected(platform.getIconRes(), platform.getName(), platform.getUrl());
            }
        });
    }

    @Override
    public int getItemCount() {
        return visiblePlatforms.size();
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
                filtered.addAll(allPlatforms);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim();
                for (PlatformIcons.Option platform : allPlatforms) {
                    if (platform.getName().toLowerCase(Locale.ROOT).contains(filterPattern)
                            || platform.getUrl().toLowerCase(Locale.ROOT).contains(filterPattern)) {
                        filtered.add(platform);
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
            List<PlatformIcons.Option> filtered = (List<PlatformIcons.Option>) results.values;
            final List<PlatformIcons.Option> next =
                    filtered != null ? filtered : new ArrayList<>();
            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return visiblePlatforms.size();
                }

                @Override
                public int getNewListSize() {
                    return next.size();
                }

                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    return visiblePlatforms.get(oldPos).getName()
                            .equals(next.get(newPos).getName());
                }

                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    PlatformIcons.Option oldOption = visiblePlatforms.get(oldPos);
                    PlatformIcons.Option newOption = next.get(newPos);
                    return oldOption.getIconRes() == newOption.getIconRes()
                            && oldOption.getName().equals(newOption.getName())
                            && oldOption.getUrl().equals(newOption.getUrl());
                }
            });
            visiblePlatforms.clear();
            visiblePlatforms.addAll(next);
            diff.dispatchUpdatesTo(SocialPlatformAdapter.this);
        }
    };

    public static class PlatformViewHolder extends RecyclerView.ViewHolder {
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
