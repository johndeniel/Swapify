package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.Filter;
import android.widget.Filterable;

import com.akin.wallet.R;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.PlatformIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Linked Social Account rows for the Social Account screen: the current
 * link set with remove actions, reused in pick mode by the link search.
 * The host owns the displayed list (saves read it directly); the adapter
 * keeps a private copy as the filter source.
 */
public class LinkedAccountAdapter extends RecyclerView.Adapter<LinkedAccountAdapter.AccountViewHolder> implements Filterable {

    public interface OnActionListener {
        void onAction(CredentialItem item, boolean isRemove);
    }

    private final List<CredentialItem> items;
    // Filter source. Displayed items mutate in place so unlinking keeps the
    // host's list in sync for save; both lists hold the same references, so
    // removals apply to each by identity.
    private final List<CredentialItem> itemsFull;
    private final boolean showRemove;
    private final OnActionListener listener;
    /** Pick mode only: false hides the [+] icon, the row itself taps. */
    private boolean showPickAction = true;

    public void setShowPickAction(boolean showPickAction) {
        this.showPickAction = showPickAction;
    }

    public LinkedAccountAdapter(List<CredentialItem> items, boolean showRemove, OnActionListener listener) {
        this.items = items;
        this.itemsFull = new ArrayList<>(items);
        this.showRemove = showRemove;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_linked_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        CredentialItem item = items.get(position);
        holder.name.setText(item.getPlatform());
        holder.username.setText(item.getUsername());
        PlatformIcons.bindIcon(holder.icon, item.getPlatform(), item.getIconRes());

        if (showRemove) {
            holder.action.setImageResource(R.drawable.ic_remove_circle);
            holder.action.setOnClickListener(v -> {
                // Resolve position at click time (bind-time positions go stale
                // after prior removals) and refresh AFTER mutating: listeners
                // read the list, so notifying first leaves the empty state
                // one removal behind — the last unlink never showed it.
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos >= items.size()) {
                    return;
                }
                CredentialItem removed = items.get(pos);
                items.remove(pos);
                itemsFull.remove(removed);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, items.size());
                listener.onAction(removed, true);
            });
        } else {
            // Pick mode (link search): the row itself links, like the
            // platform picker rows. The [+] icon stays hidden; row tap picks.
            if (showPickAction) {
                holder.action.setVisibility(View.VISIBLE);
                holder.action.setImageResource(R.drawable.ic_add_circle);
                holder.action.setOnClickListener(v -> listener.onAction(item, false));
            } else {
                holder.action.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> listener.onAction(item, false));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Keeps the search source in sync when the caller appends to its own list
     * externally (link picker result); identity match, same references.
     */
    public void onExternalAdd(CredentialItem item) {
        if (item != null && !itemsFull.contains(item)) {
            itemsFull.add(item);
        }
    }

    /**
     * Replaces the whole set (rotation restore): the caller's list is mutated
     * in place alongside the search source, so save still reads one list.
     */
    public void onExternalRestore(@NonNull List<CredentialItem> restored) {
        items.clear();
        items.addAll(restored);
        itemsFull.clear();
        itemsFull.addAll(restored);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return accountFilter;
    }

    // Search mirrors the platform picker: case-insensitive contains on
    // platform or username, full list restored on empty query.
    private final Filter accountFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CredentialItem> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(itemsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim();
                for (CredentialItem item : itemsFull) {
                    String platform = item.getPlatform() != null
                            ? item.getPlatform().toLowerCase(Locale.ROOT) : "";
                    String username = item.getUsername() != null
                            ? item.getUsername().toLowerCase(Locale.ROOT) : "";
                    if (platform.contains(filterPattern) || username.contains(filterPattern)) {
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
            items.clear();
            items.addAll((List<CredentialItem>) results.values);
            notifyDataSetChanged();
        }
    };

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView username;
        ImageView action;

        AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.linked_icon);
            name = itemView.findViewById(R.id.linked_name);
            username = itemView.findViewById(R.id.linked_username);
            action = itemView.findViewById(R.id.btn_action);
        }
    }
}
