package com.akin.wallet.fragment;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.MainActivity;
import com.akin.wallet.R;
import com.akin.wallet.adapter.DashboardCardAdapter;
import com.akin.wallet.adapter.DashboardIdCardAdapter;
import com.akin.wallet.adapter.DashboardLoginAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Dashboard (Home) — overhaul screen matching the MyKey reference.
 * Shows live item counts from SQLite; Recent Access lists only the
 * user's created social logins.
 */
public class DashboardFragment extends Fragment {

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
    private DashboardLoginAdapter loginAdapter;
    private RecyclerView recyclerRecentLogins;
    private View emptyLogins;
    private FloatingActionButton fabAdd;
    private View fabAddMenu;
    private View fabScrim;
    private boolean isFabMenuOpen = false;
    private Fragment sheetHost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new AppDatabaseHelper(requireContext());

        setupCardCarousel(view);
        setupIdsCarousel(view);
        setupRecentLogins(view);
        setupAddMenu(view);

        view.findViewById(R.id.btn_view_all_logins).setOnClickListener(v -> goTo(R.id.nav_login));

        refreshDashboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboard();
    }

    private void goTo(int navId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(navId);
        }
    }

    /** Extended FAB: round main button expanding the 3-option menu above it. */
    private void setupAddMenu(@NonNull View view) {
        fabAdd = view.findViewById(R.id.fab_add);
        fabAddMenu = view.findViewById(R.id.fab_add_menu);
        fabScrim = view.findViewById(R.id.fab_scrim);
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
        view.findViewById(R.id.fab_option_id).setOnClickListener(v -> openIdCreator());
        view.findViewById(R.id.fab_option_card).setOnClickListener(v -> openBankCreator());
        view.findViewById(R.id.fab_option_login).setOnClickListener(v -> addNew(R.id.nav_login));
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

    /** Opens the ID creation sheet directly (IDs tab is gone; FAB is the only entry). */
    private void openIdCreator() {
        if (sheetHost != null) {
            getChildFragmentManager().beginTransaction().remove(sheetHost).commitNow();
            sheetHost = null;
        }
        IdentificationFragment host = new IdentificationFragment();
        getChildFragmentManager().beginTransaction().add(host, null).commitNow();
        sheetHost = host;
        host.initSheetHost(requireContext(), () -> {
            if (isAdded()) {
                refreshDashboard();
            }
        });
        host.openAddSheet();
    }

    /** Opens an ID edit sheet directly (IDs tab is gone; dashboard is the editor). */
    private void openIdEditor(IdCardItem item) {
        if (sheetHost != null) {
            getChildFragmentManager().beginTransaction().remove(sheetHost).commitNow();
            sheetHost = null;
        }
        IdentificationFragment host = new IdentificationFragment();
        getChildFragmentManager().beginTransaction().add(host, null).commitNow();
        sheetHost = host;
        host.initSheetHost(requireContext(), () -> {
            if (isAdded()) {
                refreshDashboard();
            }
        });
        host.openEditSheet(item);
    }

    /**
     * Opens a creation sheet directly over the dashboard — no navigation, so
     * the destination tab is never shown. The tab fragment is attached
     * headless purely as the sheet owner; dashboard refreshes on save.
     */
    /** Opens the bank creation sheet directly (bank tab is gone; FAB is the only entry). */
    private void openBankCreator() {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        if (sheetHost != null) {
            getChildFragmentManager().beginTransaction().remove(sheetHost).commitNow();
            sheetHost = null;
        }
        BankCardsFragment host = new BankCardsFragment();
        getChildFragmentManager().beginTransaction().add(host, null).commitNow();
        sheetHost = host;
        host.initSheetHost(requireContext(), () -> {
            if (isAdded()) {
                refreshDashboard();
            }
        });
        host.openAddSheet();
    }

    /** Opens a bank edit sheet directly (bank tab is gone; dashboard is the editor). */
    private void openBankEditor(BankCardItem item) {
        if (sheetHost != null) {
            getChildFragmentManager().beginTransaction().remove(sheetHost).commitNow();
            sheetHost = null;
        }
        BankCardsFragment host = new BankCardsFragment();
        getChildFragmentManager().beginTransaction().add(host, null).commitNow();
        sheetHost = host;
        host.initSheetHost(requireContext(), () -> {
            if (isAdded()) {
                refreshDashboard();
            }
        });
        host.openEditSheet(item);
    }

    /**
     * Opens a creation sheet directly over the dashboard — no navigation, so
     * the destination tab is never shown. The tab fragment is attached
     * headless purely as the sheet owner; dashboard refreshes on save.
     * (Bank/ID creation moved to openBankCreator/openIdCreator.)
     */
    private void addNew(int navId) {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        if (sheetHost != null) {
            getChildFragmentManager().beginTransaction().remove(sheetHost).commitNow();
            sheetHost = null;
        }
        if (navId == R.id.nav_login) {
            SocialLoginsFragment host = new SocialLoginsFragment();
            getChildFragmentManager().beginTransaction().add(host, null).commitNow();
            sheetHost = host;
            host.initSheetHost(requireContext(), () -> {
                if (isAdded()) {
                    refreshDashboard();
                }
            });
            host.openAddSheet();
        }
    }

    /** Horizontal snap carousel rendering the user's real bank cards. */
    private void setupCardCarousel(@NonNull View view) {
        recyclerCarousel = view.findViewById(R.id.recycler_cards_carousel);
        emptyCards = view.findViewById(R.id.empty_cards);

        cardAdapter = new DashboardCardAdapter(this::openBankEditor);
        carouselLayoutManager =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
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
        }
    }

    /** Horizontal snap carousel rendering the user's real government IDs. */
    private void setupIdsCarousel(@NonNull View view) {
        recyclerIdsCarousel = view.findViewById(R.id.recycler_ids_carousel);
        emptyIds = view.findViewById(R.id.empty_ids);

        idAdapter = new DashboardIdCardAdapter(this::openIdEditor);
        idsLayoutManager =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
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
        }
    }

    /** Recent Access — vertical list of created social logins only. */
    private void setupRecentLogins(@NonNull View view) {
        recyclerRecentLogins = view.findViewById(R.id.recycler_recent_logins);
        emptyLogins = view.findViewById(R.id.empty_logins);

        loginAdapter = new DashboardLoginAdapter(item -> goTo(R.id.nav_login));
        recyclerRecentLogins.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecentLogins.setAdapter(loginAdapter);

        if (emptyLogins != null) {
            emptyLogins.setOnClickListener(v -> goTo(R.id.nav_login));
        }
    }

    private void refreshRecentLogins(List<CredentialItem> logins) {
        if (loginAdapter == null || recyclerRecentLogins == null) {
            return;
        }
        loginAdapter.updateData(logins);
        boolean hasLogins = logins != null && !logins.isEmpty();
        recyclerRecentLogins.setVisibility(hasLogins ? View.VISIBLE : View.GONE);
        if (emptyLogins != null) {
            emptyLogins.setVisibility(hasLogins ? View.GONE : View.VISIBLE);
        }
    }

    private void refreshDashboard() {
        if (dbHelper == null) {
            return;
        }
        List<IdCardItem> ids = dbHelper.getAllIdCards();
        List<BankCardItem> cards = dbHelper.getAllBankCards();
        List<CredentialItem> logins = dbHelper.getAllLogins();

        refreshCardCarousel(cards);
        refreshIdsCarousel(ids);
        refreshRecentLogins(logins);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}
