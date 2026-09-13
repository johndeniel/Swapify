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
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class BankCardsFragment extends Fragment {

    private static final String[] CARD_TYPES = {"Debit", "Credit", "Prepaid"};
    private static final String[] CARD_NETWORKS = {"Visa", "Mastercard", "Amex", "UnionPay", "JCB"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bank_cards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btn_add).setOnClickListener(v -> showAddCardDialog());
    }

    private void showAddCardDialog() {
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

        TextView textCardType = dialogView.findViewById(R.id.text_card_type);
        TextView textCardNetwork = dialogView.findViewById(R.id.text_card_network);
        EditText inputBankName = dialogView.findViewById(R.id.input_bank_name);
        EditText inputHolderName = dialogView.findViewById(R.id.input_holder_name);
        EditText inputCardNumber = dialogView.findViewById(R.id.input_card_number);
        EditText inputExpiry = dialogView.findViewById(R.id.input_expiry);
        EditText inputCvv = dialogView.findViewById(R.id.input_cvv);
        EditText inputPin = dialogView.findViewById(R.id.input_pin);

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

            Toast.makeText(requireContext(), "Design preview only — saving not added yet",
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
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
