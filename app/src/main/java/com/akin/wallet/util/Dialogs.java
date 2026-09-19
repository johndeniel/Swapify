package com.akin.wallet.util;

import android.content.Context;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * M3 basic confirmation dialogs. Every destructive confirm in the app is
 * the same title + message + Delete/Cancel shape, previously copy-pasted
 * per form with an inconsistent builder import.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** "Delete <thing>?" confirm; runs {@code onDelete} on Delete. */
    public static void confirmDelete(Context context, String title, String message,
                                     Runnable onDelete) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Delete", (d, which) -> onDelete.run())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
