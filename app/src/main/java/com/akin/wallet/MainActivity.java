package com.akin.wallet;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.graphics.Color;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.DashboardCardAdapter;
import com.akin.wallet.adapter.DashboardIdCardAdapter;
import com.akin.wallet.adapter.DashboardSocialAccountAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Dashboard (Home) — single-screen host, no fragments. Shows live Government
 * IDs, Bank Cards and Social Accounts from SQLite, plus the quick-add FAB
 * menu. Child screens open as full-screen activities and this refreshes in
 * onResume. (Merged from DashboardFragment: one screen never needed the
 * fragment back stack.)
 */
public class MainActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private DashboardCardAdapter cardAdapter;
    private RecyclerView recyclerCarousel;
    private LinearLayoutManager carouselLayoutManager;
    private PagerSnapHelper carouselSnapHelper;
    private View emptyCards;
    private DashboardIdCardAdapter idAdapter;
    private RecyclerView recyclerIdsCarousel;
    private LinearLayoutManager idsLayoutManager;
    private PagerSnapHelper idsSnapHelper;
    private View emptyIds;
    private DashboardSocialAccountAdapter socialAdapter;
    private View cardSocialAccounts;
    private RecyclerView recyclerSocialAccounts;
    private View emptySocialAccounts;

    /** Credit-card ratio shared with the carousel faces (width : height). */
    private static final float CARD_ASPECT_RATIO = 1.586f;
    private FloatingActionButton fabAdd;
    private View fabAddMenu;
    private View fabScrim;
    private boolean isFabMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupSystemBars();

        dbHelper = new AppDatabaseHelper(this);

        setupCardCarousel();
        setupIdsCarousel();
        setupSocialAccounts();
        setupAddMenu();

        findViewById(R.id.btn_view_all_social).setOnClickListener(v -> goTo(R.id.nav_social_account));

        refreshDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    @Override
    protected void onDestroy() {
        // SQLiteOpenHelper holds a pooled connection; release it with the screen.
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupSystemBars();
        }
    }

    /**
     * System bars stay visible (no immersive mode): the bottom navigation bar
     * is shown, not hidden. Re-applied on focus in case the system hid it
     * transiently (e.g. after a fullscreen intent returns).
     */
    private void setupSystemBars() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(getColor(R.color.dashboard_bg_start));
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.show(android.view.WindowInsets.Type.navigationBars());
            }
        } else {
            // Lay out edge-to-edge behind the bar, but keep the bar visible:
            // no HIDE_NAVIGATION / IMMERSIVE_STICKY flags.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    /** Social Account lives outside the single-screen host: open it directly. */
    private void goTo(int navId) {
        if (navId == R.id.nav_social_account) {
            startActivity(new Intent(this, SocialAccountActivity.class));
        }
    }

    /** Extended FAB: round main button expanding the 3-option menu above it. */
    private void setupAddMenu() {
        fabAdd = findViewById(R.id.fab_add);
        fabAddMenu = findViewById(R.id.fab_add_menu);
        fabScrim = findViewById(R.id.fab_scrim);
        if (fabAdd == null) {
            return;
        }
        fabAdd.setOnClickListener(v -> toggleAddMenu());
        if (fabScrim != null) {
            fabScrim.setOnClickListener(v -> {
                if (isFabMenuOpen) {
                    toggleAddMenu();
                }
            });
        }
        findViewById(R.id.fab_option_id).setOnClickListener(v -> openIdCreator());
        findViewById(R.id.fab_option_card).setOnClickListener(v -> openBankCreator());
        findViewById(R.id.fab_option_login).setOnClickListener(v -> addNew(R.id.nav_social_account));
    }

    private void toggleAddMenu() {
        isFabMenuOpen = !isFabMenuOpen;
        if (fabAddMenu != null) {
            if (isFabMenuOpen) {
                fabAddMenu.setVisibility(View.VISIBLE);
                playMenuEntrance();
            } else {
                cancelMenuEntrance();
                fabAddMenu.setVisibility(View.GONE);
            }
        }
        if (fabScrim != null) {
            fabScrim.setVisibility(isFabMenuOpen ? View.VISIBLE : View.GONE);
        }
        if (fabAdd != null) {
            fabAdd.setImageResource(isFabMenuOpen ? R.drawable.ic_close : R.drawable.ic_add);
        }
    }

    /** Staggered fade/rise entrance, top item first (M3 FAB menu motion). */
    private void playMenuEntrance() {
        if (!(fabAddMenu instanceof ViewGroup)) {
            return;
        }
        ViewGroup menu = (ViewGroup) fabAddMenu;
        float rise = dp(10);
        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(rise);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 45L)
                    .setDuration(180L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void cancelMenuEntrance() {
        if (!(fabAddMenu instanceof ViewGroup)) {
            return;
        }
        ViewGroup menu = (ViewGroup) fabAddMenu;
        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);
            child.animate().cancel();
            child.setAlpha(1f);
            child.setTranslationY(0f);
        }
    }

    /** Opens a social edit form directly (only View All opens the account screen). */
    private void openSocialEditor(CredentialItem item) {
        dbHelper.touchLoginUpdatedAt(item.getId());
        startActivity(SocialAccountFormActivity.editIntent(this, item));
    }

    /** Opens the ID creation form directly (FAB is the only entry). */
    private void openIdCreator() {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        startActivity(new Intent(this, GovermentIDFormActivity.class));
    }

    /** Opens an ID edit form directly (dashboard is the editor). */
    private void openIdEditor(IdCardItem item) {
        startActivity(GovermentIDFormActivity.editIntent(this, item));
    }

    /** Opens the bank creation form directly (FAB is the only entry). */
    private void openBankCreator() {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        startActivity(new Intent(this, BankCardFormActivity.class));
    }

    /**
     * Opens a bank card for editing. The tap itself is a recency signal: the
     * card's updated_at is bumped first so it sorts newest-first when the
     * list refreshes on return — even if the edit is cancelled. No immediate
     * refresh here; onResume already re-queries after the editor closes.
     */
    private void openBankEditor(BankCardItem item) {
        dbHelper.touchBankCardUpdatedAt(item.getId());
        startActivity(BankCardFormActivity.editIntent(this, item));
    }

    /** Opens the login creation form directly (full screen, no navigation). */
    private void addNew(int navId) {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        if (navId == R.id.nav_social_account) {
            startActivity(new Intent(this, SocialAccountFormActivity.class));
        }
    }

    /** Horizontal snap carousel rendering the user's real bank cards. */
    private void setupCardCarousel() {
        recyclerCarousel = findViewById(R.id.recycler_cards_carousel);
        emptyCards = findViewById(R.id.empty_cards);

        cardAdapter = new DashboardCardAdapter(this::openBankEditor);
        carouselLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerCarousel.setLayoutManager(carouselLayoutManager);
        recyclerCarousel.setAdapter(cardAdapter);
        recyclerCarousel.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View child,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(child);
                if (position != RecyclerView.NO_POSITION
                        && position < state.getItemCount() - 1) {
                    outRect.right = dp(12);
                }
            }
        });
        carouselSnapHelper = new PagerSnapHelper();
        carouselSnapHelper.attachToRecyclerView(recyclerCarousel);

        if (emptyCards != null) {
            emptyCards.setOnClickListener(v -> openBankCreator());
        }
    }

    private void refreshCardCarousel(List<BankCardItem> cards) {
        if (cardAdapter == null || recyclerCarousel == null) {
            return;
        }
        cardAdapter.updateData(cards);
        boolean hasCards = cards != null && !cards.isEmpty();
        recyclerCarousel.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        if (emptyCards != null) {
            emptyCards.setVisibility(hasCards ? View.GONE : View.VISIBLE);
            if (!hasCards) {
                matchEmptyHeightToCards(recyclerCarousel, emptyCards);
            }
        }
    }

    /**
     * Sizes an empty-state card exactly like one carousel page (0.68 viewport
     * width at 1.586:1) so the section keeps its height with no data.
     * Measures the visible empty card itself — the carousel is GONE here and
     * always measures zero.
     */
    private void matchEmptyHeightToCards(@NonNull RecyclerView carousel, @NonNull View empty) {
        empty.post(() -> {
            int contentWidth = empty.getWidth();
            if (contentWidth <= 0) {
                return;
            }
            int viewport = contentWidth
                    - carousel.getPaddingStart() - carousel.getPaddingEnd();
            if (viewport <= 0) {
                return;
            }
            int pageHeight = (int) ((viewport * DashboardCardAdapter.PAGE_WIDTH_RATIO - dp(8))
                    / CARD_ASPECT_RATIO);
            if (pageHeight > 0 && empty.getLayoutParams().height != pageHeight) {
                empty.getLayoutParams().height = pageHeight;
                empty.requestLayout();
            }
        });
    }

    /** Horizontal snap carousel rendering the user's real government IDs. */
    private void setupIdsCarousel() {
        recyclerIdsCarousel = findViewById(R.id.recycler_ids_carousel);
        emptyIds = findViewById(R.id.empty_ids);

        idAdapter = new DashboardIdCardAdapter(this::openIdEditor);
        idsLayoutManager =
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerIdsCarousel.setLayoutManager(idsLayoutManager);
        recyclerIdsCarousel.setAdapter(idAdapter);
        recyclerIdsCarousel.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View child,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(child);
                if (position != RecyclerView.NO_POSITION
                        && position < state.getItemCount() - 1) {
                    outRect.right = dp(12);
                }
            }
        });
        idsSnapHelper = new PagerSnapHelper();
        idsSnapHelper.attachToRecyclerView(recyclerIdsCarousel);

        if (emptyIds != null) {
            emptyIds.setOnClickListener(v -> openIdCreator());
        }
    }

    private void refreshIdsCarousel(List<IdCardItem> ids) {
        if (idAdapter == null || recyclerIdsCarousel == null) {
            return;
        }
        idAdapter.updateData(ids);
        boolean hasIds = ids != null && !ids.isEmpty();
        recyclerIdsCarousel.setVisibility(hasIds ? View.VISIBLE : View.GONE);
        if (emptyIds != null) {
            emptyIds.setVisibility(hasIds ? View.GONE : View.VISIBLE);
            if (!hasIds) {
                matchEmptyHeightToCards(recyclerIdsCarousel, emptyIds);
            }
        }
    }

    /** Social Account — vertical list of created social accounts only. */
    private void setupSocialAccounts() {
        cardSocialAccounts = findViewById(R.id.card_social_accounts);
        recyclerSocialAccounts = findViewById(R.id.recycler_social_accounts);
        emptySocialAccounts = findViewById(R.id.empty_social_accounts);

        // Row taps intentionally do nothing for now (edit flow to be decided later).
        socialAdapter = new DashboardSocialAccountAdapter(item -> { });
        recyclerSocialAccounts.setLayoutManager(new LinearLayoutManager(this));
        recyclerSocialAccounts.setAdapter(socialAdapter);

        if (emptySocialAccounts != null) {
            emptySocialAccounts.setOnClickListener(v -> addNew(R.id.nav_social_account));
        }
    }

    private void refreshSocialAccounts(List<CredentialItem> accounts) {
        if (socialAdapter == null || recyclerSocialAccounts == null) {
            return;
        }
        socialAdapter.updateData(accounts);
        boolean hasAccounts = accounts != null && !accounts.isEmpty();
        if (cardSocialAccounts != null) {
            cardSocialAccounts.setVisibility(hasAccounts ? View.VISIBLE : View.GONE);
        }
        if (emptySocialAccounts != null) {
            emptySocialAccounts.setVisibility(hasAccounts ? View.GONE : View.VISIBLE);
        }
    }

    private void refreshDashboard() {
        if (dbHelper == null) {
            return;
        }
        List<IdCardItem> ids = dbHelper.getAllIdCards();
        List<BankCardItem> cards = dbHelper.getAllBankCards();
        List<CredentialItem> accounts = dbHelper.getAllLogins();

        refreshCardCarousel(cards);
        refreshIdsCarousel(ids);
        refreshSocialAccounts(accounts);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}
