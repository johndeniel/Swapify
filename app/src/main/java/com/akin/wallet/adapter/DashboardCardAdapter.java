package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.BankCardItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard card carousel — renders the user's real bank cards with the
 * same face as the Cards tab (background ramp, network logo, masked
 * number). Tapping a card opens Bank Cards.
 */
public class DashboardCardAdapter extends RecyclerView.Adapter<DashboardCardAdapter.CardViewHolder> {

    /** Shared carousel page width (fraction of viewport). IDs use the same
        constant so both carousels stay pixel-identical in size. */
    public static final float PAGE_WIDTH_RATIO = 0.68f;

    public interface OnCardClickListener {
        void onCardClick(BankCardItem item);
    }

    private final int[] backgrounds = {
            R.drawable.bg_bank_card_blue,
            R.drawable.bg_bank_card_purple,
            R.drawable.bg_bank_card_green,
            R.drawable.bg_bank_card_orange,
            R.drawable.bg_bank_card_slate
    };

    private final List<BankCardItem> items = new ArrayList<>();
    private final OnCardClickListener listener;

    public DashboardCardAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<BankCardItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_card, parent, false);
        // Slightly narrower than the viewport so the card reads smaller
        // and the next card peeks in from the right.
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int parentWidth = parent.getMeasuredWidth();
        if (parentWidth <= 0) {
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels;
        }
        if (lp != null && parentWidth > 0) {
            lp.width = (int) (parentWidth * PAGE_WIDTH_RATIO);
            view.setLayoutParams(lp);
        }
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
        holder.bank.setText(safe(item.getBankName(), "YOUR BANK").toUpperCase());
        holder.holder.setText(safe(item.getHolderName(), "CARDHOLDER NAME").toUpperCase());
        holder.number.setText("•••• •••• •••• " + last4(item.getCardNumber()));
        holder.expiry.setText(formatExpiry(item.getExpiry()));
        BankCardDesignAdapter.applyNetworkLogo(holder.network, item.getCardNetwork());
        holder.type.setText(safe(item.getCardType(), "DEBIT").toUpperCase());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static String safe(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
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

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            bank = itemView.findViewById(R.id.preview_bank);
            network = itemView.findViewById(R.id.preview_network);
            type = itemView.findViewById(R.id.preview_type);
            number = itemView.findViewById(R.id.preview_number);
            holder = itemView.findViewById(R.id.preview_holder);
            expiry = itemView.findViewById(R.id.preview_expiry);
        }
    }
}
