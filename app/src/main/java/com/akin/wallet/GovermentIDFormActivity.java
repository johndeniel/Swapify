package com.akin.wallet;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.IdCardDesignAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Government ID creation/edit form as a full screen (replaces the old bottom
 * sheet). Add mode when no ID is passed; edit mode otherwise. The type-picker
 * carousel lives in the form content; dropdowns stay alert dialogs. Callers
 * refresh in onResume; RESULT_OK is set on save.
 */
public class GovermentIDFormActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "extra_id";
    public static final String EXTRA_TYPE = "extra_type";
    public static final String EXTRA_FIELDS_JSON = "extra_fields_json";
    public static final String EXTRA_DESIGN = "extra_design";
    public static final String EXTRA_CREATED_AT = "extra_created_at";
    public static final String EXTRA_UPDATED_AT = "extra_updated_at";

    /**
     * Intent that opens this form to edit an existing ID. Mirrors the bank and
     * social factories: one packing site per form. Audit timestamps ride as
     * top-level extras (never inside fields_json) so the edit round-trip
     * preserves creation order.
     */
    public static Intent editIntent(@NonNull Context context, @NonNull IdCardItem item) {
        Intent edit = new Intent(context, GovermentIDFormActivity.class);
        edit.putExtra(EXTRA_ID, item.getId());
        edit.putExtra(EXTRA_TYPE, item.getIdType());
        edit.putExtra(EXTRA_FIELDS_JSON, item.getFieldsJson());
        edit.putExtra(EXTRA_DESIGN, item.getDesign());
        edit.putExtra(EXTRA_CREATED_AT, item.getCreatedAt());
        edit.putExtra(EXTRA_UPDATED_AT, item.getUpdatedAt());
        return edit;
    }

    private AppDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goverment_id_form);

        dbHelper = new AppDatabaseHelper(this);

        // Back chevron, same as the bank card form: plain finish, no save.
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        int id = getIntent().getIntExtra(EXTRA_ID, -1);
        if (id == -1) {
            bindForm(null);
        } else {
            String json = getIntent().getStringExtra(EXTRA_FIELDS_JSON);
            Map<String, String> fields = json != null
                    ? IdCardItem.parseFieldsJson(json)
                    : new LinkedHashMap<>();
            // Timestamps arrive as sibling extras (same level as id), never
            // parsed from the JSON blob; 0 is the default when absent.
            bindForm(new IdCardItem(
                    id,
                    getIntent().getStringExtra(EXTRA_TYPE),
                    fields,
                    getIntent().getIntExtra(EXTRA_DESIGN, 0),
                    getIntent().getLongExtra(EXTRA_CREATED_AT, 0),
                    getIntent().getLongExtra(EXTRA_UPDATED_AT, 0)));
        }
    }

    private void bindForm(@Nullable IdCardItem existing) {
        final boolean isEdit = existing != null;


        LinearLayout formContainer = findViewById(R.id.form_container);
        // Action row mirrors the bank card form: btn_save is the outline
        // container, text_save_label carries the Save/Update caption.
        View btnSave = findViewById(R.id.btn_save);
        TextView textSaveLabel = findViewById(R.id.text_save_label);

        // Draft holds EVERY typed value (even for hidden types) so switching
        // ID type back and forth never loses input; save filters to active spec.
        final Map<String, String> draftValues = new LinkedHashMap<>();
        if (isEdit) {
            draftValues.putAll(existing.getFields());
        }
        final Map<String, EditText> textInputs = new LinkedHashMap<>();
        // Dropdown value texts, registered for setError like text inputs so a
        // failed save flags the value itself — same behavior as Date of Birth.
        final Map<String, TextView> dropdownValues = new LinkedHashMap<>();

        final int[] selectedType = {0};
        // Fixed authentic design per ID type — no color picker. Column kept as 0.
        final int fixedDesign = 0;
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

        final IdCardDesignAdapter[] adapterRef = new IdCardDesignAdapter[1];
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
        IdCardDesignAdapter designAdapter = new IdCardDesignAdapter(pos -> {
            if (isEdit && !knownType[0]) {
                Toast.makeText(GovermentIDFormActivity.this,
                        "ID type is fixed for entries from a newer version", Toast.LENGTH_SHORT).show();
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
                new LinearLayoutManager(GovermentIDFormActivity.this, LinearLayoutManager.HORIZONTAL, false);
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
                isEdit ? existing : null, draftValues), draftValues, textInputs, dropdownValues,
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
                    isEdit ? existing : null, draftValues);

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
                IdCardItem updated = new IdCardItem(
                        existing.getId(), existing.getIdType(), filtered, fixedDesign,
                        existing.getCreatedAt(), 0);
                // Known types may have been switched via selector; use the new name.
                if (knownType[0]) {
                    updated = new IdCardItem(existing.getId(), typeName, filtered, fixedDesign,
                            existing.getCreatedAt(), 0);
                }
                dbHelper.updateIdCard(updated);
                Toast.makeText(GovermentIDFormActivity.this, "Updated Successfully", Toast.LENGTH_SHORT).show();
            } else {
                IdCardItem newCard = new IdCardItem(typeName, filtered, fixedDesign);
                dbHelper.insertIdCard(newCard);
                Toast.makeText(GovermentIDFormActivity.this, "ID Saved", Toast.LENGTH_SHORT).show();
            }
            setResult(RESULT_OK);
            finish();
        });

        // Delete lives on this screen (no IDs tab), shown in-row in edit mode
        // only — same placement and outline-red style as the bank card form.
        // Dialog copy and toasts stay ID-specific; only the design is shared.
        View btnDelete = findViewById(R.id.btn_delete);
        if (isEdit) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(GovermentIDFormActivity.this)
                    .setTitle("Delete ID")
                    .setMessage("Are you sure you want to delete this "
                            + existing.getIdType() + "?")
                    .setPositiveButton("Delete", (d, which) -> {
                        dbHelper.deleteIdCard(existing.getId());
                        setResult(RESULT_OK);
                        finish();
                        Toast.makeText(GovermentIDFormActivity.this,
                                "Deleted Successfully", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
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
                                          @Nullable IdCardItem existing,
                                          Map<String, String> draft) {
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
        LinearLayout row = new LinearLayout(GovermentIDFormActivity.this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(16);
        row.setLayoutParams(rowParams);

        View left = buildFieldView(first, draft, textInputs, dropdownValues, onChanged);
        LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftParams.setMarginEnd(dp(6));
        left.setLayoutParams(leftParams);

        View right = buildFieldView(second, draft, textInputs, dropdownValues, onChanged);
        LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rightParams.setMarginStart(dp(6));
        right.setLayoutParams(rightParams);

        row.addView(left);
        row.addView(right);
        return row;
    }

    private View buildTextField(IdTypeSpec.IdField field, Map<String, String> draft,
                                Map<String, EditText> textInputs, Runnable onChanged) {
        LinearLayout wrap = new LinearLayout(GovermentIDFormActivity.this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapParams.topMargin = dp(16);
        wrap.setLayoutParams(wrapParams);

        TextView label = new TextView(GovermentIDFormActivity.this);
        label.setText(field.required ? field.label + " *" : field.label);
        label.setTextColor(getResources().getColor(R.color.dashboard_muted, null));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wrap.addView(label);

        EditText input = new EditText(GovermentIDFormActivity.this);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = dp(8);
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
        int h = dp(16);
        int v = dp(14);
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
        LinearLayout wrap = new LinearLayout(GovermentIDFormActivity.this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapParams.topMargin = dp(16);
        wrap.setLayoutParams(wrapParams);

        TextView label = new TextView(GovermentIDFormActivity.this);
        label.setText(field.required ? field.label + " *" : field.label);
        label.setTextColor(getResources().getColor(R.color.dashboard_muted, null));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wrap.addView(label);

        LinearLayout row = new LinearLayout(GovermentIDFormActivity.this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);
        row.setBackgroundResource(R.drawable.bg_dashboard_card);
        int h = dp(16);
        int padV = dp(14);
        row.setPadding(h, padV, h, padV);
        row.setClickable(true);
        row.setFocusable(true);

        TextView valueView = new TextView(GovermentIDFormActivity.this);
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

        ImageView chevron = new ImageView(GovermentIDFormActivity.this);
        chevron.setImageResource(R.drawable.ic_dropdown);
        LinearLayout.LayoutParams chevParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        chevron.setLayoutParams(chevParams);
        chevron.setContentDescription("Select " + field.label);

        row.addView(valueView);
        row.addView(chevron);

        row.setOnClickListener(v -> {
            int checked = indexOfOption(field.options, draft.get(field.key));
            new AlertDialog.Builder(GovermentIDFormActivity.this)
                    .setTitle(field.label)
                    .setSingleChoiceItems(field.options, checked, (d, which) -> {
                        String picked = field.options[which];
                        draft.put(field.key, picked);
                        valueView.setText(picked);
                        valueView.setAlpha(1f);
                        hideFieldError(dropdownValues, field.key);
                        onChanged.run();
                        d.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Clear", (d, which) -> {
                        draft.put(field.key, "");
                        valueView.setText("Select");
                        valueView.setAlpha(0.4f);
                        hideFieldError(dropdownValues, field.key);
                        onChanged.run();
                    })
                    .show();
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
        Toast.makeText(GovermentIDFormActivity.this, message, Toast.LENGTH_SHORT).show();
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

    private static int indexOfOption(String[] options, String value) {
        if (options == null || value == null) {
            return -1;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(value.trim())) {
                return i;
            }
        }
        return -1;
    }

    private void applyIdTypeSelection(int pos,
                                      LinearLayout formContainer,
                                      Map<String, String> draft, Map<String, EditText> textInputs,
                                      Map<String, TextView> dropdownValues,
                                      Runnable refreshPreview, LinearLayout dotsContainer,
                                      IdCardDesignAdapter designAdapter, int[] selectedType) {
        if (pos < 0 || pos >= IdTypeSpec.getTypeNames().length) {
            return;
        }
        selectedType[0] = pos;
        rebuildForm(formContainer, currentSpec(pos, true, null, draft),
                draft, textInputs, dropdownValues, refreshPreview);
        if (dotsContainer != null && designAdapter != null) {
            setupDots(dotsContainer, designAdapter.getTypeCount(), pos);
        }
        refreshPreview.run();
    }

    private void setupDots(LinearLayout container, int count, int selected) {
        container.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        int margin = (int) (4 * density);
        for (int i = 0; i < count; i++) {
            View dot = new View(GovermentIDFormActivity.this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot);
            dot.setAlpha(i == selected ? 1f : 0.3f);
            container.addView(dot);
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }
}
