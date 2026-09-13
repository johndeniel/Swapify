package com.akin.wallet.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.BankCardDesignAdapter;
import com.akin.wallet.adapter.BankCardListAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class BankCardsFragment extends Fragment {

    private static final String[] CARD_TYPES = {"Debit", "Credit", "Prepaid"};
    private static final String[] CARD_NETWORKS = {"Visa", "Mastercard"};

    private AppDatabaseHelper dbHelper;
    private BankCardListAdapter cardAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bank_cards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new AppDatabaseHelper(requireContext());

        RecyclerView recyclerCards = view.findViewById(R.id.recycler_cards);
        recyclerCards.setLayoutManager(new LinearLayoutManager(requireContext()));
        cardAdapter = new BankCardListAdapter(dbHelper.getAllBankCards(),
                new BankCardListAdapter.OnCardActionListener() {
                    @Override
                    public void onEdit(BankCardItem item) {
                        showCardDialog(item);
                    }

                    @Override
                    public void onDelete(BankCardItem item) {
                        showDeleteConfirmation(item);
                    }
                });
        recyclerCards.setAdapter(cardAdapter);

        view.findViewById(R.id.btn_add).setOnClickListener(v -> showCardDialog(null));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cardAdapter != null && dbHelper != null) {
            cardAdapter.updateData(dbHelper.getAllBankCards());
        }
    }

    private void showDeleteConfirmation(BankCardItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Card")
                .setMessage("Are you sure you want to delete this card?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteBankCard(item.getId());
                    cardAdapter.updateData(dbHelper.getAllBankCards());
                    Toast.makeText(requireContext(), "Deleted Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCardDialog(@Nullable BankCardItem existing) {
        final boolean isEdit = existing != null;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_add_bank_card, null);
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

        final int[] selectedType = {0};
        final int[] selectedNetwork = {0};
        final int[] selectedDesign = {0};

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        TextView textCardType = dialogView.findViewById(R.id.text_card_type);
        TextView textCardNetwork = dialogView.findViewById(R.id.text_card_network);
        EditText inputBankName = dialogView.findViewById(R.id.input_bank_name);
        EditText inputHolderName = dialogView.findViewById(R.id.input_holder_name);
        EditText inputCardNumber = dialogView.findViewById(R.id.input_card_number);
        EditText inputExpiry = dialogView.findViewById(R.id.input_expiry);
        EditText inputCvv = dialogView.findViewById(R.id.input_cvv);
        EditText inputPin = dialogView.findViewById(R.id.input_pin);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);



        BankCardDesignAdapter designAdapter = new BankCardDesignAdapter();
        RecyclerView recyclerDesign = dialogView.findViewById(R.id.recycler_card_design);
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerDesign.setLayoutManager(layoutManager);
        recyclerDesign.setAdapter(designAdapter);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerDesign);

        LinearLayout dotsContainer = dialogView.findViewById(R.id.dots_container);
        setupDots(dotsContainer, designAdapter.getDesignCount(), 0);

        recyclerDesign.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                View snapView = snapHelper.findSnapView(layoutManager);
                if (snapView != null) {
                    int pos = layoutManager.getPosition(snapView);
                    if (pos != RecyclerView.NO_POSITION && pos != selectedDesign[0]) {
                        selectedDesign[0] = pos;
                        setupDots(dotsContainer, designAdapter.getDesignCount(), pos);
                    }
                }
            }
        });

        if (isEdit) {
            dialogTitle.setText("Edit Bank Card");
            btnSave.setText("Update");
            selectedType[0] = indexOf(CARD_TYPES, existing.getCardType());
            selectedNetwork[0] = indexOf(CARD_NETWORKS, existing.getCardNetwork());
            int design = existing.getDesign();
            if (design < 0 || design >= designAdapter.getDesignCount()) {
                design = 0;
            }
            selectedDesign[0] = design;
            textCardType.setText(CARD_TYPES[selectedType[0]]);
            textCardNetwork.setText(CARD_NETWORKS[selectedNetwork[0]]);
            inputBankName.setText(existing.getBankName());
            inputHolderName.setText(existing.getHolderName());
            inputCardNumber.setText(existing.getCardNumber());
            inputExpiry.setText(existing.getExpiry());
            inputCvv.setText(existing.getCvv());
            inputPin.setText(existing.getPin());
            final int scrollTo = design;
            recyclerDesign.post(() -> {
                recyclerDesign.scrollToPosition(scrollTo);
                setupDots(dotsContainer, designAdapter.getDesignCount(), scrollTo);
            });
        }

        Runnable refreshPreview = () -> {
            String digits = inputCardNumber.getText().toString().replaceAll("\\D", "");
            String last4 = digits.length() > 4
                    ? digits.substring(digits.length() - 4)
                    : digits;
            String expDigits = inputExpiry.getText().toString().replaceAll("\\D", "");
            String expDisplay = expDigits.length() == 4
                    ? expDigits.substring(0, 2) + "/" + expDigits.substring(2)
                    : expDigits;
            designAdapter.updatePreview(
                    inputBankName.getText().toString().trim(),
                    inputHolderName.getText().toString().trim(),
                    last4,
                    expDisplay,
                    CARD_TYPES[selectedType[0]],
                    CARD_NETWORKS[selectedNetwork[0]]);
        };

        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshPreview.run();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        inputBankName.addTextChangedListener(previewWatcher);
        inputHolderName.addTextChangedListener(previewWatcher);
        inputCardNumber.addTextChangedListener(previewWatcher);
        inputExpiry.addTextChangedListener(previewWatcher);

        final boolean[] formattingNumber = {false};
        inputCardNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (formattingNumber[0]) return;
                formattingNumber[0] = true;
                int cursor = inputCardNumber.getSelectionStart();
                int beforeLen = s.length();
                String digits = s.toString().replaceAll("\\D", "");
                if (digits.length() > 19) {
                    digits = digits.substring(0, 19);
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        sb.append(' ');
                    }
                    sb.append(digits.charAt(i));
                }
                s.replace(0, s.length(), sb.toString());
                int newCursor = cursor + (s.length() - beforeLen);
                if (newCursor < 0) newCursor = 0;
                if (newCursor > s.length()) newCursor = s.length();
                inputCardNumber.setSelection(newCursor);
                formattingNumber[0] = false;
            }
        });

        final boolean[] formattingExpiry = {false};
        inputExpiry.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (formattingExpiry[0]) return;
                formattingExpiry[0] = true;
                int cursor = inputExpiry.getSelectionStart();
                int beforeLen = s.length();
                String digits = s.toString().replaceAll("\\D", "");
                if (digits.length() > 4) {
                    digits = digits.substring(0, 4);
                }
                String formatted = digits.length() > 2
                        ? digits.substring(0, 2) + "/" + digits.substring(2)
                        : digits;
                s.replace(0, s.length(), formatted);
                int newCursor = cursor + (s.length() - beforeLen);
                if (newCursor < 0) newCursor = 0;
                if (newCursor > s.length()) newCursor = s.length();
                if (newCursor == 3 && s.length() == 5 && beforeLen < s.length()) {
                    newCursor = 5;
                }
                inputExpiry.setSelection(newCursor);
                formattingExpiry[0] = false;
            }
        });

        dialogView.findViewById(R.id.row_card_type).setOnClickListener(v ->
                showChoiceDialog("Card Type", CARD_TYPES, selectedType[0], which -> {
                    selectedType[0] = which;
                    textCardType.setText(CARD_TYPES[which]);
                    refreshPreview.run();
                }));

        dialogView.findViewById(R.id.row_card_network).setOnClickListener(v ->
                showChoiceDialog("Card Network", CARD_NETWORKS, selectedNetwork[0], which -> {
                    selectedNetwork[0] = which;
                    textCardNetwork.setText(CARD_NETWORKS[which]);
                    refreshPreview.run();
                }));

        dialogView.findViewById(R.id.btn_toggle_cvv).setOnClickListener(v -> {
            if (inputCvv.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputCvv.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputCvv.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputCvv.setSelection(inputCvv.getText().length());
        });

        dialogView.findViewById(R.id.btn_toggle_pin).setOnClickListener(v -> {
            if (inputPin.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPin.setSelection(inputPin.getText().length());
        });

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            if (inputBankName.getText().toString().trim().isEmpty()) {
                inputBankName.setError("Bank name is required");
                return;
            }
            if (inputHolderName.getText().toString().trim().isEmpty()) {
                inputHolderName.setError("Cardholder name is required");
                return;
            }
            String cardDigits = inputCardNumber.getText().toString().replaceAll("\\D", "");
            if (cardDigits.isEmpty() || cardDigits.length() > 19) {
                inputCardNumber.setError("Card number is required (max 19 digits)");
                return;
            }
            String expDigits = inputExpiry.getText().toString().replaceAll("\\D", "");
            String cvvDigits = inputCvv.getText().toString().replaceAll("\\D", "");
            String pinDigits = inputPin.getText().toString().replaceAll("\\D", "");

            if (expDigits.length() != 4) {
                inputExpiry.setError("Expiry must be exactly 4 digits (MMYY)");
                return;
            }
            if (cvvDigits.length() != 3) {
                inputCvv.setError("CVV must be exactly 3 digits");
                return;
            }
            if (pinDigits.isEmpty() || pinDigits.length() > 6) {
                inputPin.setError("PIN is required (max 6 digits)");
                return;
            }

            if (isEdit) {
                BankCardItem updated = new BankCardItem(
                        existing.getId(),
                        CARD_TYPES[selectedType[0]],
                        CARD_NETWORKS[selectedNetwork[0]],
                        inputBankName.getText().toString().trim(),
                        inputHolderName.getText().toString().trim(),
                        cardDigits,
                        expDigits,
                        cvvDigits,
                        pinDigits,
                        selectedDesign[0]);
                dbHelper.updateBankCard(updated);
                cardAdapter.updateData(dbHelper.getAllBankCards());
                Toast.makeText(requireContext(), "Updated Successfully", Toast.LENGTH_SHORT).show();
            } else {
                BankCardItem newCard = new BankCardItem(
                        CARD_TYPES[selectedType[0]],
                        CARD_NETWORKS[selectedNetwork[0]],
                        inputBankName.getText().toString().trim(),
                        inputHolderName.getText().toString().trim(),
                        cardDigits,
                        expDigits,
                        cvvDigits,
                        pinDigits,
                        selectedDesign[0]);
                dbHelper.insertBankCard(newCard);
                cardAdapter.updateData(dbHelper.getAllBankCards());
                Toast.makeText(requireContext(), "Card Saved", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private static int indexOf(String[] options, String value) {
        if (value != null) {
            for (int i = 0; i < options.length; i++) {
                if (options[i].equalsIgnoreCase(value.trim())) {
                    return i;
                }
            }
        }
        return 0;
    }

    private void showChoiceDialog(String title, String[] options, int checked, OnChoiceListener listener) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setSingleChoiceItems(options, checked, (d, which) -> {
                    listener.onChoice(which);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDots(LinearLayout container, int count, int selected) {
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        int margin = (int) (4 * density);
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot);
            dot.setAlpha(i == selected ? 1f : 0.3f);
            container.addView(dot);
        }
    }

    private interface OnChoiceListener {
        void onChoice(int which);
    }
}
