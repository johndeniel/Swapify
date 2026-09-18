package com.akin.wallet.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.graphics.Color;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.DashboardCardAdapter;
import com.akin.wallet.adapter.DashboardIdCardAdapter;
import com.akin.wallet.adapter.DashboardSocialAccountAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.security.AppLockManager;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.search.SearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    // M3 Search state. Masters hold the full newest-first rows; the SearchView
    // filters them into its own result lists (the dashboard behind stays whole).
    private List<IdCardItem> allIds;
    private List<BankCardItem> allCards;
    private List<CredentialItem> allAccounts;
    private String currentQuery = "";
    private static final String KEY_SEARCH_QUERY = "dashboard_search_query";
    private static final String KEY_SEARCH_OPEN = "dashboard_search_open";
    private SearchView searchView;
    private boolean searchShowing = false;
    private DashboardCardAdapter searchCardAdapter;
    private RecyclerView recyclerSearchCards;
    private View searchHeaderCards;
    private DashboardIdCardAdapter searchIdAdapter;
    private RecyclerView recyclerSearchIds;
    private View searchHeaderIds;
    private DashboardSocialAccountAdapter searchSocialAdapter;
    private View cardSearchSocial;
    private RecyclerView recyclerSearchSocial;
    private View searchHeaderSocial;
    private View headerIds;
    private View headerCards;
    private View headerSocial;
    private View emptySearchResults;
    private TextView emptySearchSub;
    private long lastBackgroundAt;

    /**
     * Session re-lock: backing out of the verify screen means "do not enter",
     * so the dashboard closes instead of sitting unlocked behind it.
     */
    private final ActivityResultLauncher<Intent> verifyLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupSystemBars();

        dbHelper = new AppDatabaseHelper(this);

        setupHeader();
        setupCardCarousel();
        setupIdsCarousel();
        setupSocialAccounts();
        setupSearch();
        setupAddMenu();

        if (savedInstanceState != null) {
            // Rotation: restore the query and re-open the SearchView exactly
            // as left (masters load below in refreshDashboard, which
            // re-filters into the results).
            currentQuery = savedInstanceState.getString(KEY_SEARCH_QUERY, "");
            boolean open = savedInstanceState.getBoolean(KEY_SEARCH_OPEN, false);
            if (open && searchView != null) {
                if (!currentQuery.isEmpty()) {
                    searchView.getEditText().setText(currentQuery);
                }
                setFabVisible(false);
                searchView.post(() -> searchView.show());
            }
        }

        refreshDashboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboard();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Vault re-lock: cold starts always land here locked, and returning
        // after the app sat in the background past the grace period asks
        // again. Quick trips (editors) stay unlocked.
        if (dbHelper == null || !AppLockManager.isPinSet(this)) {
            return;
        }
        boolean graceExpired = lastBackgroundAt > 0
                && System.currentTimeMillis() - lastBackgroundAt
                > AppLockManager.SESSION_GRACE_MS;
        if (!AppLockManager.isSessionUnlocked() || graceExpired) {
            verifyLauncher.launch(new Intent(this, LockActivity.class)
                    .putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_VERIFY));
        }
    }

    @Override
    protected void onStop() {
        lastBackgroundAt = System.currentTimeMillis();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_QUERY, currentQuery);
        outState.putBoolean(KEY_SEARCH_OPEN, searchShowing);
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

    /**
     * M3 TopAppBar header: title/subtitle are static in XML; search opens
     * the full-screen SearchView, settings opens Security preferences
     * (biometrics live there). Back closes search first.
     */
    private void setupHeader() {
        headerIds = findViewById(R.id.header_ids);
        headerCards = findViewById(R.id.header_cards);
        headerSocial = findViewById(R.id.header_social);

        View btnSearch = findViewById(R.id.btn_header_search);
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                if (searchView != null) {
                    searchView.show();
                }
            });
        }
        View btnSettings = findViewById(R.id.btn_header_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(
                    v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (searchShowing && searchView != null) {
                    searchView.hide();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /**
     * M3 Search: full-screen SearchView (toolbar back + field + clear built
     * in), opened from the header search IconButton. Typing filters the
     * master lists into its own result lists while the dashboard behind
     * stays whole. The FAB hides while results cover it.
     */
    private void setupSearch() {
        searchView = findViewById(R.id.search_view);
        if (searchView == null) {
            return;
        }

        searchIdAdapter = new DashboardIdCardAdapter(this::openIdEditor);
        recyclerSearchIds = findViewById(R.id.recycler_search_ids);
        recyclerSearchIds.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerSearchIds.setAdapter(searchIdAdapter);
        recyclerSearchIds.addItemDecoration(gapDecoration());
        searchHeaderIds = findViewById(R.id.search_header_ids);

        searchCardAdapter = new DashboardCardAdapter(this::openBankEditor);
        recyclerSearchCards = findViewById(R.id.recycler_search_cards);
        recyclerSearchCards.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerSearchCards.setAdapter(searchCardAdapter);
        recyclerSearchCards.addItemDecoration(gapDecoration());
        searchHeaderCards = findViewById(R.id.search_header_cards);

        // Row taps open the account's edit form, same as the dashboard list.
        searchSocialAdapter = new DashboardSocialAccountAdapter(this::openSocialEditor);
        recyclerSearchSocial = findViewById(R.id.recycler_search_social);
        recyclerSearchSocial.setLayoutManager(new LinearLayoutManager(this));
        recyclerSearchSocial.setAdapter(searchSocialAdapter);
        cardSearchSocial = findViewById(R.id.card_search_social);
        searchHeaderSocial = findViewById(R.id.search_header_social);

        emptySearchResults = findViewById(R.id.empty_search_results);
        emptySearchSub = findViewById(R.id.empty_search_sub);

        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString() : "";
                updateSearchResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        searchView.addTransitionListener((view, oldState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWN) {
                searchShowing = true;
                if (isFabMenuOpen) {
                    toggleAddMenu();
                }
                setFabVisible(false);
                // Re-filter on open (covers rotation restore: the query is
                // set before show, masters load separately).
                updateSearchResults();
            } else if (newState == SearchView.TransitionState.HIDDEN) {
                searchShowing = false;
                setFabVisible(true);
            }
        });
    }

    /** 12dp inter-card gap shared by the dashboard and search carousels. */
    private RecyclerView.ItemDecoration gapDecoration() {
        return new RecyclerView.ItemDecoration() {
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
        };
    }

    /** FAB hides while the SearchView covers the screen. */
    private void setFabVisible(boolean visible) {
        if (fabAdd != null) {
            fabAdd.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Filters the master lists into the SearchView results. Empty query shows
     * everything newest-first; non-empty collapses empty sections and shows
     * one global "no results" card when nothing matches anywhere.
     */
    private void updateSearchResults() {
        if (allIds == null || allCards == null || allAccounts == null
                || searchIdAdapter == null || searchCardAdapter == null
                || searchSocialAdapter == null) {
            return;
        }
        String q = currentQuery.trim().toLowerCase(Locale.US);
        if (q.isEmpty()) {
            bindSearchResults(allIds, allCards, allAccounts, false);
            return;
        }
        String digits = q.replaceAll("\\D", "");
        bindSearchResults(filterIds(allIds, q), filterCards(allCards, q, digits),
                filterAccounts(allAccounts, q), true);
    }

    private void bindSearchResults(List<IdCardItem> ids, List<BankCardItem> cards,
                                   List<CredentialItem> accounts, boolean searching) {
        searchIdAdapter.updateData(ids);
        boolean hasIds = ids != null && !ids.isEmpty();
        recyclerSearchIds.setVisibility(hasIds ? View.VISIBLE : View.GONE);
        searchHeaderIds.setVisibility(hasIds ? View.VISIBLE : View.GONE);

        searchCardAdapter.updateData(cards);
        boolean hasCards = cards != null && !cards.isEmpty();
        recyclerSearchCards.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        searchHeaderCards.setVisibility(hasCards ? View.VISIBLE : View.GONE);

        searchSocialAdapter.updateData(accounts);
        boolean hasAccounts = accounts != null && !accounts.isEmpty();
        cardSearchSocial.setVisibility(hasAccounts ? View.VISIBLE : View.GONE);
        searchHeaderSocial.setVisibility(hasAccounts ? View.VISIBLE : View.GONE);

        boolean allEmpty = !hasIds && !hasCards && !hasAccounts;
        emptySearchResults.setVisibility(
                searching && allEmpty ? View.VISIBLE : View.GONE);
        if (searching && allEmpty && emptySearchSub != null) {
            emptySearchSub.setText(
                    getString(R.string.search_empty_sub) + " for \"" + currentQuery.trim() + "\"");
        }
    }

    private static List<IdCardItem> filterIds(List<IdCardItem> source, String q) {
        List<IdCardItem> out = new ArrayList<>();
        for (IdCardItem item : source) {
            if (containsText(item.getIdType(), q) || idFieldsContain(item, q)) {
                out.add(item);
            }
        }
        return out;
    }

    private static List<BankCardItem> filterCards(
            List<BankCardItem> source, String q, String digits) {
        List<BankCardItem> out = new ArrayList<>();
        for (BankCardItem item : source) {
            // Non-secret fields only: card number matches on digits so "1234"
            // finds "•••• •••• •••• 1234". CVV/PIN are never matched.
            if (containsText(item.getBankName(), q)
                    || containsText(item.getHolderName(), q)
                    || containsText(item.getCardType(), q)
                    || containsText(item.getCardNetwork(), q)
                    || cardNumberContains(item, digits)) {
                out.add(item);
            }
        }
        return out;
    }

    private static List<CredentialItem> filterAccounts(List<CredentialItem> source, String q) {
        List<CredentialItem> out = new ArrayList<>();
        for (CredentialItem item : source) {
            // Non-secret fields only: password/PIN stay out of the index.
            if (containsText(item.getPlatform(), q)
                    || containsText(item.getUsername(), q)
                    || containsText(item.getMobile(), q)) {
                out.add(item);
            }
        }
        return out;
    }

    private static boolean containsText(String value, String q) {
        return value != null && !value.trim().isEmpty()
                && value.toLowerCase(Locale.US).contains(q);
    }

    private static boolean idFieldsContain(@NonNull IdCardItem id, String q) {
        Map<String, String> fields = id.getFields();
        for (String value : fields.values()) {
            if (containsText(value, q)) {
                return true;
            }
        }
        return false;
    }

    /** Card numbers are raw digits; match on digits only (CVV/PIN excluded). */
    private static boolean cardNumberContains(@NonNull BankCardItem card, String digits) {
        if (digits.isEmpty() || card.getCardNumber() == null) {
            return false;
        }
        String numberDigits = card.getCardNumber().replaceAll("\\D", "");
        return !numberDigits.isEmpty() && numberDigits.contains(digits);
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
        findViewById(R.id.fab_option_login).setOnClickListener(v -> openSocialCreator());
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

    /** Opens a social edit form directly (dashboard rows open the editor). */
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

    /**
     * Opens an ID edit form directly (dashboard is the editor). The tap itself
     * is a recency signal: updated_at is bumped first so the ID sorts
     * newest-first on return — mirroring the bank-card and login open paths.
     * No immediate refresh here; onResume re-queries after the editor closes.
     */
    private void openIdEditor(IdCardItem item) {
        dbHelper.touchIdCardUpdatedAt(item.getId());
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

    /** Opens the login creation form directly (dashboard rows open the editor). */
    private void openSocialCreator() {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        startActivity(new Intent(this, SocialAccountFormActivity.class));
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
        recyclerCarousel.addItemDecoration(gapDecoration());
        carouselSnapHelper = new PagerSnapHelper();
        carouselSnapHelper.attachToRecyclerView(recyclerCarousel);

        if (emptyCards != null) {
            emptyCards.setOnClickListener(v -> openBankCreator());
        }
        View btnEmptyCards = findViewById(R.id.btn_empty_cards);
        if (btnEmptyCards != null) {
            btnEmptyCards.setOnClickListener(v -> openBankCreator());
        }
    }

    private void refreshCardCarousel(List<BankCardItem> cards) {
        if (cardAdapter == null || recyclerCarousel == null) {
            return;
        }
        cardAdapter.updateData(cards);
        boolean hasCards = cards != null && !cards.isEmpty();
        recyclerCarousel.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        if (headerCards != null) {
            headerCards.setVisibility(View.VISIBLE);
        }
        if (emptyCards != null) {
            emptyCards.setVisibility(!hasCards ? View.VISIBLE : View.GONE);
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
        recyclerIdsCarousel.addItemDecoration(gapDecoration());
        idsSnapHelper = new PagerSnapHelper();
        idsSnapHelper.attachToRecyclerView(recyclerIdsCarousel);

        if (emptyIds != null) {
            emptyIds.setOnClickListener(v -> openIdCreator());
        }
        View btnEmptyIds = findViewById(R.id.btn_empty_ids);
        if (btnEmptyIds != null) {
            btnEmptyIds.setOnClickListener(v -> openIdCreator());
        }
    }

    private void refreshIdsCarousel(List<IdCardItem> ids) {
        if (idAdapter == null || recyclerIdsCarousel == null) {
            return;
        }
        idAdapter.updateData(ids);
        boolean hasIds = ids != null && !ids.isEmpty();
        recyclerIdsCarousel.setVisibility(hasIds ? View.VISIBLE : View.GONE);
        if (headerIds != null) {
            headerIds.setVisibility(View.VISIBLE);
        }
        if (emptyIds != null) {
            emptyIds.setVisibility(!hasIds ? View.VISIBLE : View.GONE);
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

        // Row taps open the account's edit form, same as the IDs and cards above.
        socialAdapter = new DashboardSocialAccountAdapter(this::openSocialEditor);
        recyclerSocialAccounts.setLayoutManager(new LinearLayoutManager(this));
        recyclerSocialAccounts.setAdapter(socialAdapter);

        if (emptySocialAccounts != null) {
            emptySocialAccounts.setOnClickListener(v -> openSocialCreator());
        }
        View btnEmptySocial = findViewById(R.id.btn_empty_social);
        if (btnEmptySocial != null) {
            btnEmptySocial.setOnClickListener(v -> openSocialCreator());
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
        if (headerSocial != null) {
            headerSocial.setVisibility(View.VISIBLE);
        }
        if (emptySocialAccounts != null) {
            emptySocialAccounts.setVisibility(!hasAccounts ? View.VISIBLE : View.GONE);
        }
    }

    private void refreshDashboard() {
        if (dbHelper == null) {
            return;
        }
        allIds = dbHelper.getAllIdCards();
        allCards = dbHelper.getAllBankCards();
        allAccounts = dbHelper.getAllLogins();

        refreshIdsCarousel(allIds);
        refreshCardCarousel(allCards);
        refreshSocialAccounts(allAccounts);

        // Editors close back here: re-filter open results off fresh masters.
        if (searchShowing) {
            updateSearchResults();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}
