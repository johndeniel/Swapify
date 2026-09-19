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
public class GovermentIdAdapter extends RecyclerView.Adapter<GovermentIdAdapter.IdCardViewHolder> {

    public interface OnIdClickListener {
        void onIdClick(IdCardItem item);
    }

    private final List<IdCardItem> idCards = new ArrayList<>();
    private final OnIdClickListener listener;

    public GovermentIdAdapter(OnIdClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<IdCardItem> newIdCards) {
        List<IdCardItem> next =
                newIdCards != null ? new ArrayList<>(newIdCards) : new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return idCards.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return idCards.get(oldPos).getId() == next.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return idCards.get(oldPos).equals(next.get(newPos));
            }
        });
        idCards.clear();
        idCards.addAll(next);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public IdCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        return new IdCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IdCardViewHolder holder, int position) {
        IdCardItem idCard = idCards.get(position);
        Map<String, String> fields = idCard.getFields();
        String rawType = idCard.getIdType() == null ? "" : idCard.getIdType().trim();
        // Unknown types render through the generic face — never masqueraded
        // as another document. forName() falls back to the first type, so it
        // is only called for known types.
        IdTypeSpec.IdType spec = IdTypeSpec.isKnownType(idCard.getIdType())
                ? IdTypeSpec.forName(idCard.getIdType())
                : IdTypeSpec.genericType(idCard.getIdType(), fields);
        GovermentIdFaceRenderer.render(holder.face,
                spec,
                idCard.getIdType(),
                rawType.isEmpty() ? "GOVERNMENT ID" : rawType,
                fields);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onIdClick(idCard);
            }
        });
    }

    @Override
    public int getItemCount() {
        return idCards.size();
    }

    public static class IdCardViewHolder extends RecyclerView.ViewHolder {
        final GovermentIdFaceRenderer.FaceViews face;

        IdCardViewHolder(@NonNull View itemView) {
            super(itemView);
            face = GovermentIdFaceRenderer.FaceViews.bind(itemView);
        }
    }
}
