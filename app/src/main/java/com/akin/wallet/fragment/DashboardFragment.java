package com.akin.wallet.fragment;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

    private TextView txtIdsCount;
    private TextView txtCardsCount;
    private TextView txtLoginsCount;

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

        txtIdsCount = view.findViewById(R.id.txt_ids_count);
        txtCardsCount = view.findViewById(R.id.txt_cards_count);
        txtLoginsCount = view.findViewById(R.id.txt_logins_count);

        setupCardCarousel(view);
        setupIdsCarousel(view);
        setupRecentLogins(view);

        view.findViewById(R.id.card_ids).setOnClickListener(v -> goTo(R.id.nav_id));
        view.findViewById(R.id.card_bank).setOnClickListener(v -> goTo(R.id.nav_bank));
        view.findViewById(R.id.card_social).setOnClickListener(v -> goTo(R.id.nav_login));

        view.findViewById(R.id.btn_view_all_cards).setOnClickListener(v -> goTo(R.id.nav_bank));
        view.findViewById(R.id.btn_view_all_ids).setOnClickListener(v -> goTo(R.id.nav_id));
        view.findViewById(R.id.btn_view_all_logins).setOnClickListener(v -> goTo(R.id.nav_login));

        view.findViewById(R.id.banner_privacy).setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Offline-first: your data never leaves this device",
                        Toast.LENGTH_SHORT).show());

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

    /** Horizontal snap carousel rendering the user's real bank cards. */
    private void setupCardCarousel(@NonNull View view) {
        recyclerCarousel = view.findViewById(R.id.recycler_cards_carousel);
        emptyCards = view.findViewById(R.id.empty_cards);

        cardAdapter = new DashboardCardAdapter(item -> goTo(R.id.nav_bank));
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
            emptyCards.setOnClickListener(v -> goTo(R.id.nav_bank));
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

        idAdapter = new DashboardIdCardAdapter(item -> goTo(R.id.nav_id));
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
            emptyIds.setOnClickListener(v -> goTo(R.id.nav_id));
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
        if (dbHelper == null || txtIdsCount == null) {
            return;
        }
        List<IdCardItem> ids = dbHelper.getAllIdCards();
        List<BankCardItem> cards = dbHelper.getAllBankCards();
        List<CredentialItem> logins = dbHelper.getAllLogins();

        String idsText = ids.size() + (ids.size() == 1 ? " item" : " items");
        String cardsText = cards.size() + (cards.size() == 1 ? " item" : " items");
        String loginsText = logins.size() + (logins.size() == 1 ? " item" : " items");

        txtIdsCount.setText(idsText);
        txtCardsCount.setText(cardsText);
        txtLoginsCount.setText(loginsText);

        refreshCardCarousel(cards);
        refreshIdsCarousel(ids);
        refreshRecentLogins(logins);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}
