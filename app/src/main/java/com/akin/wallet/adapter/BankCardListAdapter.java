package com.akin.wallet.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.BankCardItem;

import java.util.ArrayList;
import java.util.List;

public class BankCardListAdapter extends RecyclerView.Adapter<BankCardListAdapter.CardViewHolder> {

    public interface OnCardActionListener {
        void onEdit(BankCardItem item);
        void onDelete(BankCardItem item);
    }

    private final int[] backgrounds = {
            R.drawable.bg_bank_card_blue,
            R.drawable.bg_bank_card_purple,
            R.drawable.bg_bank_card_green,
            R.drawable.bg_bank_card_orange,
            R.drawable.bg_bank_card_slate
    };

    private final List<BankCardItem> items = new ArrayList<>();
    private final OnCardActionListener listener;
    private int expandedPosition = -1;

    public BankCardListAdapter(List<BankCardItem> items, OnCardActionListener listener) {
        if (items != null) {
            this.items.addAll(items);
        }
        this.listener = listener;
    }

    public void updateData(List<BankCardItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        expandedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bank_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        BankCardItem item = items.get(position);
        BankCardDesignAdapter.applyCardOutline(holder.cardRoot);
        int design = item.getDesign();
        if (design < 0 || design >= backgrounds.length) {
            design = 0;
        }
        holder.cardRoot.setBackgroundResource(backgrounds[design]);
        holder.bank.setText(item.getBankName().toUpperCase());
        holder.holder.setText(item.getHolderName().toUpperCase());
        holder.number.setText("•••• •••• •••• " + last4(item.getCardNumber()));
        holder.expiry.setText(formatExpiry(item.getExpiry()));
        BankCardDesignAdapter.applyNetworkLogo(holder.network, item.getCardNetwork());
        holder.type.setText(item.getCardType().toUpperCase());

        boolean isExpanded = position == expandedPosition;
        holder.expandedSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (isExpanded) {
            holder.detailType.setText(item.getCardType());
            holder.detailNetwork.setText(item.getCardNetwork());
            holder.detailBank.setText(item.getBankName());
            holder.detailHolder.setText(item.getHolderName());
            holder.detailNumber.setText("•••• •••• •••• " + last4(item.getCardNumber()));
            holder.detailExpiry.setText(formatExpiry(item.getExpiry()));
            holder.detailCvv.setText("•••");
            holder.detailPin.setText(mask(item.getPin()));

            holder.btnToggleNumber.setTag(false);
            holder.btnToggleNumber.setOnClickListener(v -> {
                boolean isShowing = (boolean) v.getTag();
                if (isShowing) {
                    holder.detailNumber.setText("•••• •••• •••• " + last4(item.getCardNumber()));
                    v.setTag(false);
                } else {
                    holder.detailNumber.setText(grouped(item.getCardNumber()));
                    v.setTag(true);
                }
            });

            holder.btnToggleCvv.setTag(false);
            holder.btnToggleCvv.setOnClickListener(v -> {
                boolean isShowing = (boolean) v.getTag();
                if (isShowing) {
                    holder.detailCvv.setText("•••");
                    v.setTag(false);
                } else {
                    holder.detailCvv.setText(item.getCvv());
                    v.setTag(true);
                }
            });

            holder.btnTogglePin.setTag(false);
            holder.btnTogglePin.setOnClickListener(v -> {
                boolean isShowing = (boolean) v.getTag();
                if (isShowing) {
                    holder.detailPin.setText(mask(item.getPin()));
                    v.setTag(false);
                } else {
                    holder.detailPin.setText(item.getPin());
                    v.setTag(true);
                }
            });

            holder.btnCopyType.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "Card type", item.getCardType()));
            holder.btnCopyNetwork.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "Card network", item.getCardNetwork()));
            holder.btnCopyBank.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "Bank name", item.getBankName()));
            holder.btnCopyHolder.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "Cardholder name", item.getHolderName()));
            holder.btnCopyNumber.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "Card number", digitsOnly(item.getCardNumber())));
            holder.btnCopyExpiry.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "Valid thru", item.getExpiry()));
            holder.btnCopyCvv.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "CVV", item.getCvv()));
            holder.btnCopyPin.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), "PIN", item.getPin()));

            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(item);
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(item);
            });
        }

        holder.cardRoot.setOnClickListener(v -> {
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
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String last4(String number) {
        if (number == null) {
            return "••••";
        }
        String digits = number.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "••••";
        }
        return digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
    }

    private void copyToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text != null ? text : "");
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, label + " copied", Toast.LENGTH_SHORT).show();
    }

    private static String digitsOnly(String value) {
        return value != null ? value.replaceAll("\\D", "") : "";
    }

    private static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "Not set";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            sb.append('•');
        }
        return sb.toString();
    }

    private static String grouped(String number) {
        if (number == null) {
            return "";
        }
        String digits = number.replaceAll("\\D", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(' ');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private static String formatExpiry(String expiry) {
        if (expiry == null) {
            return "MM/YY";
        }
        String digits = expiry.replaceAll("\\D", "");
        if (digits.length() == 4) {
            return digits.substring(0, 2) + "/" + digits.substring(2);
        }
        return digits.isEmpty() ? "MM/YY" : digits;
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView bank;
        ImageView network;
        TextView type;
        TextView number;
        TextView holder;
        TextView expiry;
        LinearLayout expandedSection;
        TextView detailType;
        TextView detailNetwork;
        TextView detailBank;
        TextView detailHolder;
        TextView detailNumber;
        TextView detailExpiry;
        TextView detailCvv;
        TextView detailPin;
        ImageView btnToggleNumber;
        ImageView btnToggleCvv;
        ImageView btnTogglePin;
        ImageView btnCopyType;
        ImageView btnCopyNetwork;
        ImageView btnCopyBank;
        ImageView btnCopyHolder;
        ImageView btnCopyNumber;
        ImageView btnCopyExpiry;
        ImageView btnCopyCvv;
        ImageView btnCopyPin;
        LinearLayout btnEdit;
        LinearLayout btnDelete;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            bank = itemView.findViewById(R.id.preview_bank);
            network = itemView.findViewById(R.id.preview_network);
            type = itemView.findViewById(R.id.preview_type);
            number = itemView.findViewById(R.id.preview_number);
            holder = itemView.findViewById(R.id.preview_holder);
            expiry = itemView.findViewById(R.id.preview_expiry);
            expandedSection = itemView.findViewById(R.id.expanded_section);
            detailType = itemView.findViewById(R.id.detail_type);
            detailNetwork = itemView.findViewById(R.id.detail_network);
            detailBank = itemView.findViewById(R.id.detail_bank);
            detailHolder = itemView.findViewById(R.id.detail_holder);
            detailNumber = itemView.findViewById(R.id.detail_number);
            detailExpiry = itemView.findViewById(R.id.detail_expiry);
            detailCvv = itemView.findViewById(R.id.detail_cvv);
            detailPin = itemView.findViewById(R.id.detail_pin);
            btnToggleNumber = itemView.findViewById(R.id.btn_toggle_number);
            btnToggleCvv = itemView.findViewById(R.id.btn_toggle_cvv);
            btnTogglePin = itemView.findViewById(R.id.btn_toggle_pin);
            btnCopyType = itemView.findViewById(R.id.btn_copy_type);
            btnCopyNetwork = itemView.findViewById(R.id.btn_copy_network);
            btnCopyBank = itemView.findViewById(R.id.btn_copy_bank);
            btnCopyHolder = itemView.findViewById(R.id.btn_copy_holder);
            btnCopyNumber = itemView.findViewById(R.id.btn_copy_number);
            btnCopyExpiry = itemView.findViewById(R.id.btn_copy_expiry);
            btnCopyCvv = itemView.findViewById(R.id.btn_copy_cvv);
            btnCopyPin = itemView.findViewById(R.id.btn_copy_pin);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
