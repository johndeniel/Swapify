package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.IdTypeSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fixed preview for the ID bottom sheet — the SAME central reusable face
 * ({@code item_id_card_preview.xml}) used by the list. Typing in the form
 * updates it live via {@link #updatePreview(String, Map)}; switching ID type
 * only changes the bound title, number label and values.
 */
public class IdCardDesignAdapter extends RecyclerView.Adapter<IdCardDesignAdapter.CardViewHolder> {

    private String idType = IdTypeSpec.TYPE_NATIONAL_ID;
    private Map<String, String> fields = new LinkedHashMap<>();

    /** Single fixed preview (kept for the carousel call sites). */
    public int getDesignCount() {
        return 1;
    }

    public void updatePreview(String idType, Map<String, String> fields) {
        this.idType = idType != null ? idType : "";
        this.fields = fields != null ? new LinkedHashMap<>(fields) : new LinkedHashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_id_card_preview, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        BankCardDesignAdapter.applyCardOutline(holder.cardRoot);
        IdTypeSpec.IdType spec = IdTypeSpec.forName(idType);
        String holderName = fields.get(spec.nameKey);
        String number = fields.get(spec.numberKey);

        holder.idType.setText(idType.trim().isEmpty()
                ? "GOVERNMENT ID" : idType.trim().toUpperCase());
        holder.subtitle.setText(IdTypeSpec.previewSubtitle(idType));
        holder.holder.setText(holderName != null && !holderName.trim().isEmpty()
                ? holderName.trim().toUpperCase() : "FULL NAME");
        holder.numberLabel.setText(IdTypeSpec.numberLabel(idType));
        holder.number.setText(number != null && !number.trim().isEmpty()
                ? number.trim() : "—");
        String meta = IdTypeSpec.buildPreviewMeta(spec, fields);
        holder.meta.setText(meta);
        holder.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView idType;
        TextView subtitle;
        TextView holder;
        TextView numberLabel;
        TextView number;
        TextView meta;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            idType = itemView.findViewById(R.id.preview_id_type);
            subtitle = itemView.findViewById(R.id.preview_subtitle);
            holder = itemView.findViewById(R.id.preview_holder);
            numberLabel = itemView.findViewById(R.id.preview_number_label);
            number = itemView.findViewById(R.id.preview_number);
            meta = itemView.findViewById(R.id.preview_meta);
        }
    }
}
