package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.CredentialItem;

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
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
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
        try {
            if (item.getIconRes() != 0) {
                holder.icon.setImageResource(item.getIconRes());
            } else {
                holder.icon.setImageResource(R.drawable.ic_social);
            }
        } catch (Exception e) {
            holder.icon.setImageResource(R.drawable.ic_social);
        }
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
