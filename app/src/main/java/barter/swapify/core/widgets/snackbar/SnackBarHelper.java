package barter.swapify.core.widgets.snackbar;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

public class SnackBarHelper {
    public static void invoke(@NonNull String message, @NonNull View view) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
}
