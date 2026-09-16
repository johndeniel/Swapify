package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard ID carousel — renders the user's real government IDs with the
 * same authentic face as the IDs tab (background ramp, ink, photo well).
 * Pages are sized exactly like the bank-card carousel. Tapping a card
 * opens Government IDs.
 */
public class DashboardIdCardAdapter extends RecyclerView.Adapter<DashboardIdCardAdapter.IdViewHolder> {

    public interface OnIdClickListener {
        void onIdClick(IdCardItem item);
    }

    private final List<IdCardItem> items = new ArrayList<>();
    private final OnIdClickListener listener;

    public DashboardIdCardAdapter(OnIdClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<IdCardItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_id_card, parent, false);
        // Same page size as the bank-card carousel.
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int parentWidth = parent.getMeasuredWidth();
        if (parentWidth <= 0) {
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels;
        }
        if (lp != null && parentWidth > 0) {
            lp.width = (int) (parentWidth * DashboardCardAdapter.PAGE_WIDTH_RATIO);
            view.setLayoutParams(lp);
        }
        return new IdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IdViewHolder holder, int position) {
        IdCardItem item = items.get(position);
        Map<String, String> fields = item.getFields();
        IdTypeSpec.IdType spec = IdTypeSpec.forName(item.getIdType());

        BankCardDesignAdapter.applyCardOutline(holder.cardRoot);
        IdTypeSpec.FaceScheme scheme = IdTypeSpec.faceScheme(item.getIdType());
        holder.cardRoot.setBackgroundResource(scheme.backgroundRes);

        String number = fields.get(spec.numberKey);
        holder.eyebrow.setTextColor(colorOf(holder, scheme.subtitleColorRes));
        holder.previewType.setText(!item.getIdType().trim().isEmpty()
                ? item.getIdType().trim().toUpperCase() : "GOVERNMENT ID");
        holder.previewType.setTextColor(colorOf(holder, scheme.titleColorRes));
        holder.subtitle.setText(IdTypeSpec.previewSubtitle(item.getIdType()));
        holder.subtitle.setTextColor(colorOf(holder, scheme.subtitleColorRes));
        holder.rule.setBackgroundColor(colorOf(holder, scheme.ruleColorRes));
        holder.holderLabel.setTextColor(colorOf(holder, scheme.numberLabelColorRes));
        holder.holder.setText(IdTypeSpec.displayName(spec, fields));
        holder.holder.setTextColor(colorOf(holder, scheme.holderColorRes));
        holder.numberLabel.setText(IdTypeSpec.numberLabel(item.getIdType()));
        holder.numberLabel.setTextColor(colorOf(holder, scheme.numberLabelColorRes));
        holder.number.setText(number != null && !number.trim().isEmpty()
                ? number.trim() : "—");
        holder.number.setTextColor(colorOf(holder, scheme.numberColorRes));
        String meta = IdTypeSpec.buildPreviewMeta(spec, fields);
        holder.meta.setText(meta);
        holder.meta.setTextColor(colorOf(holder, scheme.metaColorRes));
        holder.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        android.graphics.drawable.GradientDrawable photoBg =
                (android.graphics.drawable.GradientDrawable) holder.photoBox.getBackground().mutate();
        photoBg.setColor(colorOf(holder, scheme.photoBgRes));
        float density = holder.cardRoot.getResources().getDisplayMetrics().density;
        photoBg.setStroke((int) (1 * density + 0.5f), colorOf(holder, scheme.photoBorderRes));
        holder.photoIcon.setColorFilter(colorOf(holder, scheme.photoIconRes));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIdClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int colorOf(IdViewHolder holder, int res) {
        return holder.itemView.getResources().getColor(res, null);
    }

    static class IdViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView eyebrow;
        TextView previewType;
        TextView subtitle;
        View rule;
        TextView holderLabel;
        TextView holder;
        TextView numberLabel;
        TextView number;
        TextView meta;
        FrameLayout photoBox;
        ImageView photoIcon;

        IdViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            eyebrow = itemView.findViewById(R.id.preview_eyebrow);
            previewType = itemView.findViewById(R.id.preview_id_type);
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
