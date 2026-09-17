package com.akin.wallet;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.akin.wallet.adapter.SocialAccountAdapter;
import com.akin.wallet.db.AppDatabaseHelper;
import com.akin.wallet.model.CredentialItem;

/**
 * Social Account full screen — the only entry is dashboard View All.
 * Not connected to the bottom navigation. Creation sheets still open from
 * the dashboard FAB; edit sheets open here via the shared sheet helper.
 */
public class SocialAccountActivity extends AppCompatActivity {

    private SocialAccountAdapter adapter;
    private AppDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_account);

        dbHelper = new AppDatabaseHelper(this);

        RecyclerView recycler = findViewById(R.id.recycler_accounts);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SocialAccountAdapter(dbHelper.getAllLogins(), dbHelper);
        adapter.setOnCredentialActionListener(new SocialAccountAdapter.OnCredentialActionListener() {
            @Override
            public void onEdit(CredentialItem item) {
                startActivity(SocialAccountFormActivity.editIntent(
                        SocialAccountActivity.this, item));
            }

            @Override
            public void onDelete(CredentialItem item) {
                showDeleteConfirmation(item);
            }
        });
        recycler.setAdapter(adapter);

        EditText searchInput = findViewById(R.id.search_accounts);
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
    }

    private void refreshList() {
        if (adapter != null && dbHelper != null) {
            adapter.updateData(dbHelper.getAllLogins());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void showDeleteConfirmation(CredentialItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete " + item.getPlatform() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteLogin(item.getId());
                    adapter.updateData(dbHelper.getAllLogins());
                    Toast.makeText(this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
