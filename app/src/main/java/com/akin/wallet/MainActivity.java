package com.akin.wallet;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsetsController;
import android.graphics.Color;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.akin.wallet.fragment.DashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hideSystemNavigation();
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
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
        getWindow().setStatusBarColor(getColor(R.color.dashboard_bg_start));
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
        bottomNav = findViewById(R.id.bottom_nav);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = fragmentFor(item.getItemId());
            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });
    }

    /** Dashboard links call this. Logins opens as a standalone screen. */
    public void navigateToTab(int itemId) {
        if (itemId == R.id.nav_social_account) {
            startActivity(new Intent(this, SocialAccountActivity.class));
            return;
        }
        Fragment fragment = fragmentFor(itemId);
        if (fragment == null) {
            return;
        }
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(itemId);
        } else {
            loadFragment(fragment);
        }
    }

    private Fragment fragmentFor(int id) {
        if (id == R.id.nav_home) {
            return new DashboardFragment();
        }
        return null;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
