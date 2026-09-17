package com.akin.wallet;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.LinkedAccountAdapter;
import com.akin.wallet.adapter.PlatformSelectionAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.PlatformOption;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Social login creation/edit form as a full screen (replaces the old bottom
 * sheets). Add mode when no login id is passed; edit mode otherwise. The
 * platform and link pickers stay as nested bottom sheets. Callers refresh in
 * onResume; RESULT_OK is set on successful save.
 */
public class SocialLoginFormActivity extends AppCompatActivity {

    public static final String EXTRA_LOGIN_ID = "extra_login_id";
    public static final String EXTRA_PLATFORM = "extra_platform";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_PASSWORD = "extra_password";
    public static final String EXTRA_PIN = "extra_pin";
    public static final String EXTRA_ICON_RES = "extra_icon_res";

    private AppDatabaseHelper dbHelper;
    private int selectedIcon = R.drawable.google;
    private String selectedName = "Google";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_login_form);

        dbHelper = new AppDatabaseHelper(this);

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
                    getIntent().getIntExtra(EXTRA_ICON_RES, R.drawable.google));
            bindEditForm(item);
        }
    }

    private void bindAddForm() {
        selectedIcon = R.drawable.google;
        selectedName = "Google";
        final Context context = this;


        ImageView platformIcon = findViewById(R.id.platform_icon);
        TextView platformName = findViewById(R.id.platform_name);

        platformIcon.setImageResource(selectedIcon);
        platformName.setText(selectedName);

        findViewById(R.id.platform_selector).setOnClickListener(v -> {
            BottomSheetDialog pickerDialog = new BottomSheetDialog(context,
                    com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
            View pickerView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_select_social_platform, null);
            pickerDialog.setContentView(pickerView);

            pickerDialog.setOnShowListener(dialogInterface -> {
                if (pickerDialog.getWindow() != null) {
                    pickerDialog.getWindow().setStatusBarColor(context.getColor(R.color.dark_bg));
                }
                com.google.android.material.bottomsheet.BottomSheetDialog dialog3 =
                        (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
                View bottomSheet = dialog3.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.requestLayout();
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        .setHideable(false);
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        .setDraggable(false);
            });

            RecyclerView recyclerPlatforms = pickerView.findViewById(R.id.recycler_platforms);
            recyclerPlatforms.setLayoutManager(new LinearLayoutManager(context));
            PlatformSelectionAdapter platformAdapter2 = new PlatformSelectionAdapter(getPlatforms(),
                    (iconRes, name, url) -> {
                        selectedIcon = iconRes;
                        selectedName = name;
                        platformIcon.setImageResource(iconRes);
                        platformName.setText(name);
                        pickerDialog.dismiss();
                    });
            recyclerPlatforms.setAdapter(platformAdapter2);

            EditText searchPlatform2 = pickerView.findViewById(R.id.search_platform);
            searchPlatform2.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    platformAdapter2.getFilter().filter(s);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            pickerDialog.show();
        });

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

        List<CredentialItem> existingLogins = dbHelper.getAllLogins();
        LinearLayout associateSection = findViewById(R.id.associate_section);
        RecyclerView recyclerLinked = findViewById(R.id.recycler_linked);
        List<CredentialItem> linkedItems = new ArrayList<>();

        if (existingLogins.isEmpty()) {
            associateSection.setVisibility(View.GONE);
        } else {
            associateSection.setVisibility(View.VISIBLE);

            LinkedAccountAdapter linkedAdapter = new LinkedAccountAdapter(linkedItems, true, (linkedItem, isRemove) -> {
                if (linkedItems.isEmpty()) {
                    associateSection.setVisibility(View.GONE);
                }
            });
            recyclerLinked.setLayoutManager(new LinearLayoutManager(context));
            recyclerLinked.setAdapter(linkedAdapter);

            findViewById(R.id.btn_add_associate).setOnClickListener(v -> {
                List<CredentialItem> available = new ArrayList<>();
                for (CredentialItem existing : existingLogins) {
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
                    Toast.makeText(context, "All accounts already linked", Toast.LENGTH_SHORT).show();
                    return;
                }

                BottomSheetDialog pickerDialog = new BottomSheetDialog(context,
                        com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
                View pickerView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_link_social_account, null);
                pickerDialog.setContentView(pickerView);

                pickerDialog.setOnShowListener(dialogInterface -> {
                    if (pickerDialog.getWindow() != null) {
                        pickerDialog.getWindow().setStatusBarColor(context.getColor(R.color.dark_bg));
                    }
                    com.google.android.material.bottomsheet.BottomSheetDialog d =
                            (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
                    View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    bottomSheet.requestLayout();
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                            .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                            .setHideable(false);
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                            .setDraggable(false);
                });

                RecyclerView recyclerAccounts = pickerView.findViewById(R.id.recycler_accounts);
                recyclerAccounts.setLayoutManager(new LinearLayoutManager(context));
                recyclerAccounts.setAdapter(new LinkedAccountAdapter(available, false, (pickedItem, isRemove) -> {
                    linkedItems.add(pickedItem);
                    linkedAdapter.notifyItemInserted(linkedItems.size() - 1);
                    pickerDialog.dismiss();
                }));

                pickerDialog.show();
            });
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            EditText inputUsername = findViewById(R.id.input_username);
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String pin = inputPin.getText().toString().trim();

            if (username.isEmpty()) {
                inputUsername.setError("Username required");
                return;
            }

                CredentialItem newitem = new CredentialItem(selectedName, username, password, pin, selectedIcon);
                long newId = dbHelper.insertLogin(newitem);

                for (CredentialItem linked : linkedItems) {
                    dbHelper.insertAssociation(newId, linked.getId());
                }

            setResult(RESULT_OK);
            finish();
            Toast.makeText(context, "Account Saved", Toast.LENGTH_SHORT).show();
        });


    }

    private static List<PlatformOption> getPlatforms() {
        List<PlatformOption> platforms = new ArrayList<>();
        platforms.add(new PlatformOption(R.drawable.binance, "Binance", "binance.com"));
        platforms.add(new PlatformOption(R.drawable.bdo, "BDO", "bdo.com.ph"));
        platforms.add(new PlatformOption(R.drawable.bpi, "BPI", "bpi.com.ph"));
        platforms.add(new PlatformOption(R.drawable.discord, "Discord", "discord.com"));
        platforms.add(new PlatformOption(R.drawable.dropbox, "Dropbox", "dropbox.com"));
        platforms.add(new PlatformOption(R.drawable.facebook, "Facebook", "facebook.com"));
        platforms.add(new PlatformOption(R.drawable.gcash, "GCash", "gcash.com"));
        platforms.add(new PlatformOption(R.drawable.gitlab, "GitLab", "gitlab.com"));
        platforms.add(new PlatformOption(R.drawable.github, "GitHub", "github.com"));
        platforms.add(new PlatformOption(R.drawable.gmail, "Gmail", "gmail.com"));
        platforms.add(new PlatformOption(R.drawable.gotyme, "GoTyme", "gotyme.com"));
        platforms.add(new PlatformOption(R.drawable.google, "Google", "google.com"));
        platforms.add(new PlatformOption(R.drawable.instagram, "Instagram", "instagram.com"));
        platforms.add(new PlatformOption(R.drawable.itunes, "iTunes", "itunes.com"));
        platforms.add(new PlatformOption(R.drawable.lazada, "Lazada", "lazada.com"));
        platforms.add(new PlatformOption(R.drawable.line, "LINE", "line.me"));
        platforms.add(new PlatformOption(R.drawable.linkedin, "LinkedIn", "linkedin.com"));
        platforms.add(new PlatformOption(R.drawable.maribank, "MariBank", "maribank.com.ph"));
        platforms.add(new PlatformOption(R.drawable.maya, "Maya", "maya.ph"));
        platforms.add(new PlatformOption(R.drawable.messenger, "Messenger", "messenger.com"));
        platforms.add(new PlatformOption(R.drawable.microsoft, "Microsoft", "microsoft.com"));
        platforms.add(new PlatformOption(R.drawable.netflix, "Netflix", "netflix.com"));
        platforms.add(new PlatformOption(R.drawable.paypal, "PayPal", "paypal.com"));
        platforms.add(new PlatformOption(R.drawable.pinterest, "Pinterest", "pinterest.com"));
        platforms.add(new PlatformOption(R.drawable.rcbc, "RCBC", "rcbc.com.ph"));
        platforms.add(new PlatformOption(R.drawable.reddit, "Reddit", "reddit.com"));
        platforms.add(new PlatformOption(R.drawable.shoopee, "Shopee", "shopee.com"));
        platforms.add(new PlatformOption(R.drawable.slack, "Slack", "slack.com"));
        platforms.add(new PlatformOption(R.drawable.snapchat, "Snapchat", "snapchat.com"));
        platforms.add(new PlatformOption(R.drawable.soundcloud, "SoundCloud", "soundcloud.com"));
        platforms.add(new PlatformOption(R.drawable.spotify, "Spotify", "spotify.com"));
        platforms.add(new PlatformOption(R.drawable.steam, "Steam", "store.steampowered.com"));
        platforms.add(new PlatformOption(R.drawable.telegram, "Telegram", "telegram.org"));
        platforms.add(new PlatformOption(R.drawable.tiktok, "TikTok", "tiktok.com"));
        platforms.add(new PlatformOption(R.drawable.tinder, "Tinder", "tinder.com"));
        platforms.add(new PlatformOption(R.drawable.unionbank, "UnionBank", "unionbank.com.ph"));
        platforms.add(new PlatformOption(R.drawable.viber, "Viber", "viber.com"));
        platforms.add(new PlatformOption(R.drawable.wattpad, "Wattpad", "wattpad.com"));
        platforms.add(new PlatformOption(R.drawable.whatsapp, "WhatsApp", "whatsapp.com"));
        platforms.add(new PlatformOption(R.drawable.wise, "Wise", "wise.com"));
        platforms.add(new PlatformOption(R.drawable.x, "X", "x.com"));
        platforms.add(new PlatformOption(R.drawable.youtube, "YouTube", "youtube.com"));
        platforms.add(new PlatformOption(R.drawable.zoom, "Zoom", "zoom.us"));
        return platforms;
    }

    private void bindEditForm(@NonNull CredentialItem item) {
        final Context context = this;


        ImageView platformIcon = findViewById(R.id.platform_icon);
        TextView platformName = findViewById(R.id.platform_name);
        TextView btnSave = findViewById(R.id.btn_save);
        EditText inputUsername = findViewById(R.id.input_username);
        EditText inputPassword = findViewById(R.id.input_password);
        EditText inputPin = findViewById(R.id.input_pin);

        btnSave.setText("Update");

        selectedIcon = item.getIconRes();
        selectedName = item.getPlatform();
        platformIcon.setImageResource(item.getIconRes());
        platformName.setText(item.getPlatform());
        inputUsername.setText(item.getUsername());
        inputPassword.setText(item.getPassword());
        inputPin.setText(item.getPin());

        findViewById(R.id.platform_selector).setOnClickListener(v -> {
            BottomSheetDialog pickerDialog = new BottomSheetDialog(context,
                    com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
            View pickerView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_select_social_platform, null);
            pickerDialog.setContentView(pickerView);

            pickerDialog.setOnShowListener(dialogInterface -> {
                if (pickerDialog.getWindow() != null) {
                    pickerDialog.getWindow().setStatusBarColor(context.getColor(R.color.dark_bg));
                }
                com.google.android.material.bottomsheet.BottomSheetDialog dialog3 =
                        (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
                View bottomSheet = dialog3.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                bottomSheet.requestLayout();
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        .setHideable(false);
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        .setDraggable(false);
            });

            RecyclerView recyclerPlatforms = pickerView.findViewById(R.id.recycler_platforms);
            recyclerPlatforms.setLayoutManager(new LinearLayoutManager(context));
            PlatformSelectionAdapter platformAdapter = new PlatformSelectionAdapter(getPlatforms(),
                    (iconRes, name, url) -> {
                        selectedIcon = iconRes;
                        selectedName = name;
                        platformIcon.setImageResource(iconRes);
                        platformName.setText(name);
                        pickerDialog.dismiss();
                    });
            recyclerPlatforms.setAdapter(platformAdapter);

            EditText searchPlatform = pickerView.findViewById(R.id.search_platform);
            searchPlatform.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    platformAdapter.getFilter().filter(s);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            pickerDialog.show();
        });

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

        List<CredentialItem> existingLogins = dbHelper.getAllLogins();
        LinearLayout associateSection = findViewById(R.id.associate_section);
        RecyclerView recyclerLinked = findViewById(R.id.recycler_linked);
        List<CredentialItem> linkedItems = new ArrayList<>();

        List<Integer> currentAssocIds = dbHelper.getAssociations(item.getId());
        for (CredentialItem login : existingLogins) {
            for (int assocId : currentAssocIds) {
                if (login.getId() == assocId) {
                    linkedItems.add(login);
                    break;
                }
            }
        }

        if (existingLogins.isEmpty()) {
            associateSection.setVisibility(View.GONE);
        } else {
            associateSection.setVisibility(View.VISIBLE);

            LinkedAccountAdapter linkedAdapter = new LinkedAccountAdapter(linkedItems, true, (linkedItem, isRemove) -> {
                if (linkedItems.isEmpty()) {
                    associateSection.setVisibility(View.GONE);
                }
            });
            recyclerLinked.setLayoutManager(new LinearLayoutManager(context));
            recyclerLinked.setAdapter(linkedAdapter);

            findViewById(R.id.btn_add_associate).setOnClickListener(v -> {
                List<CredentialItem> available = new ArrayList<>();
                for (CredentialItem existing : existingLogins) {
                    if (existing.getId() == item.getId()) continue;
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
                    Toast.makeText(context, "All accounts already linked", Toast.LENGTH_SHORT).show();
                    return;
                }

                BottomSheetDialog pickerDialog = new BottomSheetDialog(context,
                        com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
                View pickerView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_link_social_account, null);
                pickerDialog.setContentView(pickerView);

                pickerDialog.setOnShowListener(dialogInterface -> {
                    if (pickerDialog.getWindow() != null) {
                        pickerDialog.getWindow().setStatusBarColor(context.getColor(R.color.dark_bg));
                    }
                    com.google.android.material.bottomsheet.BottomSheetDialog d =
                            (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
                    View bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                    bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
                    bottomSheet.requestLayout();
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                            .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                            .setHideable(false);
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                            .setDraggable(false);
                });

                RecyclerView recyclerAccounts = pickerView.findViewById(R.id.recycler_accounts);
                recyclerAccounts.setLayoutManager(new LinearLayoutManager(context));
                recyclerAccounts.setAdapter(new LinkedAccountAdapter(available, false, ( pickedItem, isRemove) -> {
                    linkedItems.add(pickedItem);
                    linkedAdapter.notifyItemInserted(linkedItems.size() - 1);
                    pickerDialog.dismiss();
                }));

                pickerDialog.show();
            });
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            String pin = inputPin.getText().toString().trim();

            if (username.isEmpty()) {
                inputUsername.setError("Username required");
                return;
            }

            dbHelper.deleteLogin(item.getId());

            CredentialItem updated = new CredentialItem(selectedName, username, password, pin, selectedIcon);
            long newId = dbHelper.insertLogin(updated);

            for (CredentialItem linked : linkedItems) {
                dbHelper.insertAssociation(newId, linked.getId());
            }

            setResult(RESULT_OK);
            finish();
            Toast.makeText(context, "Updated Successfully", Toast.LENGTH_SHORT).show();
        });


    }
}
