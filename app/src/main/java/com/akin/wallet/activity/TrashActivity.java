package com.akin.wallet.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.TrashGalleryAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.util.Dialogs;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private AlertDialog bulkDeleteDialog;

    private static final String KEY_SELECTION = "trash_selection";

    /** Single-thread vault I/O: reads and bulk writes stay off the UI thread. */
    private final ExecutorService dbIo = Executors.newSingleThreadExecutor();
    private int loadGeneration;
    /** Selection restored once after the first post-rotation load. */
    private ArrayList<String> pendingSelection;

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
        // Uniform tiles pair up; headers take the full row. Bounds-guarded:
        // layout can probe positions mid-animation that no longer exist.
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (galleryAdapter == null
                        || position < 0 || position >= galleryAdapter.getItemCount()) {
                    return 2;
                }
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

        if (savedInstanceState != null) {
            pendingSelection = savedInstanceState.getStringArrayList(KEY_SELECTION);
        }

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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (galleryAdapter != null) {
            outState.putStringArrayList(KEY_SELECTION, galleryAdapter.saveSelection());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrash();
    }

    @Override
    protected void onDestroy() {
        if (bulkDeleteDialog != null && bulkDeleteDialog.isShowing()) {
            bulkDeleteDialog.dismiss();
        }
        bulkDeleteDialog = null;
        dbIo.shutdownNow();
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
        final int generation = ++loadGeneration;
        dbIo.execute(() -> {
            final List<IdCardItem> ids;
            final List<BankCardItem> cards;
            final List<CredentialItem> accounts;
            try {
                ids = dbHelper.getTrashedIdCards();
                cards = dbHelper.getTrashedBankCards();
                accounts = dbHelper.getTrashedLogins();
            } catch (RuntimeException e) {
                runOnUiThread(() -> {
                    if (generation != loadGeneration || isFinishing()) {
                        return;
                    }
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.err_trash_load, Snackbar.LENGTH_SHORT).show();
                });
                return;
            }
            runOnUiThread(() -> {
                if (generation != loadGeneration || isFinishing()) {
                    return;
                }
                bindTrash(ids, cards, accounts);
            });
        });
    }

    /** Binds one loaded snapshot on the UI thread (adapter + chrome). */
    private void bindTrash(List<IdCardItem> ids, List<BankCardItem> cards,
                           List<CredentialItem> accounts) {
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
        if (pendingSelection != null) {
            galleryAdapter.restoreSelection(pendingSelection);
            pendingSelection = null;
        }
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
            MenuItem selectAll = toolbar.getMenu().findItem(R.id.action_select_all);
            if (selectAll != null) {
                selectAll.setVisible(selecting && selected < total);
            }
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
        List<Integer> ids = new ArrayList<>();
        List<Integer> cards = new ArrayList<>();
        List<Integer> socials = new ArrayList<>();
        splitSelection(selected, ids, cards, socials);
        final int count = selected.size();
        dbIo.execute(() -> {
            dbHelper.restoreIdCards(ids);
            dbHelper.restoreBankCards(cards);
            dbHelper.restoreLogins(socials);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                loadTrash();
                Snackbar.make(findViewById(android.R.id.content),
                        getString(R.string.trash_restored_count, count),
                        Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    /** Groups selected entries into per-table id lists for batch writes. */
    private static void splitSelection(List<TrashGalleryAdapter.Entry> selected,
                                       List<Integer> ids, List<Integer> cards,
                                       List<Integer> socials) {
        for (TrashGalleryAdapter.Entry entry : selected) {
            if (entry.kind == TrashGalleryAdapter.KIND_ID && entry.id != null) {
                ids.add(entry.id.getId());
            } else if (entry.kind == TrashGalleryAdapter.KIND_CARD && entry.card != null) {
                cards.add(entry.card.getId());
            } else if (entry.kind == TrashGalleryAdapter.KIND_SOCIAL && entry.account != null) {
                socials.add(entry.account.getId());
            }
        }
    }

    /** Confirms, then permanently deletes every selected item. */
    private void confirmBulkDelete() {
        List<TrashGalleryAdapter.Entry> selected = galleryAdapter.selectedEntries();
        if (selected.isEmpty() || dbHelper == null) {
            return;
        }
        // Snapshot: deletes mutate the backing rows while the adapter
        // still holds them; keyed ids survive the reload either way.
        List<Integer> ids = new ArrayList<>();
        List<Integer> cards = new ArrayList<>();
        List<Integer> socials = new ArrayList<>();
        splitSelection(selected, ids, cards, socials);
        final int count = selected.size();
        bulkDeleteDialog = Dialogs.confirmDelete(this,
                getString(R.string.trash_delete_forever),
                getString(R.string.trash_delete_many, count),
                () -> dbIo.execute(() -> {
                    dbHelper.deleteIdCards(ids);
                    dbHelper.deleteBankCards(cards);
                    dbHelper.deleteLogins(socials);
                    runOnUiThread(() -> {
                        if (isFinishing()) {
                            return;
                        }
                        loadTrash();
                        Snackbar.make(findViewById(android.R.id.content),
                                getString(R.string.trash_deleted_count, count),
                                Snackbar.LENGTH_SHORT).show();
                    });
                }));
    }
}
