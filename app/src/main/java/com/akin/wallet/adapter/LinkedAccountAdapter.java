package com.akin.wallet.adapter;

import android.content.res.ColorStateList;
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

public class LinkedAccountAdapter extends RecyclerView.Adapter<LinkedAccountAdapter.AssocViewHolder> {

    public interface OnActionListener {
        void onAction(CredentialItem item, boolean isRemove);
    }

    private final List<CredentialItem> items;
    private final boolean showRemove;
    private final OnActionListener listener;

    public LinkedAccountAdapter(List<CredentialItem> items, boolean showRemove, OnActionListener listener) {
        this.items = items;
        this.showRemove = showRemove;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AssocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_linked_account, parent, false);
        return new AssocViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssocViewHolder holder, int position) {
        CredentialItem item = items.get(position);
        holder.name.setText(item.getPlatform());
        holder.email.setText(item.getUsername());
        holder.icon.setImageResource(item.getIconRes());

        if (showRemove) {
            holder.action.setImageResource(R.drawable.ic_remove_circle);
            holder.action.setOnClickListener(v -> {
                listener.onAction(item, true);
                items.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, items.size());
            });
        } else {
            holder.action.setImageResource(R.drawable.ic_add_circle);
            holder.action.setOnClickListener(v -> listener.onAction(item, false));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AssocViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        TextView email;
        ImageView action;

        AssocViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.assoc_icon);
            name = itemView.findViewById(R.id.assoc_name);
            email = itemView.findViewById(R.id.assoc_email);
            action = itemView.findViewById(R.id.btn_action);
        }
    }
}
