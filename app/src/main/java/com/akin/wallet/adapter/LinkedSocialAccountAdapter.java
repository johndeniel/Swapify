package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
public class LinkedSocialAccountAdapter extends RecyclerView.Adapter<LinkedSocialAccountAdapter.AccountViewHolder> implements Filterable {

    public interface OnActionListener {
        void onAction(CredentialItem account, boolean removed);
    }

    private final List<CredentialItem> visibleAccounts;
    private final List<CredentialItem> filterSource;
    private final boolean unlinkMode;
    private final OnActionListener listener;
    /** Pick mode only: false hides the [+] icon, the row itself taps. */
    private boolean pickActionVisible = true;

    public void setPickActionVisible(boolean pickActionVisible) {
        this.pickActionVisible = pickActionVisible;
    }

    public LinkedSocialAccountAdapter(List<CredentialItem> accounts, boolean unlinkMode, OnActionListener listener) {
        this.visibleAccounts = accounts;
        this.filterSource = new ArrayList<>(accounts);
        this.unlinkMode = unlinkMode;
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
        CredentialItem account = visibleAccounts.get(position);
        holder.name.setText(account.getPlatform());
        holder.username.setText(account.getUsername());
        PlatformIcons.bindIcon(holder.icon, account.getPlatform(), account.getIconRes());

        if (unlinkMode) {
            holder.action.setImageResource(R.drawable.ic_remove_circle);
            holder.action.setOnClickListener(v -> {
                int clicked = holder.getBindingAdapterPosition();
                if (clicked < 0 || clicked >= visibleAccounts.size()) {
                    return;
                }
                CredentialItem removed = visibleAccounts.get(clicked);
                visibleAccounts.remove(clicked);
                filterSource.remove(removed);
                notifyItemRemoved(clicked);
                notifyItemRangeChanged(clicked, visibleAccounts.size() - clicked);
                listener.onAction(removed, true);
            });
        } else {
            // Pick mode (link search): the row itself links, like the
            // platform picker rows. The [+] icon stays hidden; row tap picks.
            if (pickActionVisible) {
                holder.action.setVisibility(View.VISIBLE);
                holder.action.setImageResource(R.drawable.ic_add_circle);
                holder.action.setOnClickListener(v -> listener.onAction(account, false));
            } else {
                holder.action.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> listener.onAction(account, false));
        }
    }

    @Override
    public int getItemCount() {
        return visibleAccounts.size();
    }

    /**
     * Keeps the search source in sync when the caller appends to its own list
     * externally (link picker result); identity match, same references.
     */
    public void onExternalAdd(CredentialItem account) {
        if (account != null && !filterSource.contains(account)) {
            filterSource.add(account);
        }
    }

    /**
     * Replaces the whole set (rotation restore): the caller's list is mutated
     * in place alongside the search source, so save still reads one list.
     */
    public void onExternalRestore(@NonNull List<CredentialItem> restored) {
        DiffUtil.DiffResult diff = accountDiff(new ArrayList<>(restored));
        visibleAccounts.clear();
        visibleAccounts.addAll(restored);
        filterSource.clear();
        filterSource.addAll(restored);
        diff.dispatchUpdatesTo(this);
    }

    private DiffUtil.DiffResult accountDiff(List<CredentialItem> next) {
        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return visibleAccounts.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return visibleAccounts.get(oldPos).getId() == next.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return visibleAccounts.get(oldPos).equals(next.get(newPos));
            }
        });
    }

    @Override
    public Filter getFilter() {
        return accountFilter;
    }

    private final Filter accountFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CredentialItem> filteredAccounts = filterAccounts(constraint);
            FilterResults results = new FilterResults();
            results.values = filteredAccounts;
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Object rawValues = results != null ? results.values : null;
            List<CredentialItem> nextAccounts =
                    rawValues instanceof List ? (List<CredentialItem>) rawValues : new ArrayList<>();
            DiffUtil.DiffResult diff = accountDiff(nextAccounts);
            visibleAccounts.clear();
            visibleAccounts.addAll(nextAccounts);
            diff.dispatchUpdatesTo(LinkedSocialAccountAdapter.this);
        }
    };

    private List<CredentialItem> filterAccounts(CharSequence constraint) {
        List<CredentialItem> filteredAccounts = new ArrayList<>();
        if (constraint == null || constraint.length() == 0) {
            filteredAccounts.addAll(filterSource);
            return filteredAccounts;
        }
        String filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim();
        for (CredentialItem account : filterSource) {
            if (matchesAccount(account, filterPattern)) {
                filteredAccounts.add(account);
            }
        }
        return filteredAccounts;
    }

    private static boolean matchesAccount(CredentialItem account, String filterPattern) {
        String platform = account.getPlatform() != null
                ? account.getPlatform().toLowerCase(Locale.ROOT) : "";
        String username = account.getUsername() != null
                ? account.getUsername().toLowerCase(Locale.ROOT) : "";
        return platform.contains(filterPattern) || username.contains(filterPattern);
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
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
