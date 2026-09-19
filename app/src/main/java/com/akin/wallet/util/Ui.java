package com.akin.wallet.util;

import android.content.Context;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;

import androidx.annotation.StringRes;

/**
 * Stateless UI helpers shared by the form activities. Each replaces 3+
 * near-identical private copies that had already started to drift.
 */
public final class Ui {

    private Ui() {
    }

    /** Density-independent pixels for programmatic layouts and dividers. */
    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    /** Eye-icon flip for password/PIN/CVV fields without losing the cursor. */
    public static void togglePasswordVisibility(EditText input) {
        if (input.getTransformationMethod() == PasswordTransformationMethod.getInstance()) {
            input.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        input.setSelection(input.getText().length());
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
}
