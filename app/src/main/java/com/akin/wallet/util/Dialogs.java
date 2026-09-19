package com.akin.wallet.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * M3 basic confirmation dialogs. Every destructive confirm in the app is
 * the same title + message + Delete/Cancel shape, previously copy-pasted
 * per form with an inconsistent builder import.
 *
 * <p>Builders return the shown dialog so owners can dismiss it in
 * onDestroy — otherwise a rotation with a dialog up leaks the window.
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** "Delete <thing>?" confirm; runs {@code onDelete} on Delete. */
    public static AlertDialog confirmDelete(Context context, String title, String message,
                                            Runnable onDelete) {
        return new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Delete", (d, which) -> onDelete.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Single-choice option picker shared by the bank and ID dropdowns. */
    public interface OnChoice {
        void onChoice(int which);
    }

    public static AlertDialog singleChoice(Context context, String title, String[] options,
                                           int checked, @NonNull OnChoice onChoice) {
        return new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setSingleChoiceItems(options, checked, (d, which) -> {
                    onChoice.onChoice(which);
                    d.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
