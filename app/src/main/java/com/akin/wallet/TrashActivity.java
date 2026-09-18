package com.akin.wallet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.PlatformIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Trash — restorable soft-deletes behind Settings. Deleting a Government ID,
 * Bank Card or Social Account stamps deleted_at (dashboard hides it); this
 * screen groups trashed rows in dashboard-style navy cards with hairline
 * dividers — icon well + title/subtitle + restore and delete icons.
 * Restores clear deleted_at; permanent deletes remove the row (logins also
 * drop their associations).
 */
public class TrashActivity extends AppCompatActivity {

    private AppDatabaseHelper dbHelper;
    private View emptyTrash;
    private View headerIds;
    private View headerCards;
    private View headerSocial;
    private TextView headerIdsCount;
    private TextView headerCardsCount;
    private TextView headerSocialCount;
    private View cardIds;
    private View cardCards;
    private View cardSocial;
    private LinearLayout containerIds;
    private LinearLayout containerCards;
    private LinearLayout containerSocial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        dbHelper = new AppDatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        emptyTrash = findViewById(R.id.empty_trash);
        headerIds = findViewById(R.id.header_trash_ids);
        headerCards = findViewById(R.id.header_trash_cards);
        headerSocial = findViewById(R.id.header_trash_social);
        headerIdsCount = findViewById(R.id.header_ids_count);
        headerCardsCount = findViewById(R.id.header_cards_count);
        headerSocialCount = findViewById(R.id.header_social_count);
        cardIds = findViewById(R.id.card_ids);
        cardCards = findViewById(R.id.card_cards);
        cardSocial = findViewById(R.id.card_social);
        containerIds = findViewById(R.id.container_ids);
        containerCards = findViewById(R.id.container_cards);
        containerSocial = findViewById(R.id.container_social);
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

        renderIds(ids);
        renderCards(cards);
        renderAccounts(accounts);

