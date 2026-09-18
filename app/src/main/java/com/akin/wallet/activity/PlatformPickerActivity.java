package com.akin.wallet.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.R;
import com.akin.wallet.adapter.PlatformSelectionAdapter;
import com.akin.wallet.model.PlatformIcons;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Full-screen platform picker. Returns the chosen platform icon and name;
 * the caller keeps its own search/filter.
 */
public class PlatformPickerActivity extends AppCompatActivity {

    public static final String EXTRA_ICON_RES = "extra_icon_res";
    public static final String EXTRA_NAME = "extra_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_social_platform);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView recyclerPlatforms = findViewById(R.id.recycler_platforms);
        recyclerPlatforms.setLayoutManager(new LinearLayoutManager(this));
        // Catalog (icon + name + url) lives in PlatformIcons: single source of
        // truth shared with the name-based icon resolution used after reinstall.
        PlatformSelectionAdapter platformAdapter = new PlatformSelectionAdapter(PlatformIcons.catalog(),
                (iconRes, name, url) -> {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_ICON_RES, iconRes);
                    data.putExtra(EXTRA_NAME, name);
                    setResult(RESULT_OK, data);
                    finish();
                });
        recyclerPlatforms.setAdapter(platformAdapter);

        EditText searchPlatform = findViewById(R.id.search_platform);
        searchPlatform.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                platformAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}
