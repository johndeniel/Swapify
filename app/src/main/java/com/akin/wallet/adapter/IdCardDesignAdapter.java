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
 * Fixed authentic preview per ID type — no color picker, nothing reused.
 * National ID always renders the navy/gold layout, Driver's License always
 * renders the pearl-white/purple layout. Typing in the form updates the
 * single preview live via {@link #updatePreview(String, Map)}.
 */
public class IdCardDesignAdapter extends RecyclerView.Adapter<IdCardDesignAdapter.CardViewHolder> {

    private static final int VIEW_NATIONAL = 0;
    private static final int VIEW_DRIVERS = 1;

    private String idType = IdTypeSpec.TYPE_NATIONAL_ID;
    private Map<String, String> fields = new LinkedHashMap<>();

    /** Single fixed design per type (kept for the dots/carousel call sites). */
    public int getDesignCount() {
        return 1;
    }

    public void updatePreview(String idType, Map<String, String> fields) {
        this.idType = idType != null ? idType : "";
        this.fields = fields != null ? new LinkedHashMap<>(fields) : new LinkedHashMap<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return IdCardListAdapter.isDrivers(idType) ? VIEW_DRIVERS : VIEW_NATIONAL;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == VIEW_DRIVERS
                ? R.layout.item_id_preview_drivers
                : R.layout.item_id_preview_national;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
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
        if (holder.subtitle != null) {
            holder.subtitle.setText(getItemViewType(position) == VIEW_DRIVERS
                    ? "REPUBLIC OF THE PHILIPPINES" : "PHILIPPINE IDENTIFICATION");
            holder.subtitle.setVisibility(View.VISIBLE);
        }
        holder.holder.setText(holderName != null && !holderName.trim().isEmpty()
                ? holderName.trim().toUpperCase() : "FULL NAME");
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
        TextView number;
        TextView meta;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            idType = itemView.findViewById(R.id.preview_id_type);
            subtitle = itemView.findViewById(R.id.preview_subtitle);
            holder = itemView.findViewById(R.id.preview_holder);
            number = itemView.findViewById(R.id.preview_number);
            meta = itemView.findViewById(R.id.preview_meta);
        }
    }
}
