package com.akin.wallet.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.LinkedAccountAdapter;
import com.akin.wallet.adapter.PlatformSelectionAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.PlatformIcons;
import com.akin.wallet.util.Dialogs;
import com.akin.wallet.util.Ui;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.search.SearchView;

import java.util.ArrayList;
import java.util.List;

/**
 * Social login creation/edit form as a full screen. Add mode when no login
 * id is passed; edit mode otherwise. The platform and link pickers open as
 * full-screen activities. Callers refresh in onResume; RESULT_OK is set on
 * successful save.
 */
public class SocialAccountFormActivity extends AppCompatActivity {

    public static final String EXTRA_LOGIN_ID = "extra_login_id";
    public static final String EXTRA_PLATFORM = "extra_platform";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_PASSWORD = "extra_password";
    public static final String EXTRA_PIN = "extra_pin";
    public static final String EXTRA_MOBILE = "extra_mobile";
    public static final String EXTRA_ICON_RES = "extra_icon_res";
    public static final String EXTRA_CREATED_AT = "extra_created_at";
    public static final String EXTRA_UPDATED_AT = "extra_updated_at";

    /** Default pick for a fresh form (also the icon fallback for stored rows). */
    public static final String DEFAULT_PLATFORM_NAME = "Google";

    /**
     * Intent that opens this form to edit an existing credential. Single
     * packing site so all callers carry the same extras (timestamps ride
     * top-level, never inside another field).
     */
    public static Intent editIntent(@NonNull Context context,
                                    @NonNull CredentialItem item) {
        Intent edit = new Intent(context, SocialAccountFormActivity.class);
        edit.putExtra(EXTRA_LOGIN_ID, (long) item.getId());
        edit.putExtra(EXTRA_PLATFORM, item.getPlatform());
        edit.putExtra(EXTRA_USERNAME, item.getUsername());
        edit.putExtra(EXTRA_PASSWORD, item.getPassword());
        edit.putExtra(EXTRA_PIN, item.getPin());
        edit.putExtra(EXTRA_MOBILE, item.getMobile());
        edit.putExtra(EXTRA_ICON_RES, item.getIconRes());
        edit.putExtra(EXTRA_CREATED_AT, item.getCreatedAt());
        edit.putExtra(EXTRA_UPDATED_AT, item.getUpdatedAt());
        return edit;
    }

    private AppDatabaseHelper dbHelper;
    private int selectedIcon = PlatformIcons.iconFor(DEFAULT_PLATFORM_NAME);
    private String selectedName = DEFAULT_PLATFORM_NAME;
    private ImageView platformIcon;
    private TextView platformName;
    private LinearLayout associateSection;
    private RecyclerView recyclerLinked;
    private TextView emptyLinked;
    private List<CredentialItem> linkPool = new ArrayList<>();
    private List<CredentialItem> linkedItems = new ArrayList<>();
    private LinkedAccountAdapter linkedAdapter;
    private int linkSelfId = -1;

    // In-place M3 platform picker: full-screen SearchView reusing the old
    // picker adapter. Tapping the platform card opens it; tapping a row
    // picks the platform and closes it (no separate activity).
    private static final String KEY_PLATFORM_QUERY = "platform_search_query";
    private static final String KEY_PLATFORM_OPEN = "platform_search_open";
    private SearchView platformSearchView;
    private PlatformSelectionAdapter platformAdapter;
    private RecyclerView recyclerPlatformSearch;
    private View emptyPlatformResults;
    private String platformQuery = "";
    private boolean platformSearchOpen = false;

