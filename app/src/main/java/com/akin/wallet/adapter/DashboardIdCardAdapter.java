package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard ID carousel — renders the user's real government IDs through the
 * shared {@link IdFaceBinder} face (compact mode for the 0.68-width page).
 * Pages are sized exactly like the bank-card carousel. Tapping a card opens
 * its edit sheet.
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
        String rawType = item.getIdType() == null ? "" : item.getIdType().trim();
        IdFaceBinder.render(holder.face,
                IdTypeSpec.forName(item.getIdType()),
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
        final IdFaceBinder.FaceViews face;

        IdViewHolder(@NonNull View itemView) {
            super(itemView);
            face = IdFaceBinder.FaceViews.bind(itemView);
        }
    }
}
