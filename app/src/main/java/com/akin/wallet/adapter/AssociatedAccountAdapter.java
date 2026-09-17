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
import com.akin.wallet.model.PlatformIcons;

import java.util.List;

public class AssociatedAccountAdapter extends RecyclerView.Adapter<AssociatedAccountAdapter.AssocViewHolder> {

    private final List<CredentialItem> items;

    public AssociatedAccountAdapter(List<CredentialItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public AssocViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_linked_social_account, parent, false);
        return new AssocViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AssocViewHolder holder, int position) {
        CredentialItem item = items.get(position);
        holder.name.setText(item.getPlatform());
        holder.email.setText(item.getUsername());
        PlatformIcons.bindIcon(holder.icon, item.getPlatform(), item.getIconRes());
        holder.action.setVisibility(View.VISIBLE);
        holder.action.setImageResource(R.drawable.ic_link);
        holder.action.setImageTintList(ColorStateList.valueOf(
                holder.itemView.getContext().getResources().getColor(R.color.text_primary)));
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
