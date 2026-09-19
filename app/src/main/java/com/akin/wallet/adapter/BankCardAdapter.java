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

    private final List<BankCardItem> cards = new ArrayList<>();
    private final OnCardClickListener listener;

    public BankCardAdapter(OnCardClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<BankCardItem> newCards) {
        List<BankCardItem> next =
                newCards != null ? new ArrayList<>(newCards) : new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return cards.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return cards.get(oldPos).getId() == next.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return cards.get(oldPos).equals(next.get(newPos));
            }
        });
        cards.clear();
        cards.addAll(next);
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
        BankCardItem card = cards.get(position);
        BankCardDesignAdapter.applyCardOutline(holder.cardRoot);
        int design = card.getDesign();
        if (design < 0 || design >= BACKGROUNDS.length) {
            design = 0;
        }
        holder.cardRoot.setBackgroundResource(BACKGROUNDS[design]);
        holder.bank.setText(CardText.safe(card.getBankName(), "YOUR BANK").toUpperCase(java.util.Locale.ROOT));
        holder.cardholder.setText(CardText.safe(card.getHolderName(), "CARDHOLDER NAME").toUpperCase(java.util.Locale.ROOT));
        holder.number.setText(holder.itemView.getContext().getString(R.string.mask_card_number,
                CardText.last4(card.getCardNumber())));
        holder.expiry.setText(CardText.formatExpiry(card.getExpiry()));
        BankCardDesignAdapter.applyNetworkLogo(holder.network, card.getCardNetwork());
        holder.type.setText(CardText.safe(card.getCardType(), "DEBIT").toUpperCase(java.util.Locale.ROOT));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardClick(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView bank;
        ImageView network;
        TextView type;
        TextView number;
        TextView cardholder;
        TextView expiry;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            bank = itemView.findViewById(R.id.preview_bank);
            network = itemView.findViewById(R.id.preview_network);
            type = itemView.findViewById(R.id.preview_type);
            number = itemView.findViewById(R.id.preview_number);
            cardholder = itemView.findViewById(R.id.preview_holder);
            expiry = itemView.findViewById(R.id.preview_expiry);
        }
    }
}
