package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import com.akin.wallet.R;
import com.akin.wallet.model.PlatformOption;
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

public class PlatformSelectionAdapter extends RecyclerView.Adapter<PlatformSelectionAdapter.PlatformViewHolder> implements Filterable {

    public interface OnPlatformSelectedListener {
        void onPlatformSelected(int iconRes, String name, String url);
    }

    private final List<PlatformOption> platforms;
    private final List<PlatformOption> platformsFull;
    private final OnPlatformSelectedListener listener;

    public PlatformSelectionAdapter(List<PlatformOption> platforms, OnPlatformSelectedListener listener) {
        this.platforms = platforms;
        this.platformsFull = new ArrayList<>(platforms);
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
        PlatformOption item = platforms.get(position);
        holder.icon.setImageResource(item.getIconRes());
        holder.name.setText(item.getName());
        holder.url.setText(item.getUrl());
        holder.itemView.setOnClickListener(v ->
                listener.onPlatformSelected(item.getIconRes(), item.getName(), item.getUrl()));
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
            List<PlatformOption> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(platformsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (PlatformOption item : platformsFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)
                            || item.getUrl().toLowerCase().contains(filterPattern)) {
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
            platforms.addAll((List<PlatformOption>) results.values);
            notifyDataSetChanged();
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
