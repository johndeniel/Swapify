package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.IdTypeSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Type-picker carousel for the Government ID form — each page is an ID type
 * rendered with the single shared dashboard item (item_dashboard_id_card)
 * and face metrics, driven by the shared draft. Same 0.68 page width, 12dp
 * gap and snap as Home. Swiping pages selects the type; typing updates live.
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
        // Single shared face: same XML + same 0.68 page-width ratio as the
        // dashboard carousel so the form picker looks identical to Home.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_id_card, parent, false);
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int parentWidth = parent.getMeasuredWidth();
        if (parentWidth <= 0) {
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels;
        }
        if (lp != null && parentWidth > 0) {
            lp.width = (int) (parentWidth * DashboardCardAdapter.PAGE_WIDTH_RATIO);
            view.setLayoutParams(lp);
        }
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        String pageType = getTypeAt(position);
        IdTypeSpec.IdType spec = IdTypeSpec.forName(pageType);

        // Each page shows its own type's values from the shared draft so the
        // user can compare faces while typing (common keys carry over).
        Map<String, String> pageFields = new LinkedHashMap<>();
        for (IdTypeSpec.IdField f : spec.fields) {
            String v = fields.get(f.key);
            pageFields.put(f.key, v != null ? v : "");
        }
        IdFaceBinder.render(holder.face, spec, pageType, pageType, pageFields);

        holder.face.cardRoot.setOnClickListener(v -> {
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

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final IdFaceBinder.FaceViews face;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            face = IdFaceBinder.FaceViews.bind(itemView);
        }
    }
}
