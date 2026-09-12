package com.akin.wallet.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import com.akin.wallet.R;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.CredentialItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SocialLoginAdapter extends RecyclerView.Adapter<SocialLoginAdapter.LoginViewHolder> implements Filterable {

    public interface OnCredentialActionListener {
        void onEdit(CredentialItem item);
        void onDelete(CredentialItem item);
    }

    private final List<CredentialItem> loginItems;
    private final List<CredentialItem> loginItemsFull;
    private final AppDatabaseHelper dbHelper;
    private OnCredentialActionListener listener;
    private int expandedPosition = -1;

    public SocialLoginAdapter(List<CredentialItem> loginItems, AppDatabaseHelper dbHelper) {
        this.loginItems = loginItems;
        this.loginItemsFull = new ArrayList<>(loginItems);
        this.dbHelper = dbHelper;
    }

    public void setOnCredentialActionListener(OnCredentialActionListener listener) {
        this.listener = listener;
    }

    public void updateData(List<CredentialItem> newItems) {
        loginItems.clear();
        loginItems.addAll(newItems);
        loginItemsFull.clear();
        loginItemsFull.addAll(newItems);
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    private void copyToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, label + " copied", Toast.LENGTH_SHORT).show();
    }

    @NonNull
    @Override
    public LoginViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_social_login, parent, false);
        return new LoginViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoginViewHolder holder, int position) {
        CredentialItem item = loginItems.get(position);
        holder.serviceName.setText(item.getPlatform());
        holder.username.setText(item.getUsername());
        holder.iconService.setImageResource(item.getIconRes());

        boolean isExpanded = position == expandedPosition;
        holder.expandedSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.btnMore.animate().rotation(isExpanded ? 180f : 0f).setDuration(200).start();

        if (isExpanded) {
            holder.detailUsername.setText(item.getUsername());

            String password = item.getPassword();
            if (password != null && !password.isEmpty()) {
                holder.detailPassword.setText(password);
                holder.detailPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                holder.btnTogglePassword.setVisibility(View.VISIBLE);
                holder.btnTogglePassword.setTag(false);
                holder.btnCopyPassword.setVisibility(View.VISIBLE);
            } else {
                holder.detailPassword.setText("Not set");
                holder.detailPassword.setTransformationMethod(null);
                holder.btnTogglePassword.setVisibility(View.GONE);
                holder.btnCopyPassword.setVisibility(View.GONE);
            }

            holder.btnTogglePassword.setOnClickListener(v -> {
                boolean isShowing = (boolean) v.getTag();
                if (isShowing) {
                    holder.detailPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    v.setTag(false);
                } else {
                    holder.detailPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    v.setTag(true);
                }
            });

            holder.btnCopyUsername.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "Username", item.getUsername()));

            holder.btnCopyPassword.setOnClickListener(v -> {
                String pw = item.getPassword();
                if (pw != null && !pw.isEmpty()) {
                    copyToClipboard(v.getContext(), "Password", pw);
                }
            });

            String pin = item.getPin();
            if (pin != null && !pin.isEmpty()) {
                holder.detailPin.setText(pin);
                holder.detailPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
                holder.btnTogglePin.setVisibility(View.VISIBLE);
                holder.btnTogglePin.setTag(false);
                holder.btnCopyPin.setVisibility(View.VISIBLE);
            } else {
                holder.detailPin.setText("Not set");
                holder.detailPin.setTransformationMethod(null);
                holder.btnTogglePin.setVisibility(View.GONE);
                holder.btnCopyPin.setVisibility(View.GONE);
            }

            holder.btnTogglePin.setOnClickListener(v -> {
                boolean isShowing = (boolean) v.getTag();
                if (isShowing) {
                    holder.detailPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    v.setTag(false);
                } else {
                    holder.detailPin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    v.setTag(true);
                }
            });

            holder.btnCopyPin.setOnClickListener(v -> {
                String p = item.getPin();
                if (p != null && !p.isEmpty()) {
                    copyToClipboard(v.getContext(), "PIN", p);
                }
            });

            List<Integer> associatedIds = dbHelper.getAssociations(item.getId());
            if (associatedIds != null && !associatedIds.isEmpty()) {
                holder.associatedSection.setVisibility(View.VISIBLE);
                List<CredentialItem> associatedItems = new ArrayList<>();
                List<CredentialItem> allLogins = dbHelper.getAllLogins();
                for (CredentialItem login : allLogins) {
                    for (int assocId : associatedIds) {
                        if (login.getId() == assocId) {
                            associatedItems.add(login);
                            break;
                        }
                    }
                }
                AssociatedAccountAdapter assocAdapter = new AssociatedAccountAdapter(associatedItems);
                holder.recyclerAssociated.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
                holder.recyclerAssociated.setAdapter(assocAdapter);
            } else {
                holder.associatedSection.setVisibility(View.GONE);
            }

            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(item);
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(item);
            });
        }

        View.OnClickListener toggleListener = v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            if (expandedPosition == adapterPosition) {
                expandedPosition = -1;
            } else {
                int oldExpanded = expandedPosition;
                expandedPosition = adapterPosition;
                if (oldExpanded != -1) {
                    notifyItemChanged(oldExpanded);
                }
            }
            notifyItemChanged(adapterPosition);
        };

        holder.headerRow.setOnClickListener(toggleListener);
        holder.btnMore.setOnClickListener(toggleListener);
    }

    @Override
    public int getItemCount() {
        return loginItems.size();
    }

    @Override
    public Filter getFilter() {
        return loginFilter;
    }

    private final Filter loginFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<CredentialItem> filtered = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filtered.addAll(loginItemsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (CredentialItem item : loginItemsFull) {
                    if (item.getPlatform().toLowerCase().contains(filterPattern)
                            || item.getUsername().toLowerCase().contains(filterPattern)) {
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
            loginItems.clear();
            loginItems.addAll((List<CredentialItem>) results.values);
            notifyDataSetChanged();
        }
    };

    static class LoginViewHolder extends RecyclerView.ViewHolder {
        ImageView iconService;
        TextView serviceName;
        TextView username;
        LinearLayout headerRow;
        ImageView btnMore;
        LinearLayout expandedSection;
        TextView detailUsername;
        TextView detailPassword;
        TextView detailPin;
        ImageView btnTogglePassword;
        ImageView btnTogglePin;
        ImageView btnCopyUsername;
        ImageView btnCopyPassword;
        ImageView btnCopyPin;
        LinearLayout associatedSection;
        RecyclerView recyclerAssociated;
        LinearLayout btnEdit;
        LinearLayout btnDelete;

        LoginViewHolder(@NonNull View itemView) {
            super(itemView);
            iconService = itemView.findViewById(R.id.icon_service);
            serviceName = itemView.findViewById(R.id.service_name);
            username = itemView.findViewById(R.id.username);
            headerRow = itemView.findViewById(R.id.header_row);
            btnMore = itemView.findViewById(R.id.btn_more);
            expandedSection = itemView.findViewById(R.id.expanded_section);
            detailUsername = itemView.findViewById(R.id.detail_username);
            detailPassword = itemView.findViewById(R.id.detail_password);
            detailPin = itemView.findViewById(R.id.detail_pin);
            btnTogglePassword = itemView.findViewById(R.id.btn_toggle_password);
            btnTogglePin = itemView.findViewById(R.id.btn_toggle_pin);
            btnCopyUsername = itemView.findViewById(R.id.btn_copy_username);
            btnCopyPassword = itemView.findViewById(R.id.btn_copy_password);
            btnCopyPin = itemView.findViewById(R.id.btn_copy_pin);
            associatedSection = itemView.findViewById(R.id.associated_section);
            recyclerAssociated = itemView.findViewById(R.id.recycler_associated);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
