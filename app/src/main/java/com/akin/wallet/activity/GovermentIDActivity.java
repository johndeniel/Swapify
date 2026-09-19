package com.akin.wallet.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.GovermentIdDesignAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;
import com.akin.wallet.util.Dialogs;
import com.akin.wallet.util.Ui;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Government ID creation/edit screen as a full screen. Add mode when no ID is
 * passed; edit mode otherwise. The type-picker carousel lives in the screen
 * content; dropdowns stay alert dialogs. Callers refresh in onResume;
 * RESULT_OK is set on save.
 */
public class GovermentIDActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "extra_id";

    /**
     * Intent that opens this screen to edit an existing ID. Carries the row id
     * only — the screen re-queries the vault, so document data never travels
     * as Intent extras and edits always start current.
     */
    public static Intent editIntent(@NonNull Context context, @NonNull IdCardItem item) {
        return new Intent(context, GovermentIDActivity.class)
                .putExtra(EXTRA_ID, item.getId());
    }

    private AppDatabaseHelper dbHelper;
    /** Owned dialogs: dismissed in onDestroy so rotation cannot leak windows. */
    private androidx.appcompat.app.AlertDialog activeDialog;

    // Rotation state. Inputs are built programmatically (no view ids), so the
    // draft + selected type are saved explicitly; bindForm seeds from them.
    private static final String KEY_DRAFT = "draft_values";
    private static final String KEY_SELECTED_TYPE = "selected_type";
    private Map<String, String> savedDraft;
    private int savedSelectedType;
    private boolean hasSavedState;
    // Live references for onSaveInstanceState (bindForm owns the locals).
    private Map<String, String> currentDraft;
    private int[] currentSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goverment_id);

        dbHelper = new AppDatabaseHelper(this);

        // Back chevron, same as the bank card screen: plain finish, no save.
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        if (savedInstanceState != null) {
            // Rotation: re-seed below from the user's in-progress draft.
            hasSavedState = true;
            savedSelectedType = savedInstanceState.getInt(KEY_SELECTED_TYPE, 0);
            Object raw = savedInstanceState.getSerializable(KEY_DRAFT);
            savedDraft = raw instanceof Map
                    ? castStringMap((Map<?, ?>) raw)
                    : new LinkedHashMap<>();
        }
        if (id == -1) {
            bindForm(null);
        } else {
            IdCardItem stored = dbHelper.getIdCardById(id);
            if (stored == null) {
                finish();
                return;
            }
            bindForm(stored);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentDraft != null) {
            outState.putSerializable(KEY_DRAFT, new LinkedHashMap<>(currentDraft));
        }
        if (currentSelected != null) {
            outState.putInt(KEY_SELECTED_TYPE, currentSelected[0]);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> castStringMap(Map<?, ?> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : raw.entrySet()) {
            if (e.getKey() instanceof String && e.getValue() instanceof String) {
                out.put((String) e.getKey(), (String) e.getValue());
            }
        }
        return out;
    }

    @Override
    protected void onDestroy() {
        if (activeDialog != null && activeDialog.isShowing()) {
            activeDialog.dismiss();
        }
        activeDialog = null;
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    private void bindForm(@Nullable IdCardItem existing) {
        final boolean isEdit = existing != null;


        LinearLayout formContainer = findViewById(R.id.form_container);
        // Action row mirrors the bank card screen: btn_save is the outline
        // container, text_save_label carries the Save/Update caption.
        View btnSave = findViewById(R.id.btn_save);
        TextView textSaveLabel = findViewById(R.id.text_save_label);

        // Draft holds EVERY typed value (even for hidden types) so switching
        // ID type back and forth never loses input; save filters to active spec.
        final Map<String, String> draftValues = new LinkedHashMap<>();
        if (isEdit) {
            draftValues.putAll(existing.getFields());
        }
        // Rotation: the user's in-progress draft wins over stored values.
        if (hasSavedState && savedDraft != null) {
            draftValues.putAll(savedDraft);
        }
        currentDraft = draftValues;
        final Map<String, EditText> textInputs = new LinkedHashMap<>();
        // Dropdown value texts, registered for setError like text inputs so a
        // failed save flags the value itself — same behavior as Date of Birth.
        final Map<String, TextView> dropdownValues = new LinkedHashMap<>();

        final int[] selectedType = {0};
        currentSelected = selectedType;
        final boolean[] knownType = {true};

        if (isEdit) {
            textSaveLabel.setText("Update");
            if (IdTypeSpec.isKnownType(existing.getIdType())) {
                selectedType[0] = IdTypeSpec.indexOf(existing.getIdType());
            } else {
                knownType[0] = false;
                selectedType[0] = 0;
            }
        } else {
            // Add mode keeps a single full-width Save button, same as the bank
            // form. The delete view is GONE, so its row margin is dropped to
            // avoid a trailing 8dp gap.
            textSaveLabel.setText(R.string.action_save);
            LinearLayout.LayoutParams saveParams =
                    (LinearLayout.LayoutParams) btnSave.getLayoutParams();
            saveParams.setMarginEnd(0);
            btnSave.setLayoutParams(saveParams);
        }
        if (hasSavedState) {
            String[] names = IdTypeSpec.getTypeNames();
            if (savedSelectedType >= 0 && savedSelectedType < names.length
                    && (knownType[0] || !isEdit)) {
                selectedType[0] = savedSelectedType;
            }
        }

        final GovermentIdDesignAdapter[] adapterRef = new GovermentIdDesignAdapter[1];
        final RecyclerView[] carouselRef = new RecyclerView[1];
        final LinearLayout dotsContainer = findViewById(R.id.dots_container);
        dotsContainer.setVisibility(View.VISIBLE);

        Runnable refreshPreview = () -> {
            if (adapterRef[0] != null) {
                adapterRef[0].updatePreview(draftValues);
            }
        };

        // Bank-style picker: each carousel page IS an ID type's authentic face.
        // Swiping (or tapping) a page selects that type and rebuilds the form.
        GovermentIdDesignAdapter designAdapter = new GovermentIdDesignAdapter(pos -> {
            if (isEdit && !knownType[0]) {
                Snackbar.make(findViewById(android.R.id.content),
                        "ID type is fixed for entries from a newer version",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (pos != selectedType[0]) {
                applyIdTypeSelection(pos, formContainer, draftValues,
                        textInputs, dropdownValues, refreshPreview, dotsContainer, adapterRef[0],
                        selectedType);
            } else if (carouselRef[0] != null) {
                carouselRef[0].smoothScrollToPosition(pos);
            }
        });
        adapterRef[0] = designAdapter;
        RecyclerView recyclerDesign = findViewById(R.id.recycler_card_design);
        carouselRef[0] = recyclerDesign;
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(GovermentIDActivity.this, LinearLayoutManager.HORIZONTAL, false);
        recyclerDesign.setLayoutManager(layoutManager);
        recyclerDesign.setAdapter(designAdapter);
        // Same 12dp inter-card gap as the dashboard carousel so the form
        // picker spaces pages exactly like Home.
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
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerDesign);

        setupDots(dotsContainer, designAdapter.getTypeCount(), selectedType[0]);

        // Guard: ignore carousel callbacks until the initial scroll to the
        // edited type has settled. Otherwise the initial layout at position 0
        // fires onScrolled and rebuilds the form for the wrong type (empty/
        // wrong fields until the user swipes).
        final boolean[] carouselReady = {false};
        recyclerDesign.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!carouselReady[0] || !knownType[0]) {
                    return;
                }
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                View snapView = snapHelper.findSnapView(layoutManager);
                if (snapView != null) {
                    int pos = layoutManager.getPosition(snapView);
                    if (pos != RecyclerView.NO_POSITION && pos != selectedType[0]) {
                        applyIdTypeSelection(pos, formContainer, draftValues,
                                textInputs, dropdownValues, refreshPreview, dotsContainer, adapterRef[0],
                                selectedType);
                    }
                }
            }
        });
        final int scrollTo = selectedType[0];

        // Initial form for the selected (or edited) type. The header title is
        // gone by design — the carousel page itself names the type.
        rebuildForm(formContainer, currentSpec(selectedType[0], knownType[0],
                isEdit ? existing : null), draftValues, textInputs, dropdownValues,
                refreshPreview);
        refreshPreview.run();

        // Position the carousel on the edited type, then enable callbacks.
        // Nested post ensures the scroll layout pass has run before we listen.
        recyclerDesign.post(() -> {
            if (scrollTo != 0) {
                recyclerDesign.scrollToPosition(scrollTo);
            }
            recyclerDesign.post(() -> carouselReady[0] = true);
        });

        btnSave.setOnClickListener(v -> {
            String typeName = currentTypeName(selectedType[0], knownType[0],
                    isEdit ? existing.getIdType() : null);
            IdTypeSpec.IdType spec = currentSpec(selectedType[0], knownType[0],
                    isEdit ? existing : null);

            // Pull latest text (watchers already keep draftValues live; this is a safety net).
            for (Map.Entry<String, EditText> e : textInputs.entrySet()) {
                draftValues.put(e.getKey(), e.getValue().getText().toString().trim());
            }
            Map<String, String> filtered = filteredDraft(typeName, draftValues);

            if (!validate(spec, filtered, textInputs, dropdownValues)) {
                return;
            }

            if (isEdit) {
                // createdAt rides along untouched (creation order is immutable);
                // updatedAt=0 tells the DB helper to stamp now on write.
                // Known types may have been switched via selector; use the new
                // name. Design is fixed per type (column kept as 0).
                String finalType = knownType[0] ? typeName : existing.getIdType();
                IdCardItem updated = new IdCardItem(
                        existing.getId(), finalType, filtered, 0,
                        existing.getCreatedAt(), 0);
                dbHelper.updateIdCard(updated);
                Ui.notifyOnReturn(R.string.msg_updated);
            } else {
                IdCardItem newCard = new IdCardItem(typeName, filtered, 0);
                dbHelper.insertIdCard(newCard);
                Ui.notifyOnReturn(R.string.msg_id_saved);
            }
            setResult(RESULT_OK);
            finish();
        });

        // Delete on this screen (no IDs tab), shown in-row in edit mode
        // only — same placement and outline-red style as the bank card screen.
        // Soft-delete behind the scenes: the dialog reads as a normal delete
        // while the row moves to Trash (Settings). Copy and toasts stay ID-specific.
        View btnDelete = findViewById(R.id.btn_delete);
        if (isEdit) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                if (activeDialog != null && activeDialog.isShowing()) {
                    activeDialog.dismiss();
                }
                activeDialog = Dialogs.confirmDelete(GovermentIDActivity.this,
                        "Delete ID",
                        "Are you sure you want to delete this "
                                + existing.getIdType() + "?",
                        () -> {
                            dbHelper.moveIdCardToTrash(existing.getId());
                            setResult(RESULT_OK);
                            finish();
                            Ui.notifyOnReturn(R.string.msg_deleted);
                        });
            });
        }


    }

    private String currentTypeName(int selected, boolean known, @Nullable String existingType) {
        if (!known && existingType != null) {
            return existingType;
        }
        String[] names = IdTypeSpec.getTypeNames();
        if (selected < 0 || selected >= names.length) {
            return names[0];
        }
        return names[selected];
    }

    private IdTypeSpec.IdType currentSpec(int selected, boolean known,
                                          @Nullable IdCardItem existing) {
        if (!known && existing != null) {
            return IdTypeSpec.genericType(existing.getIdType(), existing.getFields());
        }
        String[] names = IdTypeSpec.getTypeNames();
        if (selected < 0 || selected >= names.length) {
            selected = 0;
        }
        return IdTypeSpec.forName(names[selected]);
    }

    private Map<String, String> filteredDraft(String typeName, Map<String, String> draft) {
        Map<String, String> out = new LinkedHashMap<>();
        IdTypeSpec.IdType spec = IdTypeSpec.isKnownType(typeName)
                ? IdTypeSpec.forName(typeName)
                : IdTypeSpec.genericType(typeName, draft);
        for (IdTypeSpec.IdField f : spec.fields) {
            String v = draft.get(f.key);
            out.put(f.key, v != null ? v : "");
        }
        return out;
    }

    private void rebuildForm(LinearLayout container, IdTypeSpec.IdType spec,
                             Map<String, String> draft, Map<String, EditText> textInputs,
                             Map<String, TextView> dropdownValues, Runnable onChanged) {
        container.removeAllViews();
        textInputs.clear();
        dropdownValues.clear();
        List<IdTypeSpec.IdField> fields = spec.fields;
        for (int i = 0; i < fields.size(); i++) {
            IdTypeSpec.IdField field = fields.get(i);
            ensureDraftValue(draft, field);
            // Row pairing, greedy left-to-right: generic short-field pairs
            // ((date, date), or a date/number/picker followed by a picker)
            // plus any explicit pairWithNext flag from the spec (custom rows
            // the generic rules can't infer, e.g. two text fields). A flagged
            // last field has no next and simply stands alone.
            IdTypeSpec.IdField second = i + 1 < fields.size() ? fields.get(i + 1) : null;
            boolean datePair = second != null && isDateField(field) && isDateField(second);
            boolean shortPair = second != null && second.isDropdown()
                    && (isDateField(field) || isNumberField(field) || field.isDropdown());
            boolean flaggedPair = second != null && field.pairWithNext;
            if (datePair || shortPair || flaggedPair) {
                ensureDraftValue(draft, second);
                container.addView(buildPairRow(field, second, draft, textInputs, dropdownValues, onChanged));
                i++;
            } else {
                container.addView(buildFieldView(field, draft, textInputs, dropdownValues, onChanged));
            }
        }
    }

    /** Guarantees a draft slot so switching types never loses typed input. */
    private static void ensureDraftValue(Map<String, String> draft, IdTypeSpec.IdField field) {
        if (!draft.containsKey(field.key)) {
            draft.put(field.key, "");
        }
    }

    /** Dispatches to the dropdown or free-text builder for one field. */
    private View buildFieldView(IdTypeSpec.IdField field, Map<String, String> draft,
                                Map<String, EditText> textInputs, Map<String, TextView> dropdownValues,
                                Runnable onChanged) {
        if (field.isDropdown()) {
            return buildDropdownField(field, draft, dropdownValues, onChanged);
        }
        return buildTextField(field, draft, textInputs, onChanged);
    }

    /** Date fields carry the datetime input class (see IdField.date). */
    private static boolean isDateField(IdTypeSpec.IdField field) {
        return field != null
                && (field.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_DATETIME;
    }

    /** Number fields carry the number input class (see IdField.number). */
    private static boolean isNumberField(IdTypeSpec.IdField field) {
        return field != null
                && (field.inputType & InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_NUMBER;
    }

    /**
     * Two short fields side by side with equal weight and the bank form's 6dp
     * middle gap. Reuses the standard field builders, then swaps their
     * full-width params for weighted row params (replacing, not adding to,
     * the 16dp top margin the builders set).
     */
    private View buildPairRow(IdTypeSpec.IdField first, IdTypeSpec.IdField second,
                              Map<String, String> draft, Map<String, EditText> textInputs,
                              Map<String, TextView> dropdownValues, Runnable onChanged) {
        LinearLayout row = new LinearLayout(GovermentIDActivity.this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = Ui.dp(this, 16);
        row.setLayoutParams(rowParams);

        View left = buildFieldView(first, draft, textInputs, dropdownValues, onChanged);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.setMarginEnd(Ui.dp(this, 6));
        left.setLayoutParams(leftParams);

        View right = buildFieldView(second, draft, textInputs, dropdownValues, onChanged);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rightParams.setMarginStart(Ui.dp(this, 6));
        right.setLayoutParams(rightParams);

        row.addView(left);
        row.addView(right);
        return row;
    }

    private View buildTextField(IdTypeSpec.IdField field, Map<String, String> draft,
                                Map<String, EditText> textInputs, Runnable onChanged) {
        LinearLayout wrap = new LinearLayout(GovermentIDActivity.this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapParams.topMargin = Ui.dp(this, 16);
        wrap.setLayoutParams(wrapParams);

        TextView label = new TextView(GovermentIDActivity.this);
        label.setText(field.required ? field.label + " *" : field.label);
        label.setTextColor(getResources().getColor(R.color.dashboard_muted, null));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wrap.addView(label);

        EditText input = new EditText(GovermentIDActivity.this);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = Ui.dp(this, 8);
        input.setLayoutParams(inputParams);
        input.setBackgroundResource(R.drawable.bg_dashboard_card);
        input.setHint(field.hint);
        input.setHintTextColor(getResources().getColor(R.color.hint_text, null));
        input.setTextColor(getResources().getColor(R.color.text_primary, null));
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        input.setSingleLine(true);
        input.setInputType(field.inputType);
        if (field.maxLength > 0) {
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(field.maxLength)});
        }
        int h = Ui.dp(this, 16);
        int v = Ui.dp(this, 14);
        input.setPadding(h, v, h, v);
        String current = draft.get(field.key);
        if (current != null && !current.isEmpty()) {
            input.setText(current);
        }
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                draft.put(field.key, s.toString());
                onChanged.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        wrap.addView(input);
        textInputs.put(field.key, input);
        return wrap;
    }

    private View buildDropdownField(IdTypeSpec.IdField field, Map<String, String> draft,
                                    Map<String, TextView> dropdownValues, Runnable onChanged) {
        LinearLayout wrap = new LinearLayout(GovermentIDActivity.this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapParams.topMargin = Ui.dp(this, 16);
        wrap.setLayoutParams(wrapParams);

        TextView label = new TextView(GovermentIDActivity.this);
        label.setText(field.required ? field.label + " *" : field.label);
        label.setTextColor(getResources().getColor(R.color.dashboard_muted, null));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wrap.addView(label);

        LinearLayout row = new LinearLayout(GovermentIDActivity.this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = Ui.dp(this, 8);
        row.setLayoutParams(rowParams);
        row.setBackgroundResource(R.drawable.bg_dashboard_card);
        int h = Ui.dp(this, 16);
        int padV = Ui.dp(this, 14);
        row.setPadding(h, padV, h, padV);
        row.setClickable(true);
        row.setFocusable(true);

        TextView valueView = new TextView(GovermentIDActivity.this);
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        valueView.setLayoutParams(valueParams);
        valueView.setSingleLine(true);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        valueView.setTextColor(getResources().getColor(R.color.text_primary, null));
        // Focusable so a failed save can show the same setError popup a text
        // field shows — dropdown errors behave exactly like date-of-birth
        // errors, with no extra label below the field.
        valueView.setFocusable(true);
        valueView.setFocusableInTouchMode(true);

        String current = draft.get(field.key);
        if (current == null) {
            current = "";
        }
        if (current.isEmpty()) {
            valueView.setText("Select");
            valueView.setAlpha(0.4f);
        } else {
            valueView.setText(current);
            valueView.setAlpha(1f);
        }

        ImageView chevron = new ImageView(GovermentIDActivity.this);
        chevron.setImageResource(R.drawable.ic_dropdown);
        LinearLayout.LayoutParams chevParams = new LinearLayout.LayoutParams(Ui.dp(this, 20), Ui.dp(this, 20));
        chevron.setLayoutParams(chevParams);
        chevron.setContentDescription("Select " + field.label);

        row.addView(valueView);
        row.addView(chevron);

        row.setOnClickListener(v -> {
            int checked = Ui.indexOfIgnoreCase(field.options, draft.get(field.key));
            if (activeDialog != null && activeDialog.isShowing()) {
                activeDialog.dismiss();
            }
            activeDialog = Dialogs.singleChoice(GovermentIDActivity.this,
                    field.label, field.options, checked, which -> {
                        String picked = field.options[which];
                        draft.put(field.key, picked);
                        valueView.setText(picked);
                        valueView.setAlpha(1f);
                        hideFieldError(dropdownValues, field.key);
                        onChanged.run();
                    });
        });

        wrap.addView(row);

        // Registered for setError like a text input — a failed save flags the
        // value text itself, never a label below the field.
        dropdownValues.put(field.key, valueView);
        return wrap;
    }

    private boolean validate(IdTypeSpec.IdType spec, Map<String, String> values,
                             Map<String, EditText> textInputs, Map<String, TextView> dropdownValues) {
        // Document-specific contracts run before the generic pass so the user
        // sees the document rule first: exactly-12-digit numeric TIN / PIN and
        // numeric date shapes (presence itself is covered by the generic
        // required check below).
        if (spec != null && IdTypeSpec.TYPE_TIN.equalsIgnoreCase(spec.name)) {
            if (!validateTinFields(values, textInputs, dropdownValues)) {
                return false;
            }
        } else if (spec != null && IdTypeSpec.TYPE_PHILHEALTH.equalsIgnoreCase(spec.name)) {
            if (!validatePhilHealthFields(values, textInputs, dropdownValues)) {
                return false;
            }
        } else if (spec != null && IdTypeSpec.TYPE_NATIONAL_ID.equalsIgnoreCase(spec.name)) {
            if (!validateNationalIdFields(values, textInputs, dropdownValues)) {
                return false;
            }
        } else if (spec != null && IdTypeSpec.TYPE_PASSPORT.equalsIgnoreCase(spec.name)) {
            if (!validatePassportFields(values, textInputs, dropdownValues)) {
                return false;
            }
        } else if (spec != null && IdTypeSpec.TYPE_DRIVERS_LICENSE.equalsIgnoreCase(spec.name)) {
            if (!validateDriversLicenseFields(values, textInputs, dropdownValues)) {
                return false;
            }
        } else if (spec != null && IdTypeSpec.TYPE_SSS.equalsIgnoreCase(spec.name)) {
            if (!validateSssFields(values, textInputs, dropdownValues)) {
                return false;
            }
        }
        for (IdTypeSpec.IdField f : spec.fields) {
            String v = values.get(f.key);
            if (v == null) {
                v = "";
            }
            v = v.trim();
            if (f.required && v.isEmpty()) {
                return failField(textInputs, dropdownValues, f.key, f.label + " is required");
            }
            if (f.sensitive && !v.isEmpty() && v.replaceAll("[^A-Za-z0-9]", "").length() < 4) {
                return failField(textInputs, dropdownValues, f.key, f.label + " looks too short");
            }
        }
        return true;
    }

    /**
     * TIN-specific document rules. Fail-fast in field order (TIN, birth, issue)
     * so the first offender gets focus, matching the generic pass behavior.
     *
     * <p>BIR TINs are exactly 12 digits with no alphabet (digit count is what
     * matters). Dates are exactly 8 digits (YYYYMMDD) — no calendar computation.
     */
    private boolean validateTinFields(Map<String, String> values,
                                      Map<String, EditText> textInputs,
                                      Map<String, TextView> dropdownValues) {
        String tinError = exactDigitNumberError("TIN", values.get("tinNumber"), 12);
        if (tinError != null) {
            return failField(textInputs, dropdownValues, "tinNumber", tinError);
        }

        String dobError = numericDateError("Date of Birth", values.get("dateOfBirth"));
        if (dobError != null) {
            return failField(textInputs, dropdownValues, "dateOfBirth", dobError);
        }
        String issueError = numericDateError("Date of Issue", values.get("dateOfIssue"));
        if (issueError != null) {
            return failField(textInputs, dropdownValues, "dateOfIssue", issueError);
        }
        return true;
    }

    /**
     * PhilHealth document rules: 12-digit PhilHealth No. (grouped 3-3-3-3 on
     * the face, same presentational rule as the TIN) plus the shared numeric
     * date shape for birth. Fail-fast in field order, matching the TIN pass.
     */
    private boolean validatePhilHealthFields(Map<String, String> values,
                                             Map<String, EditText> textInputs,
                                             Map<String, TextView> dropdownValues) {
        String pinError = exactDigitNumberError("PhilHealth No.", values.get("philhealth_no"), 12);
        if (pinError != null) {
            return failField(textInputs, dropdownValues, "philhealth_no", pinError);
        }

        String dobError = numericDateError("Date of Birth", values.get("dateOfBirth"));
        if (dobError != null) {
            return failField(textInputs, dropdownValues, "dateOfBirth", dobError);
        }
        return true;
    }

    /**
     * National ID document rules: 16-digit numeric PSN (grouped 4-4-4-4 on
     * the face) plus the shared 8-digit shapes for birth and issue. Fail-fast
     * in field order, matching the other passes.
     */
    private boolean validateNationalIdFields(Map<String, String> values,
                                             Map<String, EditText> textInputs,
                                             Map<String, TextView> dropdownValues) {
        String psnError = exactDigitNumberError("PSN", values.get("psn"), 16);
        if (psnError != null) {
            return failField(textInputs, dropdownValues, "psn", psnError);
        }

        String dobError = numericDateError("Date of Birth", values.get("birth_date"));
        if (dobError != null) {
            return failField(textInputs, dropdownValues, "birth_date", dobError);
        }
        String issueError = numericDateError("Date of Issue", values.get("issue_date"));
        if (issueError != null) {
            return failField(textInputs, dropdownValues, "issue_date", issueError);
        }
        return true;
    }

    /**
     * Passport document rules: the shared 8-digit shapes for birth, issue and
     * expiry (number presence is covered by the generic required pass).
     * Fail-fast in field order, matching the other passes.
     */
    private boolean validatePassportFields(Map<String, String> values,
                                           Map<String, EditText> textInputs,
                                           Map<String, TextView> dropdownValues) {
        String dobError = numericDateError("Date of Birth", values.get("birth_date"));
        if (dobError != null) {
            return failField(textInputs, dropdownValues, "birth_date", dobError);
        }
        String issueError = numericDateError("Date of Issue", values.get("issue_date"));
        if (issueError != null) {
            return failField(textInputs, dropdownValues, "issue_date", issueError);
        }
        String expiryError = numericDateError("Date of Expiry", values.get("expiry_date"));
        if (expiryError != null) {
            return failField(textInputs, dropdownValues, "expiry_date", expiryError);
        }
        return true;
    }

    /**
     * Driver's License document rules: the shared 8-digit shapes for birth and
     * expiry plus numeric-only serial (number presence is covered by the
     * generic required pass). Fail-fast in field order, matching the passes.
     */
    private boolean validateDriversLicenseFields(Map<String, String> values,
                                                 Map<String, EditText> textInputs,
                                                 Map<String, TextView> dropdownValues) {
        String dobError = numericDateError("Date of Birth", values.get("birth_date"));
        if (dobError != null) {
            return failField(textInputs, dropdownValues, "birth_date", dobError);
        }
        String expiryError = numericDateError("Expiry Date", values.get("expiry_date"));
        if (expiryError != null) {
            return failField(textInputs, dropdownValues, "expiry_date", expiryError);
        }
        String serialRaw = trimmed(values.get("serial_no"));
        if (!serialRaw.isEmpty() && serialRaw.matches(".*[A-Za-z].*")) {
            return failField(textInputs, dropdownValues, "serial_no",
                    "Serial No. must contain numbers only (no letters)");
        }
        return true;
    }
    /**
     * SSS document rules: 10-digit numeric SS number plus the shared 8-digit
     * birth shape (other presence is covered by the generic required pass).
     * Fail-fast in field order, matching the other passes.
     */
    private boolean validateSssFields(Map<String, String> values,
                                      Map<String, EditText> textInputs,
                                      Map<String, TextView> dropdownValues) {
        String ssError = exactDigitNumberError("SS Number", values.get("ss_number"), 10);
        if (ssError != null) {
            return failField(textInputs, dropdownValues, "ss_number", ssError);
        }

        String dobError = numericDateError("Date of Birth", values.get("birth_date"));
        if (dobError != null) {
            return failField(textInputs, dropdownValues, "birth_date", dobError);
        }
        return true;
    }
    /**
     * Shared exact-length document-number rule (16-digit PSN, 12-digit TIN /
     * PhilHealth No., 10-digit SS number): no alphabet, exactly the expected
     * digit count. Returns the error message, or null when acceptable.
     */
    private static String exactDigitNumberError(String label, String rawValue, int digits) {
        String raw = trimmed(rawValue);
        if (raw.isEmpty()) {
            return label + " is required";
        }
        if (raw.matches(".*[A-Za-z].*")) {
            return label + " must contain numbers only (no letters)";
        }
        if (raw.replaceAll("\\D", "").length() != digits) {
            return label + " must be exactly " + digits + " digits";
        }
        return null;
    }

    /**
     * Numeric date-shape check: exactly 8 digits (YYYYMMDD) — the field caps
     * at 8 chars so dashes can't be typed. Empty is valid here — presence is
     * governed by the field's required flag, not format. No calendar math is
     * computed. Returns the error message, or null when acceptable.
     */
    private static String numericDateError(String label, String rawValue) {
        String raw = rawValue != null ? rawValue.trim() : "";
        if (raw.isEmpty()) {
            return null;
        }
        // Exactly 8 digits, nothing else. This single check rejects alphabet,
        // month names, dashes, and wrong lengths with one message.
        if (!raw.matches("\\d{8}")) {
            return label + " must be 8 digits (YYYYMMDD)";
        }
        return null;
    }

    /**
     * Flags one field invalid and returns false. Text inputs and dropdown
     * values both use setError plus focus, so every field — including Blood
     * Type — behaves exactly like Date of Birth. The toast survives only as a
     * last resort for a key bound to neither, which should never happen while
     * builders register every field.
     */
    private boolean failField(Map<String, EditText> textInputs, Map<String, TextView> dropdownValues,
                              String key, String message) {
        EditText input = textInputs.get(key);
        if (input != null) {
            input.setError(message);
            input.requestFocus();
            return false;
        }
        TextView value = dropdownValues != null ? dropdownValues.get(key) : null;
        if (value != null) {
            value.setError(message);
            value.requestFocus();
            return false;
        }
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
        return false;
    }

    /** Clears a dropdown's setError once the user picks or clears a value. */
    private static void hideFieldError(Map<String, TextView> dropdownValues, String key) {
        TextView value = dropdownValues != null ? dropdownValues.get(key) : null;
        if (value != null) {
            value.setError(null);
        }
    }

    private static String trimmed(String value) {
        return value != null ? value.trim() : "";
    }

    private void applyIdTypeSelection(int pos,
                                      LinearLayout formContainer,
                                      Map<String, String> draft, Map<String, EditText> textInputs,
                                      Map<String, TextView> dropdownValues,
                                      Runnable refreshPreview, LinearLayout dotsContainer,
                                      GovermentIdDesignAdapter designAdapter, int[] selectedType) {
        if (pos < 0 || pos >= IdTypeSpec.getTypeNames().length) {
            return;
        }
        selectedType[0] = pos;
        rebuildForm(formContainer, currentSpec(pos, true, null),
                draft, textInputs, dropdownValues, refreshPreview);
        if (dotsContainer != null && designAdapter != null) {
            setupDots(dotsContainer, designAdapter.getTypeCount(), pos);
        }
        refreshPreview.run();
    }

    private void setupDots(LinearLayout container, int count, int selected) {
        container.removeAllViews();
        int size = Ui.dp(this, 8);
        int margin = Ui.dp(this, 4);
        for (int i = 0; i < count; i++) {
            View dot = new View(GovermentIDActivity.this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot);
            dot.setAlpha(i == selected ? 1f : 0.3f);
            container.addView(dot);
        }
    }
}
