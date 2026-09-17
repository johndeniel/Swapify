package com.akin.wallet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.BankCardDesignAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.BankCardItem;

/**
 * Bank card creation/edit form as a full screen (replaces the old bottom
 * sheet). Add mode when no card id is passed; edit mode otherwise. The card
 * design carousel lives in the form content; the type picker stays an
 * alert dialog. Callers refresh in onResume; RESULT_OK is set on save.
 */
public class BankCardFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_NETWORK = "extra_network";
    public static final String EXTRA_BANK = "extra_bank";
    public static final String EXTRA_HOLDER = "extra_holder";
    public static final String EXTRA_NUMBER = "extra_number";
    public static final String EXTRA_EXPIRY = "extra_expiry";
    public static final String EXTRA_CVV = "extra_cvv";
    public static final String EXTRA_PIN = "extra_pin";
    public static final String EXTRA_DESIGN = "extra_design";

    private AppDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_card_form);

        dbHelper = new AppDatabaseHelper(this);

        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        if (id == -1) {
            bindForm(null);
        } else {
            bindForm(new BankCardItem(
                    id,
                    getIntent().getStringExtra(EXTRA_TYPE),
                    getIntent().getStringExtra(EXTRA_NETWORK),
                    getIntent().getStringExtra(EXTRA_BANK),
                    getIntent().getStringExtra(EXTRA_HOLDER),
                    getIntent().getStringExtra(EXTRA_NUMBER),
                    getIntent().getStringExtra(EXTRA_EXPIRY),
                    getIntent().getStringExtra(EXTRA_CVV),
                    getIntent().getStringExtra(EXTRA_PIN),
                    getIntent().getIntExtra(EXTRA_DESIGN, 0)));
        }
    }

    private static final String[] CARD_TYPES = {"Debit", "Credit", "Prepaid"};
    private static final String[] CARD_NETWORKS = {"Visa", "Mastercard"};

    private void bindForm(@Nullable BankCardItem existing) {
        final boolean isEdit = existing != null;


        final int[] selectedType = {0};
        final int[] selectedNetwork = {0};
        final int[] selectedDesign = {0};

        TextView textCardType = findViewById(R.id.text_card_type);
        TextView textCardNetwork = findViewById(R.id.text_card_network);
        EditText inputBankName = findViewById(R.id.input_bank_name);
        EditText inputHolderName = findViewById(R.id.input_holder_name);
        EditText inputCardNumber = findViewById(R.id.input_card_number);
        EditText inputExpiry = findViewById(R.id.input_expiry);
        EditText inputCvv = findViewById(R.id.input_cvv);
        EditText inputPin = findViewById(R.id.input_pin);
        TextView btnSave = findViewById(R.id.btn_save);



        BankCardDesignAdapter designAdapter = new BankCardDesignAdapter();
        RecyclerView recyclerDesign = findViewById(R.id.recycler_card_design);
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(BankCardFormActivity.this, LinearLayoutManager.HORIZONTAL, false);
        recyclerDesign.setLayoutManager(layoutManager);
        recyclerDesign.setAdapter(designAdapter);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerDesign);

        LinearLayout dotsContainer = findViewById(R.id.dots_container);
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

        findViewById(R.id.row_card_type).setOnClickListener(v ->
                showChoiceDialog("Card Type", CARD_TYPES, selectedType[0], which -> {
                    selectedType[0] = which;
                    textCardType.setText(CARD_TYPES[which]);
                    refreshPreview.run();
                }));

        findViewById(R.id.row_card_network).setOnClickListener(v ->
                showChoiceDialog("Card Network", CARD_NETWORKS, selectedNetwork[0], which -> {
                    selectedNetwork[0] = which;
                    textCardNetwork.setText(CARD_NETWORKS[which]);
                    refreshPreview.run();
                }));

        findViewById(R.id.btn_toggle_cvv).setOnClickListener(v -> {
            if (inputCvv.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputCvv.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputCvv.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputCvv.setSelection(inputCvv.getText().length());
        });

        findViewById(R.id.btn_toggle_pin).setOnClickListener(v -> {
            if (inputPin.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
                inputPin.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                inputPin.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
            inputPin.setSelection(inputPin.getText().length());
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            if (inputBankName.getText().toString().trim().isEmpty()) {
                inputBankName.setError("Bank name is required");
                return;
            }
            if (inputHolderName.getText().toString().trim().isEmpty()) {
                inputHolderName.setError("Cardholder name is required");
                return;
            }
            String cardDigits = inputCardNumber.getText().toString().replaceAll("\\D", "");
            if (cardDigits.length() < 13 || cardDigits.length() > 19) {
                inputCardNumber.setError("Card number must be 13-19 digits");
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
                Toast.makeText(BankCardFormActivity.this, "Updated Successfully", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(BankCardFormActivity.this, "Card Saved", Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_OK);
            finish();
        });

        // The bank tab is gone: delete lives here, visible in edit mode only.
        TextView btnDelete = findViewById(R.id.btn_delete);
        if (isEdit) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(BankCardFormActivity.this)
                    .setTitle("Delete Card")
                    .setMessage("Are you sure you want to delete this card?")
                    .setPositiveButton("Delete", (d, which) -> {
                        dbHelper.deleteBankCard(existing.getId());
                        setResult(RESULT_OK);
                        finish();
                        Toast.makeText(BankCardFormActivity.this,
                                "Deleted Successfully", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }


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
        new AlertDialog.Builder(BankCardFormActivity.this)
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
            View dot = new View(BankCardFormActivity.this);
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
