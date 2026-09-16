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
        holder.holder.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
        holder.previewType.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        holder.subtitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 7);
        holder.number.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
        holder.holderLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 7);
        holder.numberLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 7);
        // Dashboard-only: less space above the header so the face shifts up.
        android.view.View faceContent = (android.view.View) holder.previewType.getParent();
        if (faceContent != null) {
            float cdh = holder.itemView.getResources().getDisplayMetrics().density;
            faceContent.setPadding(
                    faceContent.getPaddingStart(),
                    (int) (6 * cdh),
                    faceContent.getPaddingEnd(),
                    (int) (8 * cdh));
        }
        // Dashboard-only: tighter title -> description -> rule stack.
        float dh = holder.itemView.getResources().getDisplayMetrics().density;
        android.view.ViewGroup.LayoutParams subLp = holder.subtitle.getLayoutParams();
        if (subLp instanceof android.view.ViewGroup.MarginLayoutParams) {
            ((android.view.ViewGroup.MarginLayoutParams) subLp).topMargin = (int) (1 * dh);
            holder.subtitle.setLayoutParams(subLp);
        }
        android.view.ViewGroup.LayoutParams ruleLp = holder.rule.getLayoutParams();
        if (ruleLp instanceof android.view.ViewGroup.MarginLayoutParams) {
            ((android.view.ViewGroup.MarginLayoutParams) ruleLp).topMargin = (int) (4 * dh);
            holder.rule.setLayoutParams(ruleLp);
        }
        holder.holder.setTextColor(colorOf(holder, scheme.holderColorRes));
        holder.numberLabel.setText(IdTypeSpec.numberLabel(item.getIdType()));
        holder.numberLabel.setTextColor(colorOf(holder, scheme.numberLabelColorRes));
        holder.number.setText(number != null && !number.trim().isEmpty()
                ? number.trim() : "—");
        holder.number.setTextColor(colorOf(holder, scheme.numberColorRes));
        String dobValue = fields.get("birth_date");
        holder.dob.setText(dobValue != null && !dobValue.trim().isEmpty() ? dobValue.trim() : "—");
        holder.dob.setTextColor(colorOf(holder, scheme.numberColorRes));
        holder.dob.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
        holder.dobLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 7);
        holder.dobLabel.setTextColor(colorOf(holder, scheme.numberLabelColorRes));
        String expDate = fields.get("expiry_date") != null ? fields.get("expiry_date").trim() : "";
        String meta = expDate.isEmpty() ? "" : "EXP " + expDate;
        holder.meta.setText(meta);
        holder.meta.setTextColor(colorOf(holder, scheme.metaColorRes));
        holder.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        android.graphics.drawable.GradientDrawable photoBg =
                (android.graphics.drawable.GradientDrawable) holder.photoBox.getBackground().mutate();
        photoBg.setColor(colorOf(holder, scheme.photoBgRes));
        float density = holder.cardRoot.getResources().getDisplayMetrics().density;
        photoBg.setStroke((int) (1 * density + 0.5f), colorOf(holder, scheme.photoBorderRes));
        photoBg.setCornerRadius(6 * density);
        holder.photoIcon.setColorFilter(colorOf(holder, scheme.photoIconRes));

        // Dashboard-only: smaller avatar well (IDs tab keeps original size).
        float d = holder.itemView.getResources().getDisplayMetrics().density;
        android.view.ViewGroup.LayoutParams photoLp = holder.photoBox.getLayoutParams();
        if (photoLp != null) {
            int well = (int) (32 * d);
            photoLp.width = well;
            photoLp.height = well;
            holder.photoBox.setLayoutParams(photoLp);
        }
        android.view.ViewGroup.LayoutParams iconLp = holder.photoIcon.getLayoutParams();
        if (iconLp != null) {
            int icon = (int) (24 * d);
            iconLp.width = icon;
            iconLp.height = icon;
            holder.photoIcon.setLayoutParams(iconLp);
        }
        // Dashboard-only: tighter gap above the avatar row.
        android.view.View photoRow = (android.view.View) holder.photoBox.getParent();
        if (photoRow != null) {
            android.view.ViewGroup.LayoutParams rowLp = photoRow.getLayoutParams();
            if (rowLp instanceof android.view.ViewGroup.MarginLayoutParams) {
                ((android.view.ViewGroup.MarginLayoutParams) rowLp).topMargin = (int) (2 * d);
                photoRow.setLayoutParams(rowLp);
            }
            // Top-align avatar and name column (was center-aligned).
            if (photoRow instanceof android.widget.LinearLayout) {
                ((android.widget.LinearLayout) photoRow)
                        .setGravity(android.view.Gravity.TOP);
            }
        }

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
        TextView dob;
        TextView dobLabel;
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
            dob = itemView.findViewById(R.id.preview_dob);
            dobLabel = itemView.findViewById(R.id.preview_dob_label);
            photoBox = itemView.findViewById(R.id.preview_photo_box);
            photoIcon = itemView.findViewById(R.id.preview_photo_icon);
        }
    }
}
