package barter.swapify.features.settings.presentation.pages;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import barter.swapify.R;
import barter.swapify.features.auth.presentation.pages.LogoutPage;

public class SettingsPage extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private Button logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_presentation_settings_page);
        hideNavigationBar();

        logout = findViewById(R.id.logout_setting);

        toolbar = findViewById(R.id.settings_top_app_bar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsPage.this, LogoutPage.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }


}