package com.akin.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.LinkedAccountAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.PlatformIcons;

import java.util.ArrayList;
import java.util.List;

/**
 * Social login creation/edit form as a full screen (replaces the old bottom
 * sheets). Add mode when no login id is passed; edit mode otherwise. The
 * platform and link pickers stay as nested bottom sheets. Callers refresh in
 * onResume; RESULT_OK is set on successful save.
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

    /** Default pick for a fresh form (also the icon fallback for legacy rows). */
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

    private final ActivityResultLauncher<Intent> platformPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Intent data = result.getData();
                selectedIcon = data.getIntExtra(
                        PlatformPickerActivity.EXTRA_ICON_RES, selectedIcon);
                String name = data.getStringExtra(PlatformPickerActivity.EXTRA_NAME);
                if (name != null) {
                    selectedName = name;
                }
                if (platformIcon != null) {
                    platformIcon.setImageResource(selectedIcon);
                }
                if (platformName != null) {
                    platformName.setText(selectedName);
                }
            });

    private final ActivityResultLauncher<Intent> linkPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                int pickedId = result.getData().getIntExtra(
                        LinkAccountPickerActivity.EXTRA_ACCOUNT_ID, -1);
                for (CredentialItem c : linkPool) {
                    if (c.getId() == pickedId) {
                        boolean dup = false;
                        for (CredentialItem l : linkedItems) {
                            if (l.getId() == pickedId) {
                                dup = true;
                                break;
                            }
                        }
                        if (!dup) {
                            linkedItems.add(c);
                            if (linkedAdapter != null) {
                                linkedAdapter.onExternalAdd(c);
                                linkedAdapter.notifyItemInserted(linkedItems.size() - 1);
                            }
                            refreshLinkedVisibility();
                        }
                        break;
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_account_form);

        dbHelper = new AppDatabaseHelper(this);

        // Back chevron, same as the bank and government ID forms.
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

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
    }

    private void bindAddForm() {
        selectedIcon = PlatformIcons.iconFor(DEFAULT_PLATFORM_NAME);
        selectedName = DEFAULT_PLATFORM_NAME;
        final Context context = this;

        platformIcon = findViewById(R.id.platform_icon);
        platformName = findViewById(R.id.platform_name);

        platformIcon.setImageResource(selectedIcon);
        platformName.setText(selectedName);

        findViewById(R.id.platform_selector).setOnClickListener(v ->
                platformPickerLauncher.launch(new Intent(this, PlatformPickerActivity.class)));

        EditText inputPassword = findViewById(R.id.input_password);
        findViewById(R.id.btn_toggle_password).setOnClickListener(v -> {
            if (inputPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        EditText inputPin = findViewById(R.id.input_pin);
        findViewById(R.id.btn_toggle_pin).setOnClickListener(v -> {
            if (inputPin.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPin.setSelection(inputPin.getText().length());
        });

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
                inputUsername.setError("Username required");
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
            Toast.makeText(context, "Account Saved", Toast.LENGTH_SHORT).show();
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

        findViewById(R.id.btn_add_associate).setOnClickListener(v -> {
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
                Toast.makeText(this, "All accounts already linked", Toast.LENGTH_SHORT).show();
                return;
            }

            int[] ids = new int[available.size()];
            for (int i = 0; i < available.size(); i++) {
                ids[i] = available.get(i).getId();
            }
            Intent link = new Intent(this, LinkAccountPickerActivity.class);
            link.putExtra(LinkAccountPickerActivity.EXTRA_AVAILABLE_IDS, ids);
            linkPickerLauncher.launch(link);
        });
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

        findViewById(R.id.platform_selector).setOnClickListener(v ->
                platformPickerLauncher.launch(new Intent(this, PlatformPickerActivity.class)));

        findViewById(R.id.btn_toggle_password).setOnClickListener(v -> {
            if (inputPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        findViewById(R.id.btn_toggle_pin).setOnClickListener(v -> {
            if (inputPin.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPin.setSelection(inputPin.getText().length());
        });

        setupAssociateSection(dbHelper.getAllLogins(), item.getId());

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String pin = inputPin.getText().toString().trim();
            String mobile = inputMobile.getText().toString().trim();

            if (username.isEmpty()) {
                inputUsername.setError("Username required");
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
            Toast.makeText(context, "Updated Successfully", Toast.LENGTH_SHORT).show();
        });

        // Delete in edit mode (the standalone list screen is gone), same
        // in-row outline-red pattern as the bank and government ID forms.
        // Soft-delete behind the scenes: the row moves to Trash (Settings)
        // but the dialog reads as a normal delete. deleteLogin stays for
        // edit reinsert + Trash permanent delete only.
        View btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(SocialAccountFormActivity.this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete this "
                        + item.getPlatform() + " account?")
                .setPositiveButton("Delete", (d, which) -> {
                    dbHelper.moveLoginToTrash(item.getId());
                    setResult(RESULT_OK);
                    finish();
                    Toast.makeText(SocialAccountFormActivity.this,
                            "Deleted Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show());

    }
}
