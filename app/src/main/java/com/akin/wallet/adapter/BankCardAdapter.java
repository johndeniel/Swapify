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
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.util.CardText;

import java.util.ArrayList;
import java.util.List;

/**
 * Bank Card carousel — the user's real bank cards with the authentic face
 * (background ramp, network logo, masked number). Tapping a card opens its
 * editor.
 */
public class BankCardAdapter extends RecyclerView.Adapter<BankCardAdapter.CardViewHolder> {

    /** Shared carousel page width (fraction of viewport). IDs use the same
        constant so both carousels stay pixel-identical in size. */
    public static final float PAGE_WIDTH_RATIO = 0.68f;

    /**
     * Card face ramps, shared with the bank picker. Order doubles as the
     * persisted design index — never reorder without a DB migration.
     */
    static final int[] BACKGROUNDS = {
            R.drawable.bg_bank_card_blue,
            R.drawable.bg_bank_card_purple,
            R.drawable.bg_bank_card_green,
            R.drawable.bg_bank_card_orange,
            R.drawable.bg_bank_card_slate
    };

    public interface OnCardClickListener {
        void onCardClick(BankCardItem item);
    }

    private final List<BankCardItem> items = new ArrayList<>();
    private final OnCardClickListener listener;

    public BankCardAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<BankCardItem> newItems) {
        List<BankCardItem> next =
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
                return items.get(oldPos).equals(next.get(newPos));
            }
        });
        items.clear();
        items.addAll(next);
        diff.dispatchUpdatesTo(this);
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
            // Pre-layout inflation: display width minus carousel padding, the
            // same viewport the dashboard measures pages against.
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels
                    - parent.getPaddingStart() - parent.getPaddingEnd();
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
        if (design < 0 || design >= BACKGROUNDS.length) {
            design = 0;
        }
        holder.cardRoot.setBackgroundResource(BACKGROUNDS[design]);
        holder.bank.setText(CardText.safe(item.getBankName(), "YOUR BANK").toUpperCase(java.util.Locale.ROOT));
        holder.holder.setText(CardText.safe(item.getHolderName(), "CARDHOLDER NAME").toUpperCase(java.util.Locale.ROOT));
        holder.number.setText("•••• •••• •••• " + CardText.last4(item.getCardNumber()));
        holder.expiry.setText(CardText.formatExpiry(item.getExpiry()));
        BankCardDesignAdapter.applyNetworkLogo(holder.network, item.getCardNetwork());
        holder.type.setText(CardText.safe(item.getCardType(), "DEBIT").toUpperCase(java.util.Locale.ROOT));

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
