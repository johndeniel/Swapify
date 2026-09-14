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
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CENTRAL reusable card list for ALL government ID types.
 * One layout ({@code item_government_id.xml}) serves every type — per-type identity
 * (face background, ink, titles, numbers) is bound from {@link IdTypeSpec}, and the
 * (title, number label, values) is bound from {@link IdTypeSpec}, and the
 * expanded details are inflated dynamically from the type's field spec.
 */
public class IdCardListAdapter extends RecyclerView.Adapter<IdCardListAdapter.CardViewHolder> {

    public interface OnIdActionListener {
        void onEdit(IdCardItem item);
        void onDelete(IdCardItem item);
    }

    private final List<IdCardItem> items = new ArrayList<>();
    private final OnIdActionListener listener;
    private int expandedPosition = -1;

    public IdCardListAdapter(List<IdCardItem> items, OnIdActionListener listener) {
        if (items != null) {
            this.items.addAll(items);
        }
        this.listener = listener;
    }

    public void updateData(List<IdCardItem> newItems) {
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
                .inflate(R.layout.item_government_id, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        IdCardItem item = items.get(position);
        Map<String, String> fields = item.getFields();
        IdTypeSpec.IdType spec = IdTypeSpec.forName(item.getIdType());

        // Rounded-corner clipping only — the per-type face (background + ink)
        // comes from FaceScheme on the single shared preview layout.
        BankCardDesignAdapter.applyCardOutline(holder.cardRoot);
        IdTypeSpec.FaceScheme scheme = IdTypeSpec.faceScheme(item.getIdType());
        holder.cardRoot.setBackgroundResource(scheme.backgroundRes);

        String number = fields.get(spec.numberKey);
        holder.previewType.setText(!item.getIdType().trim().isEmpty()
                ? item.getIdType().trim().toUpperCase() : "GOVERNMENT ID");
        holder.previewType.setTextColor(colorOf(holder, scheme.titleColorRes));
        holder.previewSubtitle.setText(IdTypeSpec.previewSubtitle(item.getIdType()));
        holder.previewSubtitle.setTextColor(colorOf(holder, scheme.subtitleColorRes));
        holder.previewRule.setBackgroundColor(colorOf(holder, scheme.ruleColorRes));
        holder.previewHolder.setText(IdTypeSpec.displayName(spec, fields));
        holder.previewHolder.setTextColor(colorOf(holder, scheme.holderColorRes));
        holder.previewNumberLabel.setText(IdTypeSpec.numberLabel(item.getIdType()));
        holder.previewNumberLabel.setTextColor(colorOf(holder, scheme.numberLabelColorRes));
        holder.previewNumber.setText(number != null && !number.trim().isEmpty()
                ? number.trim() : "—");
        holder.previewNumber.setTextColor(colorOf(holder, scheme.numberColorRes));
        String meta = IdTypeSpec.buildPreviewMeta(spec, fields);
        holder.previewMeta.setText(meta);
        holder.previewMeta.setTextColor(colorOf(holder, scheme.metaColorRes));
        holder.previewMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        boolean isExpanded = position == expandedPosition;
        holder.expandedSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (isExpanded) {
            bindDetails(holder.detailsContainer, spec, fields);
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEdit(item);
                }
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(item);
                }
            });
        } else {
            // Avoid leaking listeners / rows when recycled while collapsed.
            holder.detailsContainer.removeAllViews();
            holder.btnEdit.setOnClickListener(null);
            holder.btnDelete.setOnClickListener(null);
        }

        holder.cardRoot.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
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

    private void bindDetails(LinearLayout container, IdTypeSpec.IdType spec,
                             Map<String, String> fields) {
        container.removeAllViews();
        Context ctx = container.getContext();
        LayoutInflater inflater = LayoutInflater.from(ctx);
        Map<String, IdTypeSpec.IdField> display =
                IdTypeSpec.displayFields(spec, fields != null ? fields : new LinkedHashMap<>());

        for (Map.Entry<String, IdTypeSpec.IdField> entry : display.entrySet()) {
            String key = entry.getKey();
            IdTypeSpec.IdField field = entry.getValue();
            String value = fields != null ? fields.get(key) : null;
            if (value == null) {
                value = "";
            }
            final String fullValue = value;

            View row = inflater.inflate(R.layout.item_government_id_detail_row, container, false);
            TextView labelView = row.findViewById(R.id.row_label);
            TextView valueView = row.findViewById(R.id.row_value);
            ImageView copyView = row.findViewById(R.id.btn_copy);

            labelView.setText(field.label);

            // All values shown in full — no masking.
            valueView.setText(fullValue.isEmpty() ? "Not set" : fullValue);

            copyView.setOnClickListener(v ->
                    copyToClipboard(v.getContext(), field.label, fullValue));

            container.addView(row);

            // Divider after every row — including below the last field,
            // above Edit/Delete.
            View divider = new View(ctx);
            divider.setBackgroundColor(
                    ctx.getResources().getColor(R.color.dark_card_border, null));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, (int) (1 * ctx.getResources()
                    .getDisplayMetrics().density));
            divider.setLayoutParams(params);
            container.addView(divider);
        }
    }

    private static int colorOf(CardViewHolder holder, int colorRes) {
        return holder.cardRoot.getResources().getColor(colorRes, null);
    }

    private void copyToClipboard(Context context, String label, String text) {        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text != null ? text : "");
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, label + " copied", Toast.LENGTH_SHORT).show();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        View cardRoot;
        TextView previewType;
        TextView previewSubtitle;
        View previewRule;
        TextView previewHolder;
        TextView previewNumberLabel;
        TextView previewNumber;
        TextView previewMeta;
        LinearLayout expandedSection;
        LinearLayout detailsContainer;
        LinearLayout btnEdit;
        LinearLayout btnDelete;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            previewType = itemView.findViewById(R.id.preview_id_type);
            previewSubtitle = itemView.findViewById(R.id.preview_subtitle);
            previewRule = itemView.findViewById(R.id.preview_rule);
            previewHolder = itemView.findViewById(R.id.preview_holder);
            previewNumberLabel = itemView.findViewById(R.id.preview_number_label);
            previewNumber = itemView.findViewById(R.id.preview_number);
            previewMeta = itemView.findViewById(R.id.preview_meta);
            expandedSection = itemView.findViewById(R.id.expanded_section);
            detailsContainer = itemView.findViewById(R.id.details_container);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
