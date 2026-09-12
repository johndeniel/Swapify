package com.akin.wallet.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;

import com.akin.wallet.R;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.LinkedAccountAdapter;
import com.akin.wallet.adapter.PlatformSelectionAdapter;
import com.akin.wallet.adapter.SocialLoginAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.PlatformOption;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

public class SocialLoginsFragment extends Fragment {

    private SocialLoginAdapter adapter;
    private AppDatabaseHelper dbHelper;
    private int selectedIcon = R.drawable.google;
    private String selectedName = "Google";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_social_logins, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new AppDatabaseHelper(requireContext());

        RecyclerView recycler = view.findViewById(R.id.recycler_logins);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new SocialLoginAdapter(dbHelper.getAllLogins(), dbHelper);
        adapter.setOnCredentialActionListener(new SocialLoginAdapter.OnCredentialActionListener() {
            @Override
            public void onEdit(CredentialItem item) {
                showEditLoginDialog(item);
            }

            @Override
            public void onDelete(CredentialItem item) {
                showDeleteConfirmation(item);
            }
        });
        recycler.setAdapter(adapter);

        EditText searchInput = view.findViewById(R.id.search_login);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btn_add).setOnClickListener(v -> showAddLoginDialog());
    }

    private void showDeleteConfirmation(CredentialItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete " + item.getPlatform() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteLogin(item.getId());
                    adapter.updateData(dbHelper.getAllLogins());
                    Toast.makeText(requireContext(), "Deleted Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditLoginDialog(CredentialItem item) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_add_credential, null);
        dialog.setContentView(dialogView);

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setStatusBarColor(Color.parseColor("#FF0D1B2A"));
            }
            com.google.android.material.bottomsheet.BottomSheetDialog dialog2 =
                    (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
            View bottomSheet = dialog2.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.requestLayout();
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setHideable(false);
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setDraggable(false);
        });

        ImageView platformIcon = dialogView.findViewById(R.id.platform_icon);
        TextView platformName = dialogView.findViewById(R.id.platform_name);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);
        EditText inputUsername = dialogView.findViewById(R.id.input_username);
        EditText inputPassword = dialogView.findViewById(R.id.input_password);
        EditText inputPin = dialogView.findViewById(R.id.input_pin);

        btnSave.setText("Update");

        selectedIcon = item.getIconRes();
        selectedName = item.getPlatform();
        platformIcon.setImageResource(item.getIconRes());
        platformName.setText(item.getPlatform());
        inputUsername.setText(item.getUsername());
        inputPassword.setText(item.getPassword());
        inputPin.setText(item.getPin());

        dialogView.findViewById(R.id.platform_selector).setOnClickListener(v -> {
            BottomSheetDialog pickerDialog = new BottomSheetDialog(requireContext(),
                    com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
            View pickerView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_select_platform, null);
            pickerDialog.setContentView(pickerView);

            pickerDialog.setOnShowListener(dialogInterface -> {
                if (pickerDialog.getWindow() != null) {
                    pickerDialog.getWindow().setStatusBarColor(Color.parseColor("#FF0D1B2A"));
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
            recyclerPlatforms.setLayoutManager(new LinearLayoutManager(requireContext()));
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

        dialogView.findViewById(R.id.btn_toggle_password).setOnClickListener(v -> {
            if (inputPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        dialogView.findViewById(R.id.btn_toggle_pin).setOnClickListener(v -> {
            if (inputPin.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPin.setSelection(inputPin.getText().length());
        });

        List<CredentialItem> existingLogins = dbHelper.getAllLogins();
        LinearLayout associateSection = dialogView.findViewById(R.id.associate_section);
        RecyclerView recyclerLinked = dialogView.findViewById(R.id.recycler_linked);
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
            recyclerLinked.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerLinked.setAdapter(linkedAdapter);

            dialogView.findViewById(R.id.btn_add_associate).setOnClickListener(v -> {
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
                    Toast.makeText(requireContext(), "All accounts already linked", Toast.LENGTH_SHORT).show();
                    return;
                }

                BottomSheetDialog pickerDialog = new BottomSheetDialog(requireContext(),
                        com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
                View pickerView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_link_account, null);
                pickerDialog.setContentView(pickerView);

                pickerDialog.setOnShowListener(dialogInterface -> {
                    if (pickerDialog.getWindow() != null) {
                        pickerDialog.getWindow().setStatusBarColor(Color.parseColor("#FF0D1B2A"));
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
                recyclerAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
                recyclerAccounts.setAdapter(new LinkedAccountAdapter(available, false, ( pickedItem, isRemove) -> {
                    linkedItems.add(pickedItem);
                    linkedAdapter.notifyItemInserted(linkedItems.size() - 1);
                    pickerDialog.dismiss();
                }));

                pickerDialog.show();
            });
        }

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
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

            adapter.updateData(dbHelper.getAllLogins());
            dialog.dismiss();
            Toast.makeText(requireContext(), "Updated Successfully", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private List<PlatformOption> getPlatforms() {
        List<PlatformOption> platforms = new ArrayList<>();
        platforms.add(new PlatformOption(R.drawable.discord, "Discord", "discord.com"));
        platforms.add(new PlatformOption(R.drawable.facebook, "Facebook", "facebook.com"));
        platforms.add(new PlatformOption(R.drawable.github, "GitHub", "github.com"));
        platforms.add(new PlatformOption(R.drawable.gmail, "Gmail", "gmail.com"));
        platforms.add(new PlatformOption(R.drawable.google, "Google", "google.com"));
        platforms.add(new PlatformOption(R.drawable.instagram, "Instagram", "instagram.com"));
        platforms.add(new PlatformOption(R.drawable.linkedin, "LinkedIn", "linkedin.com"));
        platforms.add(new PlatformOption(R.drawable.reddit, "Reddit", "reddit.com"));
        platforms.add(new PlatformOption(R.drawable.snapchat, "Snapchat", "snapchat.com"));
        platforms.add(new PlatformOption(R.drawable.spotify, "Spotify", "spotify.com"));
        platforms.add(new PlatformOption(R.drawable.telegram, "Telegram", "telegram.org"));
        platforms.add(new PlatformOption(R.drawable.tiktok, "TikTok", "tiktok.com"));
        platforms.add(new PlatformOption(R.drawable.whatsapp, "WhatsApp", "whatsapp.com"));
        platforms.add(new PlatformOption(R.drawable.x, "X", "x.com"));
        platforms.add(new PlatformOption(R.drawable.youtube, "YouTube", "youtube.com"));
        return platforms;
    }

    private void showAddLoginDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_add_credential, null);
        dialog.setContentView(dialogView);

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setStatusBarColor(Color.parseColor("#FF0D1B2A"));
            }
            com.google.android.material.bottomsheet.BottomSheetDialog dialog2 =
                    (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
            View bottomSheet = dialog2.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.requestLayout();
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setHideable(false);
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setDraggable(false);
        });

        ImageView platformIcon = dialogView.findViewById(R.id.platform_icon);
        TextView platformName = dialogView.findViewById(R.id.platform_name);

        platformIcon.setImageResource(selectedIcon);
        platformName.setText(selectedName);

        dialogView.findViewById(R.id.platform_selector).setOnClickListener(v -> {
            BottomSheetDialog pickerDialog = new BottomSheetDialog(requireContext(),
                    com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
            View pickerView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_select_platform, null);
            pickerDialog.setContentView(pickerView);

            pickerDialog.setOnShowListener(dialogInterface -> {
                if (pickerDialog.getWindow() != null) {
                    pickerDialog.getWindow().setStatusBarColor(Color.parseColor("#FF0D1B2A"));
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
            recyclerPlatforms.setLayoutManager(new LinearLayoutManager(requireContext()));
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

        EditText inputPassword = dialogView.findViewById(R.id.input_password);
        dialogView.findViewById(R.id.btn_toggle_password).setOnClickListener(v -> {
            if (inputPassword.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        EditText inputPin = dialogView.findViewById(R.id.input_pin);
        dialogView.findViewById(R.id.btn_toggle_pin).setOnClickListener(v -> {
            if (inputPin.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPin.setSelection(inputPin.getText().length());
        });

        List<CredentialItem> existingLogins = dbHelper.getAllLogins();
        LinearLayout associateSection = dialogView.findViewById(R.id.associate_section);
        RecyclerView recyclerLinked = dialogView.findViewById(R.id.recycler_linked);
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
            recyclerLinked.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerLinked.setAdapter(linkedAdapter);

            dialogView.findViewById(R.id.btn_add_associate).setOnClickListener(v -> {
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
                    Toast.makeText(requireContext(), "All accounts already linked", Toast.LENGTH_SHORT).show();
                    return;
                }

                BottomSheetDialog pickerDialog = new BottomSheetDialog(requireContext(),
                        com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
                View pickerView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_link_account, null);
                pickerDialog.setContentView(pickerView);

                pickerDialog.setOnShowListener(dialogInterface -> {
                    if (pickerDialog.getWindow() != null) {
                        pickerDialog.getWindow().setStatusBarColor(Color.parseColor("#FF0D1B2A"));
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
                recyclerAccounts.setLayoutManager(new LinearLayoutManager(requireContext()));
                recyclerAccounts.setAdapter(new LinkedAccountAdapter(available, false, (pickedItem, isRemove) -> {
                    linkedItems.add(pickedItem);
                    linkedAdapter.notifyItemInserted(linkedItems.size() - 1);
                    pickerDialog.dismiss();
                }));

                pickerDialog.show();
            });
        }

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            EditText inputUsername = dialogView.findViewById(R.id.input_username);
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

            adapter.updateData(dbHelper.getAllLogins());
            dialog.dismiss();
            Toast.makeText(requireContext(), "Account Saved", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
}
