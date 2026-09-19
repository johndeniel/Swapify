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
            FilterResults results = new FilterResults();
            results.values = filterPlatforms(constraint);
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Object rawValues = results != null ? results.values : null;
            final List<PlatformIcons.Option> nextOptions =
                    rawValues instanceof List ? (List<PlatformIcons.Option>) rawValues : new ArrayList<>();
            DiffUtil.DiffResult diff = platformDiff(nextOptions);
            visiblePlatforms.clear();
            visiblePlatforms.addAll(nextOptions);
            diff.dispatchUpdatesTo(SocialPlatformAdapter.this);
        }
    };

    private List<PlatformIcons.Option> filterPlatforms(CharSequence constraint) {
        List<PlatformIcons.Option> filteredOptions = new ArrayList<>();
        if (constraint == null || constraint.length() == 0) {
            filteredOptions.addAll(allPlatforms);
            return filteredOptions;
        }
        String filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim();
        for (PlatformIcons.Option platform : allPlatforms) {
            if (matchesPlatform(platform, filterPattern)) {
                filteredOptions.add(platform);
            }
        }
        return filteredOptions;
    }

    private static boolean matchesPlatform(PlatformIcons.Option platform, String filterPattern) {
        return platform.getName().toLowerCase(Locale.ROOT).contains(filterPattern)
                || platform.getUrl().toLowerCase(Locale.ROOT).contains(filterPattern);
    }

    private DiffUtil.DiffResult platformDiff(final List<PlatformIcons.Option> nextOptions) {
        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return visiblePlatforms.size();
                }

                @Override
                public int getNewListSize() {
                    return nextOptions.size();
                }

                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    return visiblePlatforms.get(oldPos).getName()
                            .equals(nextOptions.get(newPos).getName());
                }

                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    PlatformIcons.Option oldOption = visiblePlatforms.get(oldPos);
                    PlatformIcons.Option newOption = nextOptions.get(newPos);
                    return oldOption.getIconRes() == newOption.getIconRes()
                            && oldOption.getName().equals(newOption.getName())
                            && oldOption.getUrl().equals(newOption.getUrl());
                }
            });
    }

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
