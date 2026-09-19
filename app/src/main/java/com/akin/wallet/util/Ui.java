package com.akin.wallet.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.StringRes;

import com.google.android.material.snackbar.Snackbar;

/**
 * Stateless UI helpers shared by the form activities. Each replaces 3+
 * near-identical private copies that had already started to drift.
 */
public final class Ui {

    private Ui() {
    }

    /** Density-independent pixels for programmatic layouts and dividers. */
    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** Eye-icon flip for password/PIN/CVV fields, preserving cursor position. */
    public static void togglePasswordVisibility(EditText input) {
        int start = input.getSelectionStart();
        int end = input.getSelectionEnd();
        if (input.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
            input.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        int length = input.getText().length();
        input.setSelection(Math.min(Math.max(start, 0), length),
                Math.min(Math.max(end, 0), length));
    }

    /**
     * Case-insensitive option lookup for select dialogs (card type/network,
     * ID dropdowns). Returns -1 when absent so callers apply their own
     * fallback (first item, stored index, …).
     */
    public static int indexOfIgnoreCase(String[] options, String value) {
        if (options == null || value == null) {
            return -1;
        }
        String needle = value.trim();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(needle)) {
                return i;
            }
        }
        return -1;
    }

    // -- Return-screen messages (M3 Snackbar routing) --------------------
    // Form confirms finish() immediately, which would kill a Snackbar shown
    // there unseen. Stash the message instead; the return target shows it in
    // onResume (dashboard for vault forms, settings for the PIN change).
    private static int pendingMessage;

    public static void notifyOnReturn(@StringRes int messageRes) {
        pendingMessage = messageRes;
    }

    /** Takes the stashed message, if any (0 = none). */
    public static int takePendingMessage() {
        int message = pendingMessage;
        pendingMessage = 0;
        return message;
    }

    /**
     * Shows the stashed return-screen message, if any. Forms finish()
     * immediately, which would kill a Snackbar shown there unseen — the
     * return target calls this in onResume instead.
     */
    public static void showPendingMessage(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            takePendingMessage();
            return;
        }
        int message = takePendingMessage();
        if (message != 0) {
            View content = activity.findViewById(android.R.id.content);
            if (content != null) {
                Snackbar.make(content, message, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    /** Installed version name for About/version footers (falls back to 1.0). */
    public static String versionName(Context context) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                return context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.PackageInfoFlags.of(0)).versionName;
            }
            return legacyVersionName(context);
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return "1.0";
        }
    }

    @SuppressWarnings("deprecation")
    private static String legacyVersionName(Context context)
            throws PackageManager.NameNotFoundException {
        return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
    }
}
