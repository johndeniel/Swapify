package com.akin.wallet;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.graphics.Color;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.akin.wallet.fragment.BankCardsFragment;
import com.akin.wallet.fragment.IdentificationFragment;
import com.akin.wallet.fragment.SocialLoginsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemNavigation();
        if (savedInstanceState == null) {
            loadFragment(new IdentificationFragment());
        }
        setupBottomNav();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemNavigation();
        }
    }

    private void hideSystemNavigation() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.parseColor("#FF0D1B2A"));
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_id) {
                fragment = new IdentificationFragment();
            } else if (id == R.id.nav_bank) {
                fragment = new BankCardsFragment();
            } else if (id == R.id.nav_login) {
                fragment = new SocialLoginsFragment();
            }
            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
