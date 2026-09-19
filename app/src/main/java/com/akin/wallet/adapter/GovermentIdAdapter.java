package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;
import com.akin.wallet.util.GovermentIdFaceRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Goverment ID carousel — the user's real IDs through the shared
 * {@link GovermentIdFaceRenderer} face (compact mode for the 0.68-width page). Pages
 * are sized exactly like the Bank Card carousel. Tapping a card opens its
 * editor.
 */
public class GovermentIdAdapter extends RecyclerView.Adapter<GovermentIdAdapter.IdViewHolder> {

    public interface OnIdClickListener {
        void onIdClick(IdCardItem item);
    }

    private final List<IdCardItem> items = new ArrayList<>();
    private final OnIdClickListener listener;

    public GovermentIdAdapter(OnIdClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<IdCardItem> newItems) {
        List<IdCardItem> next =
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
    public IdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_id_card, parent, false);
        // Same page size as the bank-card carousel.
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int parentWidth = parent.getMeasuredWidth();
        if (parentWidth <= 0) {
            // Pre-layout inflation: display width minus carousel padding, the
            // same viewport the dashboard measures pages against.
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels
                    - parent.getPaddingStart() - parent.getPaddingEnd();
        }
        if (lp != null && parentWidth > 0) {
            lp.width = (int) (parentWidth * BankCardAdapter.PAGE_WIDTH_RATIO);
            view.setLayoutParams(lp);
        }
        return new IdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IdViewHolder holder, int position) {
        IdCardItem item = items.get(position);
        Map<String, String> fields = item.getFields();
        String rawType = item.getIdType() == null ? "" : item.getIdType().trim();
        // Unknown types render through the generic face — never masqueraded
        // as another document. forName() falls back to the first type, so it
        // is only called for known types.
        IdTypeSpec.IdType spec = IdTypeSpec.isKnownType(item.getIdType())
                ? IdTypeSpec.forName(item.getIdType())
                : IdTypeSpec.genericType(item.getIdType(), fields);
        GovermentIdFaceRenderer.render(holder.face,
                spec,
                item.getIdType(),
                rawType.isEmpty() ? "GOVERNMENT ID" : rawType,
                fields);

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

    static class IdViewHolder extends RecyclerView.ViewHolder {
        final GovermentIdFaceRenderer.FaceViews face;

        IdViewHolder(@NonNull View itemView) {
            super(itemView);
            face = GovermentIdFaceRenderer.FaceViews.bind(itemView);
        }
    }
}