        boolean allEmpty = ids.isEmpty() && cards.isEmpty() && accounts.isEmpty();
        emptyTrash.setVisibility(allEmpty ? View.VISIBLE : View.GONE);
    }

    private void renderIds(List<IdCardItem> ids) {
        containerIds.removeAllViews();
        boolean has = ids != null && !ids.isEmpty();
        headerIds.setVisibility(has ? View.VISIBLE : View.GONE);
        cardIds.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) {
            return;
        }
        headerIdsCount.setText("• " + ids.size());
        for (int i = 0; i < ids.size(); i++) {
            IdCardItem item = ids.get(i);
            boolean isLast = i == ids.size() - 1;
            containerIds.addView(buildRow(
                    R.drawable.ic_person,
                    item.getIdType(),
                    idSubtitle(item),
                    isLast,
                    () -> {
                        dbHelper.restoreIdCard(item.getId());
                        loadTrash();
                        Toast.makeText(this, R.string.trash_restored, Toast.LENGTH_SHORT).show();
                    },
                    () -> confirmPermanentDelete(
                            "Delete this " + item.getIdType() + " forever?",
                            () -> {
                                dbHelper.deleteIdCard(item.getId());
                                loadTrash();
                                Toast.makeText(this,
                                        R.string.trash_deleted_forever, Toast.LENGTH_SHORT).show();
                            })));
        }
    }

    private void renderCards(List<BankCardItem> cards) {
        containerCards.removeAllViews();
        boolean has = cards != null && !cards.isEmpty();
        headerCards.setVisibility(has ? View.VISIBLE : View.GONE);
        cardCards.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) {
            return;
        }
        headerCardsCount.setText("• " + cards.size());
        for (int i = 0; i < cards.size(); i++) {
            BankCardItem item = cards.get(i);
            boolean isLast = i == cards.size() - 1;
            containerCards.addView(buildRow(
                    R.drawable.ic_lock,
                    cardTitle(item),
                    cardSubtitle(item),
                    isLast,
                    () -> {
                        dbHelper.restoreBankCard(item.getId());
                        loadTrash();
                        Toast.makeText(this, R.string.trash_restored, Toast.LENGTH_SHORT).show();
                    },
                    () -> confirmPermanentDelete(
                            "Delete this card forever?",
                            () -> {
                                dbHelper.deleteBankCard(item.getId());
                                loadTrash();
                                Toast.makeText(this,
                                        R.string.trash_deleted_forever, Toast.LENGTH_SHORT).show();
                            })));
        }
    }

    private void renderAccounts(List<CredentialItem> accounts) {
        containerSocial.removeAllViews();
        boolean has = accounts != null && !accounts.isEmpty();
        headerSocial.setVisibility(has ? View.VISIBLE : View.GONE);
        cardSocial.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) {
            return;
        }
        headerSocialCount.setText("• " + accounts.size());
        for (int i = 0; i < accounts.size(); i++) {
            CredentialItem item = accounts.get(i);
            boolean isLast = i == accounts.size() - 1;
            int icon = PlatformIcons.iconFor(item.getPlatform(), item.getIconRes());
            containerSocial.addView(buildRow(
                    icon,
                    titleOr(item.getPlatform(), "Social Login"),
                    item.getUsername(),
                    isLast,
                    () -> {
                        dbHelper.restoreLogin(item.getId());
                        loadTrash();
                        Toast.makeText(this, R.string.trash_restored, Toast.LENGTH_SHORT).show();
                    },
                    () -> confirmPermanentDelete(
                            "Delete this " + item.getPlatform() + " account forever?",
                            () -> {
                                dbHelper.deleteLogin(item.getId());
                                loadTrash();
                                Toast.makeText(this,
                                        R.string.trash_deleted_forever, Toast.LENGTH_SHORT).show();
                            })));
        }
    }

    private void confirmPermanentDelete(String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.trash_delete_forever)
                .setMessage(message + " This cannot be undone.")
                .setPositiveButton(R.string.trash_delete_forever, (d, which) -> onConfirm.run())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * One trash row in dashboard language: 42dp white icon well, bold
     * single-line title + muted single-line subtitle, compact Restore pill and
     * 36dp delete icon target, with a hairline divider below (hidden on last).
     */
    private View buildRow(int iconRes, String title, String subtitle, boolean isLast,
                          Runnable onRestore, Runnable onDelete) {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int hPad = dp(8);
        row.setPadding(hPad, dp(10), hPad, dp(10));
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(42), dp(42));
        icon.setLayoutParams(iconParams);
        icon.setBackgroundResource(R.drawable.bg_dashboard_icon_white);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        try {
            icon.setImageResource(iconRes);
        } catch (Exception e) {
            icon.setImageResource(R.drawable.ic_social);
        }
        icon.setContentDescription(title);
        row.addView(icon);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textsParams.setMarginStart(dp(12));
        texts.setLayoutParams(textsParams);

        TextView titleView = new TextView(this);
        titleView.setText(titleOr(title, "—"));
        titleView.setTextColor(getColor(R.color.text_primary));
        titleView.setTextSize(14);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(titleView);

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView subView = new TextView(this);
            subView.setText(subtitle.trim());
            subView.setTextColor(getColor(R.color.dashboard_muted));
            subView.setTextSize(12);
            subView.setSingleLine(true);
            subView.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            subParams.topMargin = dp(2);
            subView.setLayoutParams(subParams);
            texts.addView(subView);
        }
        row.addView(texts);

        // Twin 36dp icon targets — segmented circular-arrow restore beside
        // the neutral delete icon. Icon-only keeps every row one line tall
        // so the list stays scannable.
        FrameLayout btnRestore = new FrameLayout(this);
        LinearLayout.LayoutParams restoreWrapper =
                new LinearLayout.LayoutParams(dp(36), dp(36));
        restoreWrapper.setMarginStart(dp(4));
        btnRestore.setLayoutParams(restoreWrapper);
        btnRestore.setClickable(true);
        btnRestore.setFocusable(true);
        ImageView restoreIcon = new ImageView(this);
        FrameLayout.LayoutParams restoreIconParams =
                new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER);
        restoreIcon.setLayoutParams(restoreIconParams);
        restoreIcon.setImageResource(R.drawable.restore);
        restoreIcon.setContentDescription(getString(R.string.trash_restore));
        btnRestore.addView(restoreIcon);
        btnRestore.setOnClickListener(v -> onRestore.run());
        row.addView(btnRestore);

        // 36dp delete target with an 18dp red icon — easy to hit, clearly
        // destructive next to the blue restore mark.
        FrameLayout btnDelete = new FrameLayout(this);
        LinearLayout.LayoutParams deleteWrapper =
                new LinearLayout.LayoutParams(dp(36), dp(36));
        deleteWrapper.setMarginStart(dp(0));
        btnDelete.setLayoutParams(deleteWrapper);
        btnDelete.setClickable(true);
        btnDelete.setFocusable(true);
        ImageView deleteIcon = new ImageView(this);
        FrameLayout.LayoutParams deleteIconParams =
                new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER);
        deleteIcon.setLayoutParams(deleteIconParams);
        deleteIcon.setImageResource(R.drawable.ic_delete);
        deleteIcon.setContentDescription(getString(R.string.trash_delete_forever));
        btnDelete.addView(deleteIcon);
        btnDelete.setOnClickListener(v -> onDelete.run());
        row.addView(btnDelete);

        outer.addView(row);

        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        dividerParams.setMargins(dp(8), 0, dp(8), 0);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(getColor(R.color.dashboard_surface_border));
        divider.setVisibility(isLast ? View.GONE : View.VISIBLE);
        outer.addView(divider);

        return outer;
    }

    private static String titleOr(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
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

    private static String cardSubtitle(BankCardItem item) {
        String holder = item.getHolderName() != null ? item.getHolderName().trim() : "";
        String number = item.getCardNumber() != null ? item.getCardNumber().replaceAll("\\D", "") : "";
        String last4 = number.length() > 4 ? number.substring(number.length() - 4) : number;
        if (!holder.isEmpty() && !last4.isEmpty()) {
            return holder + " •••• " + last4;
        }
        if (!holder.isEmpty()) {
            return holder;
        }
        if (!last4.isEmpty()) {
            return "•••• " + last4;
        }
        return "";
    }

    private static String idSubtitle(IdCardItem item) {
        Map<String, String> fields = item.getFields();
        StringBuilder sb = new StringBuilder();
        for (String value : fields.values()) {
            if (value != null && !value.trim().isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" • ");
                }
                sb.append(value.trim());
                if (sb.length() > 42) {
                    break;
                }
            }
        }
        return sb.toString();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}
