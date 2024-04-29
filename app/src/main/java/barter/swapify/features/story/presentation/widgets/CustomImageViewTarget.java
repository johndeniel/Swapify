package barter.swapify.features.story.presentation.widgets;

import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class CustomImageViewTarget extends CustomTarget<Drawable> {

    private final LinearLayout layout;

    public CustomImageViewTarget(LinearLayout layout) {
        this.layout = layout;
    }

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
        layout.setBackground(resource);  // Set the loaded drawable as background
    }

    @Override
    public void onLoadCleared(Drawable placeholder) {
        // Handle clearing the resource if needed (optional)
    }
}