package com.akin.wallet.fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
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
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.IdCardDesignAdapter;
import com.akin.wallet.adapter.IdCardListAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.model.IdTypeSpec;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.LinkedHashMap;
import java.util.Map;

public class IdentificationFragment extends Fragment {

    private AppDatabaseHelper dbHelper;
    private IdCardListAdapter cardAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_identification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new AppDatabaseHelper(requireContext());

        RecyclerView recyclerCards = view.findViewById(R.id.recycler_cards);
        recyclerCards.setLayoutManager(new LinearLayoutManager(requireContext()));
        cardAdapter = new IdCardListAdapter(dbHelper.getAllIdCards(),
                new IdCardListAdapter.OnIdActionListener() {
                    @Override
                    public void onEdit(IdCardItem item) {
                        showCardDialog(item);
                    }

                    @Override
                    public void onDelete(IdCardItem item) {
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
            cardAdapter.updateData(dbHelper.getAllIdCards());
        }
    }

    private void showDeleteConfirmation(IdCardItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete ID")
                .setMessage("Are you sure you want to delete this " + item.getIdType() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteIdCard(item.getId());
                    cardAdapter.updateData(dbHelper.getAllIdCards());
                    Toast.makeText(requireContext(), "Deleted Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCardDialog(@Nullable IdCardItem existing) {
        final boolean isEdit = existing != null;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_add_government_id, null);
        dialog.setContentView(dialogView);

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setStatusBarColor(requireContext().getColor(R.color.dark_bg));
            }
            com.google.android.material.bottomsheet.BottomSheetDialog dialog2 =
                    (com.google.android.material.bottomsheet.BottomSheetDialog) dialogInterface;
            View bottomSheet = dialog2.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet == null) {
                return;
            }
            bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            bottomSheet.requestLayout();
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setHideable(false);
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                    .setDraggable(false);
        });

        TextView textIdType = dialogView.findViewById(R.id.text_id_type);
        LinearLayout formContainer = dialogView.findViewById(R.id.form_container);
        TextView btnSave = dialogView.findViewById(R.id.btn_save);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);

        // Draft holds EVERY typed value (even for hidden types) so switching
        // ID type back and forth never loses input; save filters to active spec.
        final Map<String, String> draftValues = new LinkedHashMap<>();
        if (isEdit) {
            draftValues.putAll(existing.getFields());
        }
        final Map<String, EditText> textInputs = new LinkedHashMap<>();

        final int[] selectedType = {0};
        // Fixed authentic design per ID type — no color picker. Column kept as 0.
        final int fixedDesign = 0;
        final boolean[] knownType = {true};

        if (isEdit) {
            dialogTitle.setText("Edit " + existing.getIdType());
            btnSave.setText("Update");
            if (IdTypeSpec.isKnownType(existing.getIdType())) {
                selectedType[0] = IdTypeSpec.indexOf(existing.getIdType());
            } else {
                knownType[0] = false;
                selectedType[0] = 0;
            }
        }

        final IdCardDesignAdapter[] adapterRef = new IdCardDesignAdapter[1];
        final RecyclerView[] carouselRef = new RecyclerView[1];
        final LinearLayout dotsContainer = dialogView.findViewById(R.id.dots_container);
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
                Toast.makeText(requireContext(),
                        "ID type is fixed for entries from a newer version", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pos != selectedType[0]) {
                applyIdTypeSelection(pos, textIdType, formContainer, draftValues, textInputs,
                        refreshPreview, dotsContainer, adapterRef[0], selectedType);
            } else if (carouselRef[0] != null) {
                carouselRef[0].smoothScrollToPosition(pos);
            }
        });
        adapterRef[0] = designAdapter;
        RecyclerView recyclerDesign = dialogView.findViewById(R.id.recycler_card_design);
        carouselRef[0] = recyclerDesign;
        LinearLayoutManager layoutManager =
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerDesign.setLayoutManager(layoutManager);
        recyclerDesign.setAdapter(designAdapter);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerDesign);

        setupDots(dotsContainer, designAdapter.getTypeCount(), selectedType[0]);

        recyclerDesign.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                View snapView = snapHelper.findSnapView(layoutManager);
                if (snapView != null) {
                    int pos = layoutManager.getPosition(snapView);
                    if (pos != RecyclerView.NO_POSITION && knownType[0] && pos != selectedType[0]) {
                        applyIdTypeSelection(pos, textIdType, formContainer, draftValues, textInputs,
                                refreshPreview, dotsContainer, adapterRef[0], selectedType);
                    }
                }
            }
        });
        final int scrollTo = selectedType[0];
        recyclerDesign.post(() -> recyclerDesign.scrollToPosition(scrollTo));

        // Initial header + form.
        if (isEdit && !knownType[0]) {
            textIdType.setText(existing.getIdType());
        } else {
            textIdType.setText(IdTypeSpec.getTypeNames()[selectedType[0]]);
        }
        rebuildForm(formContainer, currentSpec(selectedType[0], knownType[0],
                isEdit ? existing : null, draftValues), draftValues, textInputs, refreshPreview);
        refreshPreview.run();

        dialogView.findViewById(R.id.row_id_type).setOnClickListener(v -> {
            if (isEdit && !knownType[0]) {
                Toast.makeText(requireContext(),
                        "ID type is fixed for entries from a newer version", Toast.LENGTH_SHORT).show();
                return;
            }
            showChoiceDialog("ID Type", IdTypeSpec.getTypeNames(), selectedType[0], which -> {
                applyIdTypeSelection(which, textIdType, formContainer, draftValues, textInputs,
                        refreshPreview, dotsContainer, adapterRef[0], selectedType);
                if (carouselRef[0] != null) {
                    carouselRef[0].smoothScrollToPosition(which);
                }
            });
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

            if (!validate(spec, filtered, textInputs)) {
                return;
            }

            if (isEdit) {
                IdCardItem updated = new IdCardItem(
                        existing.getId(), existing.getIdType(), filtered, fixedDesign);
                // Known types may have been switched via selector; use the new name.
                if (knownType[0]) {
                    updated = new IdCardItem(existing.getId(), typeName, filtered, fixedDesign);
                }
                dbHelper.updateIdCard(updated);
                cardAdapter.updateData(dbHelper.getAllIdCards());
                Toast.makeText(requireContext(), "Updated Successfully", Toast.LENGTH_SHORT).show();
            } else {
                IdCardItem newCard = new IdCardItem(typeName, filtered, fixedDesign);
                dbHelper.insertIdCard(newCard);
                cardAdapter.updateData(dbHelper.getAllIdCards());
                Toast.makeText(requireContext(), "ID Saved", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // ---------- dynamic form ----------

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
                             Runnable onChanged) {
        container.removeAllViews();
        textInputs.clear();
        for (IdTypeSpec.IdField field : spec.fields) {
            if (!draft.containsKey(field.key)) {
                draft.put(field.key, "");
            }
            if (field.isDropdown()) {
                container.addView(buildDropdownField(field, draft, onChanged));
            } else {
                container.addView(buildTextField(field, draft, textInputs, onChanged));
            }
        }
    }

    private View buildTextField(IdTypeSpec.IdField field, Map<String, String> draft,
                                Map<String, EditText> textInputs, Runnable onChanged) {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapParams.topMargin = dp(16);
        wrap.setLayoutParams(wrapParams);

        TextView label = new TextView(requireContext());
        label.setText(field.required ? field.label + " *" : field.label);
        label.setTextColor(getResources().getColor(R.color.text_secondary, null));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wrap.addView(label);

        EditText input = new EditText(requireContext());
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = dp(8);
        input.setLayoutParams(inputParams);
        input.setBackgroundResource(R.drawable.bg_card);
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
                                    Runnable onChanged) {
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapParams.topMargin = dp(16);
        wrap.setLayoutParams(wrapParams);

        TextView label = new TextView(requireContext());
        label.setText(field.required ? field.label + " *" : field.label);
        label.setTextColor(getResources().getColor(R.color.text_secondary, null));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        wrap.addView(label);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);
        row.setBackgroundResource(R.drawable.bg_card);
        int h = dp(16);
        int padV = dp(14);
        row.setPadding(h, padV, h, padV);
        row.setClickable(true);
        row.setFocusable(true);

        TextView valueView = new TextView(requireContext());
        LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        valueView.setLayoutParams(valueParams);
        valueView.setSingleLine(true);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        valueView.setTextColor(getResources().getColor(R.color.text_primary, null));

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

        ImageView chevron = new ImageView(requireContext());
        chevron.setImageResource(R.drawable.ic_dropdown);
        LinearLayout.LayoutParams chevParams = new LinearLayout.LayoutParams(dp(20), dp(20));
        chevron.setLayoutParams(chevParams);
        chevron.setContentDescription("Select " + field.label);

        row.addView(valueView);
        row.addView(chevron);

        row.setOnClickListener(v -> {
            int checked = indexOfOption(field.options, draft.get(field.key));
            new AlertDialog.Builder(requireContext())
                    .setTitle(field.label)
                    .setSingleChoiceItems(field.options, checked, (d, which) -> {
                        String picked = field.options[which];
                        draft.put(field.key, picked);
                        valueView.setText(picked);
                        valueView.setAlpha(1f);
                        onChanged.run();
                        d.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .setNeutralButton("Clear", (d, which) -> {
                        draft.put(field.key, "");
                        valueView.setText("Select");
                        valueView.setAlpha(0.4f);
                        onChanged.run();
                    })
                    .show();
        });

        wrap.addView(row);
        return wrap;
    }

    private boolean validate(IdTypeSpec.IdType spec, Map<String, String> values,
                             Map<String, EditText> textInputs) {
        for (IdTypeSpec.IdField f : spec.fields) {
            String v = values.get(f.key);
            if (v == null) {
                v = "";
            }
            v = v.trim();
            if (f.required && v.isEmpty()) {
                EditText input = textInputs.get(f.key);
                if (input != null) {
                    input.setError(f.label + " is required");
                    input.requestFocus();
                } else {
                    Toast.makeText(requireContext(), f.label + " is required", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
            if (f.sensitive && !v.isEmpty() && v.replaceAll("[^A-Za-z0-9]", "").length() < 4) {
                EditText input = textInputs.get(f.key);
                if (input != null) {
                    input.setError(f.label + " looks too short");
                    input.requestFocus();
                } else {
                    Toast.makeText(requireContext(), f.label + " looks too short", Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }
        return true;
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

    // ---------- shared UI helpers (mirrors BankCardsFragment) ----------

    /** Single choke point for ID-type switches (carousel, tap, or dropdown). */
    private void applyIdTypeSelection(int pos, TextView textIdType, LinearLayout formContainer,
                                      Map<String, String> draft, Map<String, EditText> textInputs,
                                      Runnable refreshPreview, LinearLayout dotsContainer,
                                      IdCardDesignAdapter designAdapter, int[] selectedType) {
        if (pos < 0 || pos >= IdTypeSpec.getTypeNames().length) {
            return;
        }
        selectedType[0] = pos;
        textIdType.setText(IdTypeSpec.getTypeNames()[pos]);
        rebuildForm(formContainer, currentSpec(pos, true, null, draft),
                draft, textInputs, refreshPreview);
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
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_dot);
            dot.setAlpha(i == selected ? 1f : 0.3f);
            container.addView(dot);
        }
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

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private interface OnChoiceListener {
        void onChoice(int which);
    }
}
