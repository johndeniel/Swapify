package com.akin.wallet.activity;

import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.TrashGalleryAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Trash gallery — restorable soft-deletes as selectable thumbnails.
 * Deleting a Government ID, Bank Card or Social Account stamps deleted_at
 * (dashboard hides it); this screen groups the trashed rows gallery-style:
 * one uniform set of tiles (centered icon + title + masked hint, paired
 * two-per-row). Tapping a tile toggles its selection; the toolbar turns
 * contextual (count + select-all) and the bottom bar restores or
 * permanently deletes the selection in bulk.
 */
public class TrashActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private MaterialToolbar toolbar;
    private RecyclerView recyclerTrash;
    private TrashGalleryAdapter galleryAdapter;
    private View emptyTrash;
    private View actionBar;
    private MaterialButton btnBulkRestore;
    private MaterialButton btnBulkDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        dbHelper = new AppDatabaseHelper(this);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onNavigationBack());
        toolbar.inflateMenu(R.menu.trash_selection);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_select_all) {
                galleryAdapter.selectAll();
                return true;
            }
            return false;
        });

        recyclerTrash = findViewById(R.id.recycler_trash);
        GridLayoutManager grid = new GridLayoutManager(this, 2);
        // Uniform tiles pair up; headers take the full row.
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return galleryAdapter.getItemViewType(position)
                        == TrashGalleryAdapter.TYPE_TILE ? 1 : 2;
            }
        });
        recyclerTrash.setLayoutManager(grid);
        galleryAdapter = new TrashGalleryAdapter();
        galleryAdapter.setOnSelectionChangedListener(
                (selected, total) -> updateChrome(selected, total));
        recyclerTrash.setAdapter(galleryAdapter);

        emptyTrash = findViewById(R.id.empty_trash);
        actionBar = findViewById(R.id.action_bar);
        btnBulkRestore = findViewById(R.id.btn_bulk_restore);
        btnBulkDelete = findViewById(R.id.btn_bulk_delete);
        btnBulkRestore.setOnClickListener(v -> bulkRestore());
        btnBulkDelete.setOnClickListener(v -> confirmBulkDelete());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (galleryAdapter.getSelectedCount() > 0) {
                    galleryAdapter.clearSelection();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrash();
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    /** Back/close: clears an active selection first, finishes otherwise. */
    private void onNavigationBack() {
        if (galleryAdapter != null && galleryAdapter.getSelectedCount() > 0) {
            galleryAdapter.clearSelection();
        } else {
            finish();
        }
    }

    private void loadTrash() {
        if (dbHelper == null) {
            return;
        }
        List<IdCardItem> ids;
        List<BankCardItem> cards;
        List<CredentialItem> accounts;
        try {
            ids = dbHelper.getTrashedIdCards();
            cards = dbHelper.getTrashedBankCards();
            accounts = dbHelper.getTrashedLogins();
        } catch (Exception e) {
            ids = new ArrayList<>();
            cards = new ArrayList<>();
            accounts = new ArrayList<>();
        }

        List<TrashGalleryAdapter.Entry> entries = new ArrayList<>();
        if (ids != null && !ids.isEmpty()) {
            entries.add(TrashGalleryAdapter.Entry.header(
                    getString(R.string.trash_section_ids), ids.size()));
            for (IdCardItem item : ids) {
                entries.add(TrashGalleryAdapter.Entry.id(item));
            }
        }
        if (cards != null && !cards.isEmpty()) {
            entries.add(TrashGalleryAdapter.Entry.header(
                    getString(R.string.trash_section_cards), cards.size()));
            for (BankCardItem item : cards) {
                entries.add(TrashGalleryAdapter.Entry.card(item));
            }
        }
        if (accounts != null && !accounts.isEmpty()) {
            entries.add(TrashGalleryAdapter.Entry.header(
                    getString(R.string.trash_section_social), accounts.size()));
            for (CredentialItem item : accounts) {
                entries.add(TrashGalleryAdapter.Entry.social(item));
            }
        }

        galleryAdapter.updateData(entries);
        boolean allEmpty = entries.isEmpty();
        emptyTrash.setVisibility(allEmpty ? View.VISIBLE : View.GONE);
        recyclerTrash.setVisibility(allEmpty ? View.GONE : View.VISIBLE);
        updateChrome(galleryAdapter.getSelectedCount(), galleryAdapter.getSelectableCount());
    }

    /**
     * Contextual chrome: idle shows "Trash" + back; selecting shows the
     * count + close, the select-all overflow and the bulk action bar.
     */
    private void updateChrome(int selected, int total) {
        boolean selecting = selected > 0;
        toolbar.setTitle(selecting
                ? getString(R.string.trash_selected, selected)
                : getString(R.string.trash_title));
        toolbar.setNavigationIcon(selecting
                ? R.drawable.ic_close : R.drawable.ic_arrow_back);
        if (toolbar.getMenu() != null) {
            toolbar.getMenu().findItem(R.id.action_select_all)
                    .setVisible(selecting && selected < total);
        }
        actionBar.setVisibility(selecting ? View.VISIBLE : View.GONE);
        if (selecting) {
            btnBulkRestore.setText(getString(R.string.trash_restore_count, selected));
            btnBulkDelete.setText(getString(R.string.trash_delete_count, selected));
        }
    }

    /** Restores every selected item to its vault, then reloads. */
    private void bulkRestore() {
        List<TrashGalleryAdapter.Entry> selected = galleryAdapter.selectedEntries();
        if (selected.isEmpty() || dbHelper == null) {
            return;
        }
        for (TrashGalleryAdapter.Entry entry : selected) {
            if (entry.kind == TrashGalleryAdapter.KIND_ID) {
                dbHelper.restoreIdCard(entry.id.getId());
            } else if (entry.kind == TrashGalleryAdapter.KIND_CARD) {
                dbHelper.restoreBankCard(entry.card.getId());
            } else if (entry.kind == TrashGalleryAdapter.KIND_SOCIAL) {
                dbHelper.restoreLogin(entry.account.getId());
            }
        }
        int count = selected.size();
        loadTrash();
        Snackbar.make(findViewById(android.R.id.content),
                getString(R.string.trash_restored_count, count),
                Snackbar.LENGTH_SHORT).show();
    }

    /** Confirms, then permanently deletes every selected item. */
    private void confirmBulkDelete() {
        List<TrashGalleryAdapter.Entry> selected = galleryAdapter.selectedEntries();
        if (selected.isEmpty() || dbHelper == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.trash_delete_forever)
                .setMessage(getString(R.string.trash_delete_many, selected.size()))
                .setPositiveButton(R.string.trash_delete_forever, (d, which) -> {
                    // Snapshot the keys: deletes mutate the backing rows while
                    // the adapter still holds them.
                    List<TrashGalleryAdapter.Entry> doomed =
                            new ArrayList<>(galleryAdapter.selectedEntries());
                    for (TrashGalleryAdapter.Entry entry : doomed) {
                        if (entry.kind == TrashGalleryAdapter.KIND_ID) {
                            dbHelper.deleteIdCard(entry.id.getId());
                        } else if (entry.kind == TrashGalleryAdapter.KIND_CARD) {
                            dbHelper.deleteBankCard(entry.card.getId());
                        } else if (entry.kind == TrashGalleryAdapter.KIND_SOCIAL) {
                            dbHelper.deleteLogin(entry.account.getId());
                        }
                    }
                    int count = doomed.size();
                    loadTrash();
                    Snackbar.make(findViewById(android.R.id.content),
                            getString(R.string.trash_deleted_count, count),
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
