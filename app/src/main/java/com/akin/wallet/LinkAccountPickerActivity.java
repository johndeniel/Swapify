package com.akin.wallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.LinkedAccountAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.CredentialItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen account picker for associating accounts (replaces the old bottom
 * sheet). Shows only the IDs passed in; returns the picked account id.
 */
public class LinkAccountPickerActivity extends AppCompatActivity {

    public static final String EXTRA_AVAILABLE_IDS = "extra_available_ids";
    public static final String EXTRA_ACCOUNT_ID = "extra_account_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_social_account);

        int[] ids = getIntent().getIntArrayExtra(EXTRA_AVAILABLE_IDS);

        AppDatabaseHelper db = new AppDatabaseHelper(this);
        List<CredentialItem> shown = new ArrayList<>();
        if (ids != null) {
            List<CredentialItem> all = db.getAllLogins();
            for (int id : ids) {
                for (CredentialItem c : all) {
                    if (c.getId() == id) {
                        shown.add(c);
                        break;
                    }
                }
            }
        }

        RecyclerView recyclerAccounts = findViewById(R.id.recycler_accounts);
        recyclerAccounts.setLayoutManager(new LinearLayoutManager(this));
        LinkedAccountAdapter linkAdapter = new LinkedAccountAdapter(shown, false, (pickedItem, isRemove) -> {
            Intent data = new Intent();
            data.putExtra(EXTRA_ACCOUNT_ID, pickedItem.getId());
            setResult(RESULT_OK, data);
            finish();
        });
        recyclerAccounts.setAdapter(linkAdapter);

        // Search mirrors the platform picker: case-insensitive filter on tap.
        EditText searchAccounts = findViewById(R.id.search_accounts);
        searchAccounts.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                linkAdapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
}
