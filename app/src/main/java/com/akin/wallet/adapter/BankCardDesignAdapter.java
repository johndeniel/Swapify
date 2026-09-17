package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;

public class BankCardDesignAdapter extends RecyclerView.Adapter<BankCardDesignAdapter.CardViewHolder> {

    private final int[] backgrounds = {
            R.drawable.bg_bank_card_blue,
            R.drawable.bg_bank_card_purple,
            R.drawable.bg_bank_card_green,
            R.drawable.bg_bank_card_orange,
            R.drawable.bg_bank_card_slate
    };

    private String bankName = "";
    private String holderName = "";
    private String last4 = "";
    private String expiry = "";
    private String cardType = "Debit";
    private String cardNetwork = "Visa";

    public int getDesignCount() {
        return backgrounds.length;
    }

    public void updatePreview(String bankName, String holderName, String last4,
                              String expiry, String cardType, String cardNetwork) {
        this.bankName = bankName != null ? bankName : "";
        this.holderName = holderName != null ? holderName : "";
        this.last4 = last4 != null ? last4 : "";
        this.expiry = expiry != null ? expiry : "";
        this.cardType = cardType != null ? cardType : "";
        this.cardNetwork = cardNetwork != null ? cardNetwork : "";
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse the exact dashboard card (item_dashboard_card -> includes
        // item_bank_card_preview) so the form picker looks identical to the
        // dashboard carousel. Same 0.68 page-width ratio for same size + peek.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_card, parent, false);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int parentWidth = parent.getMeasuredWidth();
        if (parentWidth <= 0) {
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels;
        }
        if (lp != null && parentWidth > 0) {
            lp.width = (int) (parentWidth * DashboardCardAdapter.PAGE_WIDTH_RATIO);
            view.setLayoutParams(lp);
        }
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        applyCardOutline(holder.cardRoot);
        holder.cardRoot.setBackgroundResource(backgrounds[position]);
        holder.bank.setText(bankName.isEmpty() ? "YOUR BANK" : bankName.toUpperCase());
        holder.holder.setText(holderName.isEmpty() ? "CARDHOLDER NAME" : holderName.toUpperCase());
        holder.number.setText("•••• •••• •••• " + (last4.isEmpty() ? "••••" : last4));
        holder.expiry.setText(expiry.isEmpty() ? "MM/YY" : expiry);
        applyNetworkLogo(holder.network, cardNetwork);
        holder.type.setText(cardType.isEmpty() ? "DEBIT" : cardType.toUpperCase());
    }

    @Override
    public int getItemCount() {
        return backgrounds.length;
    }

    static void applyCardOutline(View cardRoot) {
        float density = cardRoot.getResources().getDisplayMetrics().density;
        cardRoot.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 16 * density);
            }
        });
        cardRoot.setClipToOutline(true);
    }

    static void applyNetworkLogo(ImageView logoView, String network) {
        String name = network != null ? network.trim().toLowerCase() : "visa";
        int icon;
        int heightDp;
        if ("mastercard".equals(name)) {
            icon = R.drawable.mastercard;
            heightDp = 20;
        } else {
            icon = R.drawable.visa;
            heightDp = 11;
        }
        logoView.setImageResource(icon);
        float density = logoView.getResources().getDisplayMetrics().density;
        android.view.ViewGroup.LayoutParams params = logoView.getLayoutParams();
        params.height = (int) (heightDp * density);
        logoView.setLayoutParams(params);
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
