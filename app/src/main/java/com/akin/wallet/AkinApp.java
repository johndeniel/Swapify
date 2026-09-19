package com.akin.wallet;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Locks the whole app to the dark theme the vault UI is built for. System
 * light mode otherwise leaks light M3 overlays (dialogs, menus, snackbars)
 * over the navy screens, so night mode is pinned regardless of the toggle.
 */
public class AkinApp extends Application {

    @Override
    public void onCreate() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate();
    }
}