    // In-place M3 link picker: full-screen SearchView reusing the linked
    // account adapter in pick mode. The Add action opens it with the
    // currently-linkable accounts; tapping a row links it and closes it
    // (no separate activity).
    private static final String KEY_LINK_QUERY = "link_search_query";
    private static final String KEY_LINK_OPEN = "link_search_open";
    private SearchView linkSearchView;
    private LinkedAccountAdapter linkSearchAdapter;
    private RecyclerView recyclerLinkSearch;
    private View emptyLinkResults;
    private String linkQuery = "";
    private boolean linkSearchOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_account_form);

        dbHelper = new AppDatabaseHelper(this);

        // Back chevron, same as the bank and government ID forms.
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        setupPlatformSearch(savedInstanceState);

        long id = getIntent().getLongExtra(EXTRA_LOGIN_ID, -1);
        if (id == -1) {
            bindAddForm();
        } else {
            CredentialItem item = new CredentialItem(
                    (int) id,
                    getIntent().getStringExtra(EXTRA_PLATFORM),
                    getIntent().getStringExtra(EXTRA_USERNAME),
                    getIntent().getStringExtra(EXTRA_PASSWORD),
                    getIntent().getStringExtra(EXTRA_PIN),
                    getIntent().getIntExtra(EXTRA_ICON_RES,
                            PlatformIcons.iconFor(DEFAULT_PLATFORM_NAME)),
                    getIntent().getStringExtra(EXTRA_MOBILE),
                    getIntent().getLongExtra(EXTRA_CREATED_AT, 0),
                    getIntent().getLongExtra(EXTRA_UPDATED_AT, 0));
            bindEditForm(item);
        }
        // Link pool is ready only after the bind above ran.
        setupLinkSearch(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_PLATFORM_QUERY, platformQuery);
        outState.putBoolean(KEY_PLATFORM_OPEN, platformSearchOpen);
        outState.putString(KEY_LINK_QUERY, linkQuery);
        outState.putBoolean(KEY_LINK_OPEN, linkSearchOpen);
    }

    /**
     * In-place platform picker: the shared catalog adapter rebound inside the
     * form's SearchView. Row taps apply the platform directly (same fields
     * the old picker activity returned); back closes search first.
     */
    private void setupPlatformSearch(Bundle savedInstanceState) {
        platformSearchView = findViewById(R.id.platform_search_view);
        if (platformSearchView == null) {
            return;
        }
        recyclerPlatformSearch = findViewById(R.id.recycler_platform_search);
        emptyPlatformResults = findViewById(R.id.empty_platform_results);

        platformAdapter = new PlatformSelectionAdapter(PlatformIcons.catalog(),
                (iconRes, name, url) -> {
                    selectedIcon = iconRes;
                    selectedName = name;
                    if (platformIcon != null) {
                        platformIcon.setImageResource(selectedIcon);
                    }
                    if (platformName != null) {
                        platformName.setText(selectedName);
                    }
                    platformSearchView.hide();
                });
        recyclerPlatformSearch.setLayoutManager(new LinearLayoutManager(this));
        recyclerPlatformSearch.setAdapter(platformAdapter);
        // Empty card mirrors the filter count (register before restoring).
        platformAdapter.setOnCountChangedListener(count -> {
            boolean empty = count == 0;
            if (emptyPlatformResults != null) {
                emptyPlatformResults.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            if (recyclerPlatformSearch != null) {
                recyclerPlatformSearch.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        });

        platformSearchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                platformQuery = s != null ? s.toString() : "";
                platformAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        platformSearchView.addTransitionListener((view, oldState, newState) -> {
            platformSearchOpen = newState == SearchView.TransitionState.SHOWN;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (linkSearchOpen && linkSearchView != null) {
                    linkSearchView.hide();
                } else if (platformSearchOpen && platformSearchView != null) {
                    platformSearchView.hide();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (savedInstanceState != null) {
            platformQuery = savedInstanceState.getString(KEY_PLATFORM_QUERY, "");
            if (savedInstanceState.getBoolean(KEY_PLATFORM_OPEN, false)) {
                if (!platformQuery.isEmpty()) {
                    platformSearchView.getEditText().setText(platformQuery);
                }
                platformSearchView.post(() -> platformSearchView.show());
            }
        }
    }

    /** Opens the in-place platform SearchView (replaces the picker activity). */
    private void openPlatformSearch() {
        if (platformSearchView != null) {
            platformSearchView.show();
        }
    }

    /**
     * In-place link picker: rebuilt from the live pool on every open so it
     * can never offer the edited account itself or an already-linked one
     * (same exclusion the old picker activity received as ID extras).
     */
    private void setupLinkSearch(Bundle savedInstanceState) {
        linkSearchView = findViewById(R.id.link_search_view);
        if (linkSearchView == null) {
            return;
        }
        recyclerLinkSearch = findViewById(R.id.recycler_link_search);
        emptyLinkResults = findViewById(R.id.empty_link_results);
        recyclerLinkSearch.setLayoutManager(new LinearLayoutManager(this));

        linkSearchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                linkQuery = s != null ? s.toString() : "";
                if (linkSearchAdapter != null) {
                    linkSearchAdapter.getFilter().filter(s);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        linkSearchView.addTransitionListener((view, oldState, newState) -> {
            linkSearchOpen = newState == SearchView.TransitionState.SHOWN;
        });

        if (savedInstanceState != null
                && savedInstanceState.getBoolean(KEY_LINK_OPEN, false)) {
            linkQuery = savedInstanceState.getString(KEY_LINK_QUERY, "");
            openLinkSearch();
            if (!linkQuery.isEmpty()) {
                linkSearchView.getEditText().setText(linkQuery);
            }
        }
    }

    /** Opens the in-place link SearchView (replaces the picker activity). */
    private void openLinkSearch() {
        if (linkSearchView == null || recyclerLinkSearch == null) {
            return;
        }
        List<CredentialItem> available = new ArrayList<>();
        for (CredentialItem existing : linkPool) {
            if (existing.getId() == linkSelfId) {
                continue;
            }
            boolean alreadyLinked = false;
            for (CredentialItem linked : linkedItems) {
                if (linked.getId() == existing.getId()) {
                    alreadyLinked = true;
                    break;
                }
            }
            if (!alreadyLinked) {
                available.add(existing);
            }
        }

        if (available.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content),
                    "All accounts already linked", Snackbar.LENGTH_SHORT).show();
            return;
        }

        linkSearchAdapter = new LinkedAccountAdapter(available, false, (picked, isRemove) -> {
            boolean dup = false;
            for (CredentialItem l : linkedItems) {
                if (l.getId() == picked.getId()) {
                    dup = true;
                    break;
                }
            }
            if (!dup) {
                linkedItems.add(picked);
                if (linkedAdapter != null) {
                    linkedAdapter.onExternalAdd(picked);
                    linkedAdapter.notifyItemInserted(linkedItems.size() - 1);
                }
                refreshLinkedVisibility();
            }
            linkSearchView.hide();
        });
        // No [+] icon in search: tapping the row itself links (platform style).
        linkSearchAdapter.setShowPickAction(false);
        linkSearchAdapter.setOnCountChangedListener(count -> {
            boolean empty = count == 0;
            if (emptyLinkResults != null) {
                emptyLinkResults.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            recyclerLinkSearch.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        recyclerLinkSearch.setAdapter(linkSearchAdapter);
        // Fresh adapter holds the full list: reset any stale empty state and
        // query left over from the previous open before showing.
        if (emptyLinkResults != null) {
            emptyLinkResults.setVisibility(View.GONE);
        }
        recyclerLinkSearch.setVisibility(View.VISIBLE);
        linkSearchView.getEditText().setText("");
        linkSearchView.show();
    }

    private void bindAddForm() {
        selectedIcon = PlatformIcons.iconFor(DEFAULT_PLATFORM_NAME);
        selectedName = DEFAULT_PLATFORM_NAME;
        final Context context = this;

        platformIcon = findViewById(R.id.platform_icon);
        platformName = findViewById(R.id.platform_name);

        platformIcon.setImageResource(selectedIcon);
        platformName.setText(selectedName);

        findViewById(R.id.platform_selector).setOnClickListener(v -> openPlatformSearch());

        EditText inputPassword = findViewById(R.id.input_password);
        findViewById(R.id.btn_toggle_password).setOnClickListener(v ->
                Ui.togglePasswordVisibility(inputPassword));

        EditText inputPin = findViewById(R.id.input_pin);
        findViewById(R.id.btn_toggle_pin).setOnClickListener(v ->
                Ui.togglePasswordVisibility(inputPin));

        setupAssociateSection(dbHelper.getAllLogins(), -1);

        // Add mode keeps a single full-width Save button, same as the bank
        // form: the delete view is GONE, so its row margin is dropped.
        View btnSaveAdd = findViewById(R.id.btn_save);
        LinearLayout.LayoutParams saveParams =
                (LinearLayout.LayoutParams) btnSaveAdd.getLayoutParams();
        saveParams.setMarginEnd(0);
        btnSaveAdd.setLayoutParams(saveParams);

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            EditText inputUsername = findViewById(R.id.input_username);
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String pin = inputPin.getText().toString().trim();
            EditText inputMobile = findViewById(R.id.input_mobile);
            String mobile = inputMobile.getText().toString().trim();

            if (username.isEmpty()) {
                inputUsername.setError(getString(R.string.err_username_required));
                return;
            }

                CredentialItem newitem = new CredentialItem(selectedName, username, password, pin,
                        selectedIcon, mobile, 0, 0);
                long newId = dbHelper.insertLogin(newitem);

                for (CredentialItem linked : linkedItems) {
                    dbHelper.insertAssociation(newId, linked.getId());
                }

            setResult(RESULT_OK);
            finish();
            Ui.notifyOnReturn(R.string.msg_account_saved);
        });

    }


    /** Shared associate-accounts section for add (selfId -1) and edit modes. */
    private void setupAssociateSection(List<CredentialItem> existingAccounts, int selfId) {
        linkPool = existingAccounts;
        linkSelfId = selfId;
        linkedItems = new ArrayList<>();
        associateSection = findViewById(R.id.associate_section);
        recyclerLinked = findViewById(R.id.recycler_linked);
        emptyLinked = findViewById(R.id.empty_linked);

        if (selfId != -1) {
            List<Integer> currentAssocIds = dbHelper.getAssociations(selfId);
            for (CredentialItem login : linkPool) {
                for (int assocId : currentAssocIds) {
                    if (login.getId() == assocId) {
                        linkedItems.add(login);
                        break;
                    }
                }
            }
        }

        // The section always stays on screen: an empty link set shows the
        // muted empty line inside the card (Add stays reachable) instead of
        // the whole section vanishing.
        associateSection.setVisibility(View.VISIBLE);

        linkedAdapter = new LinkedAccountAdapter(linkedItems, true,
                (linkedItem, isRemove) -> refreshLinkedVisibility());
        recyclerLinked.setLayoutManager(new LinearLayoutManager(this));
        recyclerLinked.setAdapter(linkedAdapter);
        refreshLinkedVisibility();

        findViewById(R.id.btn_add_associate).setOnClickListener(v -> openLinkSearch());
    }

    /**
     * Empty-state toggle for the associate card: muted line when nothing is
     * linked, rows otherwise. The section header (and its Add pill) stays
     * visible in both states.
     */
    private void refreshLinkedVisibility() {
        boolean empty = linkedItems == null || linkedItems.isEmpty();
        if (emptyLinked != null) {
            emptyLinked.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (recyclerLinked != null) {
            recyclerLinked.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    private void bindEditForm(@NonNull CredentialItem item) {
        final Context context = this;

        platformIcon = findViewById(R.id.platform_icon);
        platformName = findViewById(R.id.platform_name);
        TextView textSaveLabel = findViewById(R.id.text_save_label);
        EditText inputUsername = findViewById(R.id.input_username);
        EditText inputPassword = findViewById(R.id.input_password);
        EditText inputPin = findViewById(R.id.input_pin);
        EditText inputMobile = findViewById(R.id.input_mobile);

        textSaveLabel.setText("Update");

        selectedIcon = PlatformIcons.iconFor(item.getPlatform(), item.getIconRes());
        selectedName = item.getPlatform();
        platformIcon.setImageResource(selectedIcon);
        platformName.setText(item.getPlatform());
        inputUsername.setText(item.getUsername());
        inputPassword.setText(item.getPassword());
        inputPin.setText(item.getPin());
        inputMobile.setText(item.getMobile());

        findViewById(R.id.platform_selector).setOnClickListener(v -> openPlatformSearch());

        findViewById(R.id.btn_toggle_password).setOnClickListener(v ->
                Ui.togglePasswordVisibility(inputPassword));

        findViewById(R.id.btn_toggle_pin).setOnClickListener(v ->
                Ui.togglePasswordVisibility(inputPin));

        setupAssociateSection(dbHelper.getAllLogins(), item.getId());

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String pin = inputPin.getText().toString().trim();
            String mobile = inputMobile.getText().toString().trim();

            if (username.isEmpty()) {
                inputUsername.setError(getString(R.string.err_username_required));
                return;
            }

            dbHelper.deleteLogin(item.getId());

            // Edit is delete+reinsert (new row id): carry createdAt across so
            // history survives; updatedAt=0 tells insert to stamp now.
            CredentialItem updated = new CredentialItem(selectedName, username, password, pin,
                    selectedIcon, mobile, item.getCreatedAt(), 0);
            long newId = dbHelper.insertLogin(updated);

            for (CredentialItem linked : linkedItems) {
                dbHelper.insertAssociation(newId, linked.getId());
            }

            setResult(RESULT_OK);
            finish();
            Ui.notifyOnReturn(R.string.msg_updated);
        });

        // Delete in edit mode (the standalone list screen is gone), same
        // in-row outline-red pattern as the bank and government ID forms.
        // Soft-delete behind the scenes: the row moves to Trash (Settings)
        // but the dialog reads as a normal delete. deleteLogin stays for
        // edit reinsert + Trash permanent delete only.
        View btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setOnClickListener(v -> Dialogs.confirmDelete(SocialAccountFormActivity.this,
                "Delete Account",
                "Are you sure you want to delete this "
                        + item.getPlatform() + " account?",
                () -> {
                    dbHelper.moveLoginToTrash(item.getId());
                    setResult(RESULT_OK);
                    finish();
                    Ui.notifyOnReturn(R.string.msg_deleted);
                }));

    }
}
