package com.akin.wallet.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.akin.wallet.R;

/**
 * In-app legal screen behind Settings. Shows either the Privacy Policy or
 * the Terms of Service depending on {@link #EXTRA_TYPE}; both are static
 * offline-friendly texts bundled with the app.
 */
public class PolicyActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "extra_policy_type";
    public static final String TYPE_PRIVACY = "privacy";
    public static final String TYPE_TERMS = "terms";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView title = findViewById(R.id.policy_title);
        TextView body = findViewById(R.id.policy_body);

        if (TYPE_TERMS.equals(getIntent().getStringExtra(EXTRA_TYPE))) {
            title.setText(R.string.settings_terms);
            body.setText(R.string.policy_terms_body);
        } else {
            title.setText(R.string.settings_privacy);
            body.setText(R.string.policy_privacy_body);
        }
    }
}
