package com.akin.wallet;

import android.graphics.Rect;
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
    public static final String EXTRA_CREATED_AT = "extra_created_at";
    public static final String EXTRA_UPDATED_AT = "extra_updated_at";

    // Fixed option sets. Order doubles as the persisted design/type index, so
    // never reorder without a DB migration.
    private static final String[] CARD_TYPES = {"Debit", "Credit", "Prepaid"};
    private static final String[] CARD_NETWORKS = {"Visa", "Mastercard"};

    // Validation rules. Card number range follows ISO/IEC 7812 (13-19 digits);
    // CVV is 3 digits because only Visa/Mastercard are offered (no Amex 4-digit).
    // PIN follows the common 4-6 digit ATM convention.
    private static final int CARD_NUMBER_MIN_LEN = 16;
    private static final int CARD_NUMBER_MAX_LEN = 19;
    private static final int EXPIRY_DIGITS_LEN = 4;
    private static final int CVV_LEN = 3;
    private static final int PIN_MIN_LEN = 4;
    private static final int PIN_MAX_LEN = 6;
    private static final int BANK_NAME_MIN_LEN = 2;
    private static final int BANK_NAME_MAX_LEN = 50;
    private static final int HOLDER_NAME_MIN_LEN = 2;
    private static final int HOLDER_NAME_MAX_LEN = 50;
    private static final String NON_DIGITS_REGEX = "\\D";

    // Rotation keys. EditTexts restore their own text; the pickers do not, so
    // the selected indices are saved explicitly.
    private static final String KEY_SELECTED_TYPE = "selected_type";
    private static final String KEY_SELECTED_NETWORK = "selected_network";
    private static final String KEY_SELECTED_DESIGN = "selected_design";

    private AppDatabaseHelper dbHelper;

    // Form state. Plain ints (not single-element arrays): bindForm runs once per
    // creation, and lambdas capture the activity, so no effectively-final hack.
    private boolean isEdit;
    private BankCardItem editingItem;
    private int selectedType;
    private int selectedNetwork;
    private int selectedDesign;

    // Cached views. Looked up once to keep bindForm readable and avoid repeated
    // traversal on every keystroke/preview refresh.
    private TextView textCardType;
    private TextView textCardNetwork;
    private EditText inputBankName;
    private EditText inputHolderName;
    private EditText inputCardNumber;
    private EditText inputExpiry;
    private EditText inputCvv;
    private EditText inputPin;
    private View btnSave;
    private TextView textSaveLabel;
    private View btnDelete;

    private BankCardDesignAdapter designAdapter;
    private RecyclerView recyclerDesign;
    private LinearLayoutManager designLayoutManager;
    private PagerSnapHelper designSnapHelper;
    private LinearLayout dotsContainer;

    // Re-entrancy guards for the formatting watchers. Without these, setText
    // inside afterTextChanged would recurse until a stack overflow.
    private boolean isFormattingNumber;
    private boolean isFormattingExpiry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_card_form);

        dbHelper = new AppDatabaseHelper(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        if (savedInstanceState != null) {
            // Restore picker state before binding; invalid values fall back to 0.
            selectedType = sanitizeIndex(
                    savedInstanceState.getInt(KEY_SELECTED_TYPE, 0), CARD_TYPES.length);
            selectedNetwork = sanitizeIndex(
                    savedInstanceState.getInt(KEY_SELECTED_NETWORK, 0), CARD_NETWORKS.length);
            selectedDesign = Math.max(0, savedInstanceState.getInt(KEY_SELECTED_DESIGN, 0));
        }

        bindForm(resolveEditingItem(savedInstanceState != null));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TYPE, selectedType);
        outState.putInt(KEY_SELECTED_NETWORK, selectedNetwork);
        outState.putInt(KEY_SELECTED_DESIGN, selectedDesign);
    }

    @Override
    protected void onDestroy() {
        // SQLiteOpenHelper holds a pooled connection; release it with the screen.
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    /**
     * Rebuilds the editing item from the launch intent. Returns null for add
     * mode (no id extra), which drives every isEdit branch downstream.
     *
     * @param restored true when pickers were already restored from rotation and
     *                 must not be overwritten by intent defaults
     */
    @Nullable
    private BankCardItem resolveEditingItem(boolean restored) {
        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        if (id == -1) {
            return null;
        }
        BankCardItem item = new BankCardItem(
                id,
                getIntent().getStringExtra(EXTRA_TYPE),
                getIntent().getStringExtra(EXTRA_NETWORK),
                getIntent().getStringExtra(EXTRA_BANK),
                getIntent().getStringExtra(EXTRA_HOLDER),
                getIntent().getStringExtra(EXTRA_NUMBER),
                getIntent().getStringExtra(EXTRA_EXPIRY),
                getIntent().getStringExtra(EXTRA_CVV),
                getIntent().getStringExtra(EXTRA_PIN),
                getIntent().getIntExtra(EXTRA_DESIGN, 0),
                getIntent().getLongExtra(EXTRA_CREATED_AT, 0),
                getIntent().getLongExtra(EXTRA_UPDATED_AT, 0));
        if (!restored) {
            // Fresh launch: seed pickers from the stored card; rotation keeps
            // the user's in-progress picks instead.
            selectedType = indexOf(CARD_TYPES, item.getCardType());
            selectedNetwork = indexOf(CARD_NETWORKS, item.getCardNetwork());
            selectedDesign = item.getDesign();
        }
        return item;
    }

    /** Entry point: wires every section in dependency order. */
    private void bindForm(@Nullable BankCardItem existing) {
        isEdit = existing != null;
        editingItem = existing;

        cacheViews();
        setupDesignCarousel();
        if (isEdit) {
            prefillEditMode(existing);
        } else {
            applyAddModeLayout();
        }
        setupPreviewBinding();
        setupInputFormatting();
        setupPickers();
        setupVisibilityToggles();
        setupSaveAction();
        setupDeleteAction();
    }

    /** Single findViewById pass; all later code uses these fields. */
    private void cacheViews() {
        textCardType = findViewById(R.id.text_card_type);
        textCardNetwork = findViewById(R.id.text_card_network);
        inputBankName = findViewById(R.id.input_bank_name);
        inputHolderName = findViewById(R.id.input_holder_name);
        inputCardNumber = findViewById(R.id.input_card_number);
        inputExpiry = findViewById(R.id.input_expiry);
        inputCvv = findViewById(R.id.input_cvv);
        inputPin = findViewById(R.id.input_pin);
        btnSave = findViewById(R.id.btn_save);
        textSaveLabel = findViewById(R.id.text_save_label);
        btnDelete = findViewById(R.id.btn_delete);
        recyclerDesign = findViewById(R.id.recycler_card_design);
        dotsContainer = findViewById(R.id.dots_container);
    }

    /** Horizontal snap carousel shared with the dashboard (same XML + ratio). */
    private void setupDesignCarousel() {
        designAdapter = new BankCardDesignAdapter();
        designLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        recyclerDesign.setLayoutManager(designLayoutManager);
        recyclerDesign.setAdapter(designAdapter);

        // Same 12dp inter-card gap as DashboardFragment so spacing matches.
        recyclerDesign.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View child,
                                       @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(child);
                if (position != RecyclerView.NO_POSITION
                        && position < state.getItemCount() - 1) {
                    float density = parent.getResources().getDisplayMetrics().density;
                    outRect.right = (int) (12 * density);
                }
            }
        });
        designSnapHelper = new PagerSnapHelper();
        designSnapHelper.attachToRecyclerView(recyclerDesign);

        createDots(dotsContainer, designAdapter.getDesignCount());
        updateDots(dotsContainer, selectedDesign);

        // PagerSnapHelper reports the centered page; that page is the design.
        recyclerDesign.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                View snapView = designSnapHelper.findSnapView(designLayoutManager);
                if (snapView != null) {
                    int pos = designLayoutManager.getPosition(snapView);
                    if (pos != RecyclerView.NO_POSITION && pos != selectedDesign) {
                        selectedDesign = pos;
                        updateDots(dotsContainer, pos);
                    }
                }
            }
        });
    }

    /** Fills every field from the stored card; clamps a stale design index. */
    private void prefillEditMode(@NonNull BankCardItem existing) {
        textSaveLabel.setText("Update");
        textCardType.setText(CARD_TYPES[selectedType]);
        textCardNetwork.setText(CARD_NETWORKS[selectedNetwork]);

        if (selectedDesign < 0 || selectedDesign >= designAdapter.getDesignCount()) {
            selectedDesign = 0;
        }
        inputBankName.setText(existing.getBankName());
        inputHolderName.setText(existing.getHolderName());
        // Stored values are raw digits; the formatting watchers add
        // grouping (card spaces) and the expiry slash on setText.
        inputCardNumber.setText(existing.getCardNumber());
        inputExpiry.setText(existing.getExpiry());
        inputCvv.setText(existing.getCvv());
        inputPin.setText(existing.getPin());

        final int scrollTo = selectedDesign;
        recyclerDesign.post(() -> {
            recyclerDesign.scrollToPosition(scrollTo);
            updateDots(dotsContainer, scrollTo);
        });
    }

    /**
     * Add mode keeps a single full-width Save button. The delete view is GONE,
     * so its row margin is dropped to avoid a trailing 8dp gap.
     */
    private void applyAddModeLayout() {
        textSaveLabel.setText(R.string.action_save);
        LinearLayout.LayoutParams saveParams =
                (LinearLayout.LayoutParams) btnSave.getLayoutParams();
        saveParams.setMarginEnd(0);
        btnSave.setLayoutParams(saveParams);
    }

    /** Live card-face preview; also clears stale errors as the user types. */
    private void setupPreviewBinding() {
        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshPreview();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        inputBankName.addTextChangedListener(previewWatcher);
        inputHolderName.addTextChangedListener(previewWatcher);
        inputCardNumber.addTextChangedListener(previewWatcher);
        inputExpiry.addTextChangedListener(previewWatcher);

        clearErrorOnChange(inputBankName);
        clearErrorOnChange(inputHolderName);
        clearErrorOnChange(inputCardNumber);
        clearErrorOnChange(inputExpiry);
        clearErrorOnChange(inputCvv);
        clearErrorOnChange(inputPin);

        refreshPreview();
    }

    /** Pushes trimmed field values into the carousel's placeholder face. */
    private void refreshPreview() {
        String digits = extractDigits(inputCardNumber.getText().toString());
        String last4 = digits.length() > 4
                ? digits.substring(digits.length() - 4)
                : digits;
        String expDigits = extractDigits(inputExpiry.getText().toString());
        String expDisplay = expDigits.length() == EXPIRY_DIGITS_LEN
                ? expDigits.substring(0, 2) + "/" + expDigits.substring(2)
                : expDigits;
        designAdapter.updatePreview(
                inputBankName.getText().toString().trim(),
                inputHolderName.getText().toString().trim(),
                last4,
                expDisplay,
                CARD_TYPES[selectedType],
                CARD_NETWORKS[selectedNetwork]);
    }

    /** Groups card digits in 4s and expiry as MM/YY while preserving cursor. */
    private void setupInputFormatting() {
        inputCardNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isFormattingNumber) {
                    return;
                }
                isFormattingNumber = true;
                try {
                    int cursor = inputCardNumber.getSelectionStart();
                    int beforeLen = s.length();
                    String digits = extractDigits(s.toString());
                    if (digits.length() > CARD_NUMBER_MAX_LEN) {
                        digits = digits.substring(0, CARD_NUMBER_MAX_LEN);
                    }
                    s.replace(0, s.length(), groupInFours(digits));
                    inputCardNumber.setSelection(clampCursor(cursor + (s.length() - beforeLen), s.length()));
                } finally {
                    isFormattingNumber = false;
                }
            }
        });

        inputExpiry.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isFormattingExpiry) {
                    return;
                }
                isFormattingExpiry = true;
                try {
                    int cursor = inputExpiry.getSelectionStart();
                    int beforeLen = s.length();
                    String digits = extractDigits(s.toString());
                    if (digits.length() > EXPIRY_DIGITS_LEN) {
                        digits = digits.substring(0, EXPIRY_DIGITS_LEN);
                    }
                    s.replace(0, s.length(), formatExpiryInput(digits));
                    int newCursor = clampCursor(cursor + (s.length() - beforeLen), s.length());
                    // Typing the 4th digit inserts a slash before it, jumping
                    // the length 4 -> 5; pin the cursor to the end in that case.
                    if (newCursor == 3 && s.length() == 5 && beforeLen < s.length()) {
                        newCursor = 5;
                    }
                    inputExpiry.setSelection(newCursor);
                } finally {
                    isFormattingExpiry = false;
                }
            }
        });
    }

    private void setupPickers() {
        findViewById(R.id.row_card_type).setOnClickListener(v ->
                showChoiceDialog("Card Type", CARD_TYPES, selectedType, which -> {
                    selectedType = which;
                    textCardType.setText(CARD_TYPES[which]);
                    refreshPreview();
                }));

        findViewById(R.id.row_card_network).setOnClickListener(v ->
                showChoiceDialog("Card Network", CARD_NETWORKS, selectedNetwork, which -> {
                    selectedNetwork = which;
                    textCardNetwork.setText(CARD_NETWORKS[which]);
                    refreshPreview();
                }));
    }

    /** Eye icons flip the transformation method without losing cursor. */
    private void setupVisibilityToggles() {
        findViewById(R.id.btn_toggle_cvv).setOnClickListener(v ->
                togglePasswordVisibility(inputCvv));
        findViewById(R.id.btn_toggle_pin).setOnClickListener(v ->
                togglePasswordVisibility(inputPin));
    }

    private static void togglePasswordVisibility(EditText input) {
        if (input.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
            input.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        input.setSelection(input.getText().length());
    }

    private void setupSaveAction() {
        btnSave.setOnClickListener(v -> {
            if (!validateForm()) {
                return;
            }
            // Digits were validated; strip formatting once for storage so the
            // DB always holds canonical raw values (no spaces or slashes).
            String cardDigits = extractDigits(inputCardNumber.getText().toString());
            String expDigits = extractDigits(inputExpiry.getText().toString());
            String cvvDigits = extractDigits(inputCvv.getText().toString());
            String pinDigits = extractDigits(inputPin.getText().toString());

            if (isEdit) {
                dbHelper.updateBankCard(new BankCardItem(
                        editingItem.getId(),
                        CARD_TYPES[selectedType],
                        CARD_NETWORKS[selectedNetwork],
                        inputBankName.getText().toString().trim(),
                        inputHolderName.getText().toString().trim(),
                        cardDigits,
                        expDigits,
                        cvvDigits,
                        pinDigits,
                        selectedDesign,
                        // createdAt rides along untouched; updatedAt=0 tells the
                        // DB helper to stamp now().
                        editingItem.getCreatedAt(), 0));
                Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.insertBankCard(new BankCardItem(
                        CARD_TYPES[selectedType],
                        CARD_NETWORKS[selectedNetwork],
                        inputBankName.getText().toString().trim(),
                        inputHolderName.getText().toString().trim(),
                        cardDigits,
                        expDigits,
                        cvvDigits,
                        pinDigits,
                        selectedDesign));
                Toast.makeText(this, "Card Saved", Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_OK);
            finish();
        });
    }

    /** Delete lives on this screen (no bank tab); edit mode shows it in-row. */
    private void setupDeleteAction() {
        if (!isEdit) {
            return;
        }
        btnDelete.setVisibility(View.VISIBLE);
        btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete Card")
                .setMessage("Are you sure you want to delete this card?")
                .setPositiveButton("Delete", (d, which) -> {
                    dbHelper.deleteBankCard(editingItem.getId());
                    setResult(RESULT_OK);
                    finish();
                    Toast.makeText(this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show());
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    /**
     * Validates every field, marks each offender with setError, and focuses the
     * first invalid input. All fields are checked (not fail-fast) so the user
     * sees the full scope in one pass.
     *
     * @return true when every field is persistable
     */
    private boolean validateForm() {
        View firstInvalid = null;

        String bank = inputBankName.getText().toString().trim();
        if (bank.isEmpty()) {
            inputBankName.setError("Bank name is required");
            firstInvalid = firstInvalidOr(firstInvalid, inputBankName);
        } else if (bank.length() < BANK_NAME_MIN_LEN) {
            inputBankName.setError("Bank name must be at least " + BANK_NAME_MIN_LEN + " characters");
            firstInvalid = firstInvalidOr(firstInvalid, inputBankName);
        } else if (bank.length() > BANK_NAME_MAX_LEN) {
            inputBankName.setError("Bank name must be under " + (BANK_NAME_MAX_LEN + 1) + " characters");
            firstInvalid = firstInvalidOr(firstInvalid, inputBankName);
        }

        String holder = inputHolderName.getText().toString().trim();
        if (holder.isEmpty()) {
            inputHolderName.setError("Cardholder name is required");
            firstInvalid = firstInvalidOr(firstInvalid, inputHolderName);
        } else if (holder.length() < HOLDER_NAME_MIN_LEN) {
            inputHolderName.setError("Cardholder name must be at least " + HOLDER_NAME_MIN_LEN + " characters");
            firstInvalid = firstInvalidOr(firstInvalid, inputHolderName);
        } else if (holder.length() > HOLDER_NAME_MAX_LEN) {
            inputHolderName.setError("Cardholder name must be under " + (HOLDER_NAME_MAX_LEN + 1) + " characters");
            firstInvalid = firstInvalidOr(firstInvalid, inputHolderName);
        } else if (!holder.matches("[\\p{L}][\\p{L} .'-]*")) {
            // Unicode-aware: allows accented names, denies digits/symbols.
            inputHolderName.setError("Name can only contain letters, spaces, . ' -");
            firstInvalid = firstInvalidOr(firstInvalid, inputHolderName);
        }

        String cardDigits = extractDigits(inputCardNumber.getText().toString());
        if (cardDigits.length() < CARD_NUMBER_MIN_LEN || cardDigits.length() > CARD_NUMBER_MAX_LEN) {
            inputCardNumber.setError(
                    "Card number must be " + CARD_NUMBER_MIN_LEN + "-" + CARD_NUMBER_MAX_LEN + " digits");
            firstInvalid = firstInvalidOr(firstInvalid, inputCardNumber);
        }

        String expDigits = extractDigits(inputExpiry.getText().toString());
        if (expDigits.length() != EXPIRY_DIGITS_LEN) {
            inputExpiry.setError("Expiry must be exactly 4 digits (MMYY)");
            firstInvalid = firstInvalidOr(firstInvalid, inputExpiry);
        }

        String cvvDigits = extractDigits(inputCvv.getText().toString());
        if (cvvDigits.length() != CVV_LEN) {
            inputCvv.setError("CVV must be exactly " + CVV_LEN + " digits");
            firstInvalid = firstInvalidOr(firstInvalid, inputCvv);
        }

        String pinDigits = extractDigits(inputPin.getText().toString());
        if (pinDigits.length() < PIN_MIN_LEN || pinDigits.length() > PIN_MAX_LEN) {
            inputPin.setError("PIN must be " + PIN_MIN_LEN + "-" + PIN_MAX_LEN + " digits");
            firstInvalid = firstInvalidOr(firstInvalid, inputPin);
        }

        if (firstInvalid != null) {
            firstInvalid.requestFocus();
            return false;
        }
        return true;
    }

    private static View firstInvalidOr(@Nullable View current, @NonNull View candidate) {
        return current != null ? current : candidate;
    }

    /** Clears a stale setError as soon as the user edits the field again. */
    private static void clearErrorOnChange(EditText input) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (input.getError() != null) {
                    input.setError(null);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ------------------------------------------------------------------
    // Small pure helpers (unit-testable, no Android state)
    // ------------------------------------------------------------------

    private static String extractDigits(String raw) {
        return raw == null ? "" : raw.replaceAll(NON_DIGITS_REGEX, "");
    }

    /** Groups raw digits for display: "12345678" -> "1234 5678". */
    private static String groupInFours(String digits) {
        StringBuilder sb = new StringBuilder(digits.length() + digits.length() / 4);
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(' ');
            }
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    /** Formats raw expiry digits for display: "1" -> "1", "122" -> "12/2". */
    private static String formatExpiryInput(String digits) {
        return digits.length() > 2
                ? digits.substring(0, 2) + "/" + digits.substring(2)
                : digits;
    }

    private static int clampCursor(int cursor, int length) {
        return Math.max(0, Math.min(cursor, length));
    }

    private static int sanitizeIndex(int index, int size) {
        return index < 0 || index >= size ? 0 : index;
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
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(options, checked, (d, which) -> {
                    listener.onChoice(which);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Builds dots once; use updateDots() on scroll to avoid view churn. */
    private void createDots(LinearLayout container, int count) {
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        int margin = (int) (4 * density);
        for (int i = 0; i < count; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot);
            container.addView(dot);
        }
    }

    /** Flips dot alpha in place; cheaper than rebuilding on every scroll tick. */
    private static void updateDots(LinearLayout container, int selected) {
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setAlpha(i == selected ? 1f : 0.3f);
        }
    }

    /** Kept for compatibility; prefer createDots() + updateDots(). */
    private void setupDots(LinearLayout container, int count, int selected) {
        createDots(container, count);
        updateDots(container, selected);
    }

    private interface OnChoiceListener {
        void onChoice(int which);
    }
}
