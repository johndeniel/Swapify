package com.akin.wallet.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;
import com.akin.wallet.model.PlatformIcons;
import com.akin.wallet.util.CardText;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Trash gallery — soft-deleted IDs, bank cards and social accounts as one
 * uniform set of selectable tiles (centered icon well + title + masked
 * hint, paired two-per-row). No real card faces or sensitive values are
 * shown. Tapping a tile toggles its selection (check badge + blue stroke);
 * headers never select. The host reads {@link #selectedEntries()} for bulk
 * restore / delete.
 */
public class TrashGalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TILE = 1;

    public static final int KIND_ID = 0;
    public static final int KIND_CARD = 1;
    public static final int KIND_SOCIAL = 2;

    /** One gallery row: a header or a selectable trashed item. */
    public static final class Entry {
        public final int kind;
        public final boolean header;
        public final String headerTitle;
        public final int headerCount;
        public final IdCardItem id;
        public final BankCardItem card;
        public final CredentialItem account;

        private Entry(int kind, boolean header, String headerTitle, int headerCount,
                      IdCardItem id, BankCardItem card, CredentialItem account) {
            this.kind = kind;
            this.header = header;
            this.headerTitle = headerTitle;
            this.headerCount = headerCount;
            this.id = id;
            this.card = card;
            this.account = account;
        }

        public static Entry header(String title, int count) {
            return new Entry(KIND_ID, true, title, count, null, null, null);
        }

        public static Entry id(IdCardItem item) {
            return new Entry(KIND_ID, false, null, 0, item, null, null);
        }

        public static Entry card(BankCardItem item) {
            return new Entry(KIND_CARD, false, null, 0, null, item, null);
        }

        public static Entry social(CredentialItem item) {
            return new Entry(KIND_SOCIAL, false, null, 0, null, null, item);
        }

        public boolean isSelectable() {
            return !header;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry that = (Entry) o;
            if (header != that.header || kind != that.kind) {
                return false;
            }
            if (header) {
                return headerCount == that.headerCount
                        && java.util.Objects.equals(headerTitle, that.headerTitle);
            }
            switch (kind) {
                case KIND_ID:
                    return java.util.Objects.equals(id, that.id);
                case KIND_CARD:
                    return java.util.Objects.equals(card, that.card);
                case KIND_SOCIAL:
                    return java.util.Objects.equals(account, that.account);
                default:
                    return false;
            }
        }

        @Override
        public int hashCode() {
            if (header) {
                return java.util.Objects.hash(kind, headerTitle, headerCount);
            }
            Object item = kind == KIND_ID ? id : kind == KIND_CARD ? card : account;
            return java.util.Objects.hash(kind, item);
        }

        /** Stable selection key across reloads (kind + row id). */
        public String key() {
            if (header) {
                return "header:" + headerTitle;
            }
            switch (kind) {
                case KIND_ID:
                    return "id:" + (id != null ? id.getId() : -1);
                case KIND_CARD:
                    return "card:" + (card != null ? card.getId() : -1);
                case KIND_SOCIAL:
                    return "social:" + (account != null ? account.getId() : -1);
                default:
                    return "header:" + headerTitle;
            }
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount, int selectableCount);
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Set<String> selectedKeys = new HashSet<>();
    private OnSelectionChangedListener selectionListener;

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    public void updateData(List<Entry> newEntries) {
        List<Entry> next = newEntries != null ? new ArrayList<>(newEntries) : new ArrayList<>();
        androidx.recyclerview.widget.DiffUtil.DiffResult diff =
                androidx.recyclerview.widget.DiffUtil.calculateDiff(
                        new androidx.recyclerview.widget.DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return entries.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return next.size();
                            }

                            @Override
                            public boolean areItemsTheSame(int oldPos, int newPos) {
                                return entries.get(oldPos).key().equals(next.get(newPos).key());
                            }

                            @Override
                            public boolean areContentsTheSame(int oldPos, int newPos) {
                                return entries.get(oldPos).equals(next.get(newPos));
                            }
                        });
        entries.clear();
        entries.addAll(next);
        // Keep selections that still exist (rotation-safe reloads); drop the rest.
        if (!selectedKeys.isEmpty()) {
            Set<String> live = new HashSet<>();
            for (Entry e : entries) {
                if (e.isSelectable()) {
                    live.add(e.key());
                }
            }
            selectedKeys.retainAll(live);
        }
        diff.dispatchUpdatesTo(this);
        emitSelection();
    }

    /** Selection keys for rotation save/restore. */
    @NonNull
    public ArrayList<String> saveSelection() {
        return new ArrayList<>(selectedKeys);
    }

    public void restoreSelection(List<String> keys) {
        selectedKeys.clear();
        if (keys != null) {
            for (String key : keys) {
                if (key != null) {
                    selectedKeys.add(key);
                }
            }
        }
        // Prune keys with no matching row, then repaint only if selecting.
        Set<String> live = new HashSet<>();
        for (Entry e : entries) {
            if (e.isSelectable()) {
                live.add(e.key());
            }
        }
        selectedKeys.retainAll(live);
        if (!selectedKeys.isEmpty()) {
            notifyDataSetChanged();
        }
        emitSelection();
    }

    /** All currently selected (selectable) entries, in gallery order. */
    public List<Entry> selectedEntries() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (e.isSelectable() && selectedKeys.contains(e.key())) {
                out.add(e);
            }
        }
        return out;
    }

    public int getSelectedCount() {
        // Keys only ever hold selectable tiles (toggle/selectAll guard it).
        return selectedKeys.size();
    }

    public int getSelectableCount() {
        int n = 0;
        for (Entry e : entries) {
            if (e.isSelectable()) {
                n++;
            }
        }
        return n;
    }

    public void clearSelection() {
        if (selectedKeys.isEmpty()) {
            return;
        }
        selectedKeys.clear();
        notifyDataSetChanged();
        emitSelection();
    }

    public void selectAll() {
        for (Entry e : entries) {
            if (e.isSelectable()) {
                selectedKeys.add(e.key());
            }
        }
        notifyDataSetChanged();
        emitSelection();
    }

    private void emitSelection() {
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(getSelectedCount(), getSelectableCount());
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= entries.size()) {
            return TYPE_HEADER;
        }
        return entries.get(position).header ? TYPE_HEADER : TYPE_TILE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_trash_header, parent, false));
        } else {
            return new TileHolder(inflater.inflate(R.layout.item_trash_tile, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Entry entry = entries.get(position);
        if (holder instanceof HeaderHolder) {
            HeaderHolder h = (HeaderHolder) holder;
            h.title.setText(entry.headerTitle);
            h.count.setText(h.itemView.getContext()
                    .getString(R.string.trash_header_count, entry.headerCount));
            return;
        }
        TileHolder h = (TileHolder) holder;
        if (entry.kind == KIND_ID) {
            h.icon.setImageResource(R.drawable.ic_person);
            String rawType = entry.id.getIdType() == null ? "" : entry.id.getIdType().trim();
            h.title.setText(rawType.isEmpty() ? "Government ID" : rawType);
            // Same primary number the document face shows (spec-resolved key).
            h.sub.setText(IdTypeSpec.displayNumber(
                    IdTypeSpec.forName(entry.id.getIdType()), entry.id.getFields()));
        } else if (entry.kind == KIND_CARD) {
            h.icon.setImageResource(R.drawable.chip);
            h.title.setText(cardTitle(entry.card));
            h.sub.setText("•••• " + CardText.last4(entry.card.getCardNumber()));
        } else {
            String platform = entry.account.getPlatform() != null
                    && !entry.account.getPlatform().trim().isEmpty()
                    ? entry.account.getPlatform().trim() : "Social Login";
            String username = entry.account.getUsername() != null
                    ? entry.account.getUsername().trim() : "";
            h.title.setText(platform);
            h.sub.setText(username.isEmpty() ? "Social Login" : username);
            PlatformIcons.bindIcon(h.icon, entry.account.getPlatform(), entry.account.getIconRes());
        }
        bindSelectable(h, entry);
    }

    /** Tap toggles selection: badge + blue stroke on, hairline off. */
    private void bindSelectable(TileHolder h, Entry entry) {
        MaterialCardView card = h.card;
        View badge = h.badge;
        boolean selected = selectedKeys.contains(entry.key());
        badge.setVisibility(selected ? View.VISIBLE : View.GONE);
        float density = card.getResources().getDisplayMetrics().density;
        if (selected) {
            card.setStrokeColor(card.getContext().getColor(R.color.brand_blue));
            card.setStrokeWidth(Math.round(2 * density));
        } else {
            card.setStrokeColor(card.getContext().getColor(R.color.dashboard_surface_border));
            card.setStrokeWidth(Math.round(1 * density));
        }
        card.setOnClickListener(v -> {
            // Position resolved at click time: bind-time positions go stale
            // after reloads/removals.
            int pos = h.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION
                    || pos < 0 || pos >= entries.size()) {
                return;
            }
            String key = entries.get(pos).key();
            if (selectedKeys.contains(key)) {
                selectedKeys.remove(key);
            } else {
                selectedKeys.add(key);
            }
            notifyItemChanged(pos);
            emitSelection();
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private static String cardTitle(BankCardItem item) {
        String bank = item.getBankName() != null ? item.getBankName().trim() : "";
        String type = item.getCardType() != null ? item.getCardType().trim() : "";
        if (!bank.isEmpty() && !type.isEmpty()) {
            return bank + " " + type;
        }
        if (!bank.isEmpty()) {
            return bank;
        }
        if (!type.isEmpty()) {
            return type;
        }
        return "Bank Card";
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView count;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.gallery_header_title);
            count = itemView.findViewById(R.id.gallery_header_count);
        }
    }

    static class TileHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        View badge;
        ImageView icon;
        TextView title;
        TextView sub;

        TileHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cell_card);
            badge = itemView.findViewById(R.id.check_badge);
            icon = itemView.findViewById(R.id.tile_icon);
            title = itemView.findViewById(R.id.tile_title);
            sub = itemView.findViewById(R.id.tile_sub);
        }
    }
}
