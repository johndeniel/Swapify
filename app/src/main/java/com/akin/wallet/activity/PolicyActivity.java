package com.akin.wallet.activity;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.akin.wallet.R;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * In-app legal screen behind Settings. Shows the Privacy Policy, Terms of
 * Service or About page depending on {@link #EXTRA_TYPE}. The bundled text
 * is sectioned (ALL-CAPS heading line, then body, blank line apart) and each
 * section renders as an accent-blue heading over light body copy. The About
 * page stamps the installed version name at the end.
 */
public class PolicyActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "extra_policy_type";
    public static final String TYPE_PRIVACY = "privacy";
    public static final String TYPE_TERMS = "terms";
    public static final String TYPE_ABOUT = "about";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_policy);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        LinearLayout sections = findViewById(R.id.policy_sections);

        String body;
        if (TYPE_TERMS.equals(getIntent().getStringExtra(EXTRA_TYPE))) {
            toolbar.setTitle(R.string.settings_terms);
            body = getString(R.string.policy_terms_body);
        } else if (TYPE_ABOUT.equals(getIntent().getStringExtra(EXTRA_TYPE))) {
            toolbar.setTitle(R.string.settings_about);
            body = getString(R.string.policy_about_body)
                    .replace("{version}", versionName());
        } else {
            toolbar.setTitle(R.string.settings_privacy);
            body = getString(R.string.policy_privacy_body);
        }
        renderSections(sections, body);
    }

    /** Installed version stamped on the About page (falls back to 1.0). */
    private String versionName() {
        try {
            return getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    /** Splits "HEADING\nbody\n\n..." into styled heading + body view pairs. */
    private void renderSections(LinearLayout container, String body) {
        if (body == null || body.trim().isEmpty()) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        boolean first = true;
        for (String chunk : body.split("\n\n")) {
            String section = chunk.trim();
            if (section.isEmpty()) {
                continue;
            }
            int cut = section.indexOf('\n');
            String heading = cut == -1 ? section : section.substring(0, cut).trim();
            String text = cut == -1 ? "" : section.substring(cut + 1).trim();

            TextView headingView = new TextView(this);
            headingView.setText(heading);
            headingView.setTextColor(getColor(R.color.dashboard_active));
            headingView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            headingView.setTypeface(headingView.getTypeface(), android.graphics.Typeface.BOLD);
            headingView.setLetterSpacing(0.06f);
            LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            headingParams.topMargin = first ? 0 : (int) (20 * density);
            headingView.setLayoutParams(headingParams);
            container.addView(headingView);

            if (!text.isEmpty()) {
                TextView bodyView = new TextView(this);
                bodyView.setText(text);
                bodyView.setTextColor(getColor(R.color.text_subtle_light));
                bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                bodyView.setLineSpacing(0, 1.25f);
                LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                bodyParams.topMargin = (int) (6 * density);
                bodyView.setLayoutParams(bodyParams);
                container.addView(bodyView);
            }
            first = false;
        }
    }
}
