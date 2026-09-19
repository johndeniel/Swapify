package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.PlatformIcons;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard Social Account — renders only the user's created social
 * accounts (platform icon + username). Tapping a row opens the account.
 */
public class DashboardSocialAccountAdapter extends RecyclerView.Adapter<DashboardSocialAccountAdapter.AccountViewHolder> {

    public interface OnAccountClickListener {
        void onAccountClick(CredentialItem item);
    }

    private final List<CredentialItem> items = new ArrayList<>();
    private final OnAccountClickListener listener;

    public DashboardSocialAccountAdapter(OnAccountClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<CredentialItem> newItems) {
        List<CredentialItem> next =
                newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).getId() == next.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                if (!items.get(oldPos).equals(next.get(newPos))) {
                    return false;
                }
                // Divider visibility is positional (hidden on the last row).
                return (oldPos == items.size() - 1) == (newPos == next.size() - 1);
            }
        });
        items.clear();
        items.addAll(next);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_social_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        CredentialItem item = items.get(position);
        String platform = item.getPlatform() != null && !item.getPlatform().trim().isEmpty()
                ? item.getPlatform().trim() : "Social Login";
        String username = item.getUsername() != null ? item.getUsername().trim() : "";
        holder.title.setText(platform);
        holder.sub.setText(username.isEmpty() ? "Social Login" : username);
        PlatformIcons.bindIcon(holder.icon, item.getPlatform(), item.getIconRes());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccountClick(item);
            }
        });
        holder.divider.setVisibility(
                position == getItemCount() - 1 ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView sub;
        View divider;

        AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.social_icon);
            title = itemView.findViewById(R.id.social_title);
            sub = itemView.findViewById(R.id.social_sub);
            divider = itemView.findViewById(R.id.social_divider);
        }
    }
}
