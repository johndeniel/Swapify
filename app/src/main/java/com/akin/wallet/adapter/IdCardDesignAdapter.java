package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.IdTypeSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bank-style picker carousel for the ID bottom sheet — but here each page
 * is an ID TYPE's authentic face (National ID navy/gold, Driver's License
 * pearl/purple), all rendered through the SAME central reusable layout.
 * Swiping pages (or the ID Type row) selects the type; typing updates every
 * page live from the shared draft.
 */
public class IdCardDesignAdapter extends RecyclerView.Adapter<IdCardDesignAdapter.CardViewHolder> {

    public interface OnTypePageListener {
        void onTypePageSelected(int typeIndex);
    }

    private final List<IdTypeSpec.IdType> types = IdTypeSpec.getAllTypes();
    private final OnTypePageListener listener;
    private Map<String, String> fields = new LinkedHashMap<>();

    public IdCardDesignAdapter(OnTypePageListener listener) {
        this.listener = listener;
    }

    public int getTypeCount() {
        return types.size();
    }

    public String getTypeAt(int position) {
        if (position < 0 || position >= types.size()) {
            return types.get(0).name;
        }
        return types.get(position).name;
    }

    public void updatePreview(Map<String, String> fields) {
        this.fields = fields != null ? new LinkedHashMap<>(fields) : new LinkedHashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_government_id_preview, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        BankCardDesignAdapter.applyCardOutline(holder.cardRoot);
        String pageType = getTypeAt(position);
        IdTypeSpec.IdType spec = IdTypeSpec.forName(pageType);
        IdTypeSpec.FaceScheme scheme = IdTypeSpec.faceScheme(pageType);

        // Each page shows its own type's values from the shared draft so the
        // user can compare faces while typing (common keys carry over).
        Map<String, String> pageFields = new LinkedHashMap<>();
        for (IdTypeSpec.IdField f : spec.fields) {
            String v = fields.get(f.key);
            pageFields.put(f.key, v != null ? v : "");
        }
        String number = pageFields.get(spec.numberKey);

        holder.cardRoot.setBackgroundResource(scheme.backgroundRes);
        holder.eyebrow.setTextColor(colorOf(holder, scheme.subtitleColorRes));
        holder.idType.setText(pageType.toUpperCase());
        holder.idType.setTextColor(colorOf(holder, scheme.titleColorRes));
        holder.subtitle.setText(IdTypeSpec.previewSubtitle(pageType));
        holder.subtitle.setTextColor(colorOf(holder, scheme.subtitleColorRes));
        holder.rule.setBackgroundColor(colorOf(holder, scheme.ruleColorRes));
        holder.holderLabel.setTextColor(colorOf(holder, scheme.numberLabelColorRes));
        holder.holder.setText(IdTypeSpec.displayName(spec, pageFields));
        holder.holder.setTextColor(colorOf(holder, scheme.holderColorRes));
        holder.numberLabel.setText(IdTypeSpec.numberLabel(pageType));
        holder.numberLabel.setTextColor(colorOf(holder, scheme.numberLabelColorRes));
        holder.number.setText(number != null && !number.trim().isEmpty()
                ? number.trim() : "—");
        holder.number.setTextColor(colorOf(holder, scheme.numberColorRes));
        String meta = IdTypeSpec.buildPreviewMeta(spec, pageFields);
        holder.meta.setText(meta);
        holder.meta.setTextColor(colorOf(holder, scheme.metaColorRes));
        holder.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        // Square photo well adopts the card: tinted fill + border + icon.
        android.graphics.drawable.GradientDrawable photoBg =
                (android.graphics.drawable.GradientDrawable) holder.photoBox.getBackground().mutate();
        photoBg.setColor(colorOf(holder, scheme.photoBgRes));
        float density = holder.cardRoot.getResources().getDisplayMetrics().density;
        photoBg.setStroke((int) (1 * density + 0.5f), colorOf(holder, scheme.photoBorderRes));
        holder.photoIcon.setColorFilter(colorOf(holder, scheme.photoIconRes));

        holder.cardRoot.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && listener != null) {
                listener.onTypePageSelected(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return types.size();
    }

    private static int colorOf(CardViewHolder holder, int colorRes) {
        return holder.cardRoot.getResources().getColor(colorRes, null);
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView eyebrow;
        TextView idType;
        TextView subtitle;
        View rule;
        TextView holderLabel;
        TextView holder;
        TextView numberLabel;
        TextView number;
        TextView meta;
        View photoBox;
        ImageView photoIcon;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            eyebrow = itemView.findViewById(R.id.preview_eyebrow);
            idType = itemView.findViewById(R.id.preview_id_type);
            subtitle = itemView.findViewById(R.id.preview_subtitle);
            rule = itemView.findViewById(R.id.preview_rule);
            holderLabel = itemView.findViewById(R.id.preview_holder_label);
            holder = itemView.findViewById(R.id.preview_holder);
            numberLabel = itemView.findViewById(R.id.preview_number_label);
            number = itemView.findViewById(R.id.preview_number);
            meta = itemView.findViewById(R.id.preview_meta);
            photoBox = itemView.findViewById(R.id.preview_photo_box);
            photoIcon = itemView.findViewById(R.id.preview_photo_icon);
        }
    }
}
