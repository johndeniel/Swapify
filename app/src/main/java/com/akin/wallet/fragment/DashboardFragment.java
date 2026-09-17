package com.akin.wallet.fragment;

import android.content.Intent;
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

import com.akin.wallet.R;
import com.akin.wallet.adapter.DashboardCardAdapter;
import com.akin.wallet.adapter.DashboardIdCardAdapter;
import com.akin.wallet.adapter.DashboardSocialAccountAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.BankCardFormActivity;
import com.akin.wallet.GovermentIDFormActivity;
import com.akin.wallet.SocialAccountActivity;
import com.akin.wallet.SocialAccountFormActivity;
import com.akin.wallet.model.IdCardItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

/**
 * Dashboard (Home) — overhaul screen matching the MyKey reference.
 * Shows live item counts from SQLite; Social Account lists only the
 * user's created social accounts.
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
        setupSocialAccounts(view);
        setupAddMenu(view);

        view.findViewById(R.id.btn_view_all_social).setOnClickListener(v -> goTo(R.id.nav_social_account));

        refreshDashboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboard();
    }

    /** Social Account lives outside the single-screen host: open it directly. */
    private void goTo(int navId) {
        if (navId == R.id.nav_social_account) {
            startActivity(new Intent(requireContext(), SocialAccountActivity.class));
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
        view.findViewById(R.id.fab_option_login).setOnClickListener(v -> addNew(R.id.nav_social_account));
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
        Intent edit = new Intent(requireContext(), SocialAccountFormActivity.class);
        edit.putExtra(SocialAccountFormActivity.EXTRA_LOGIN_ID, (long) item.getId());
        edit.putExtra(SocialAccountFormActivity.EXTRA_PLATFORM, item.getPlatform());
        edit.putExtra(SocialAccountFormActivity.EXTRA_USERNAME, item.getUsername());
        edit.putExtra(SocialAccountFormActivity.EXTRA_PASSWORD, item.getPassword());
        edit.putExtra(SocialAccountFormActivity.EXTRA_PIN, item.getPin());
        edit.putExtra(SocialAccountFormActivity.EXTRA_ICON_RES, item.getIconRes());
        startActivity(edit);
    }

    /** Opens the ID creation form directly (IDs tab is gone; FAB is the only entry). */
    private void openIdCreator() {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        startActivity(new Intent(requireContext(), GovermentIDFormActivity.class));
    }

    /** Opens an ID edit form directly (IDs tab is gone; dashboard is the editor). */
    private void openIdEditor(IdCardItem item) {
        Intent edit = new Intent(requireContext(), GovermentIDFormActivity.class);
        edit.putExtra(GovermentIDFormActivity.EXTRA_ID, item.getId());
        edit.putExtra(GovermentIDFormActivity.EXTRA_TYPE, item.getIdType());
        edit.putExtra(GovermentIDFormActivity.EXTRA_FIELDS_JSON, item.getFieldsJson());
        edit.putExtra(GovermentIDFormActivity.EXTRA_DESIGN, item.getDesign());
        startActivity(edit);
    }

    /** Opens the bank creation form directly (bank tab is gone; FAB is the only entry). */
    private void openBankCreator() {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        startActivity(new Intent(requireContext(), BankCardFormActivity.class));
    }

    /** Opens a bank edit form directly (bank tab is gone; dashboard is the editor). */
    private void openBankEditor(BankCardItem item) {
        Intent edit = new Intent(requireContext(), BankCardFormActivity.class);
        edit.putExtra(BankCardFormActivity.EXTRA_ID, item.getId());
        edit.putExtra(BankCardFormActivity.EXTRA_TYPE, item.getCardType());
        edit.putExtra(BankCardFormActivity.EXTRA_NETWORK, item.getCardNetwork());
        edit.putExtra(BankCardFormActivity.EXTRA_BANK, item.getBankName());
        edit.putExtra(BankCardFormActivity.EXTRA_HOLDER, item.getHolderName());
        edit.putExtra(BankCardFormActivity.EXTRA_NUMBER, item.getCardNumber());
        edit.putExtra(BankCardFormActivity.EXTRA_EXPIRY, item.getExpiry());
        edit.putExtra(BankCardFormActivity.EXTRA_CVV, item.getCvv());
        edit.putExtra(BankCardFormActivity.EXTRA_PIN, item.getPin());
        edit.putExtra(BankCardFormActivity.EXTRA_DESIGN, item.getDesign());
        edit.putExtra(BankCardFormActivity.EXTRA_CREATED_AT, item.getCreatedAt());
        edit.putExtra(BankCardFormActivity.EXTRA_UPDATED_AT, item.getUpdatedAt());
        startActivity(edit);
    }

    /**
     * Opens the login creation form directly (full screen, no navigation).
     * (Bank/ID creation moved to openBankCreator/openIdCreator.)
     */
    private void addNew(int navId) {
        if (isFabMenuOpen) {
            toggleAddMenu();
        }
        if (navId == R.id.nav_social_account) {
            startActivity(new Intent(requireContext(), SocialAccountFormActivity.class));
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
            if (!hasIds) {
                matchEmptyHeightToCards(recyclerIdsCarousel, emptyIds);
            }
        }
    }

    /** Social Account — vertical list of created social accounts only. */
    private void setupSocialAccounts(@NonNull View view) {
        cardSocialAccounts = view.findViewById(R.id.card_social_accounts);
        recyclerSocialAccounts = view.findViewById(R.id.recycler_social_accounts);
        emptySocialAccounts = view.findViewById(R.id.empty_social_accounts);

        // Row taps intentionally do nothing for now (edit flow to be decided later).
        socialAdapter = new DashboardSocialAccountAdapter(item -> { });
        recyclerSocialAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
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
