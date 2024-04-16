package barter.swapify.features.swipe.presentation.widgets;

import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerFrameLayout;

public class GlideShimmer implements RequestListener<Drawable> {
    private final ShimmerFrameLayout shimmerFrameLayout;

    public GlideShimmer(ShimmerFrameLayout shimmerFrameLayout) {
        this.shimmerFrameLayout = shimmerFrameLayout;
        setShimmerEffect();
    }

    private void setShimmerEffect() {
        shimmerFrameLayout.setShimmer(
                new Shimmer.AlphaHighlightBuilder()
                        .setBaseAlpha(0.1f)
                        .setHighlightAlpha(0.9f)
                        .setDuration(1000)
                        .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                        .build()
        );
    }

    @Override
    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
        shimmerFrameLayout.stopShimmer();
        return false;
    }

    @Override
    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setShimmer(
                new Shimmer.AlphaHighlightBuilder()
                        .setBaseAlpha(1f)
                        .build());
        return false;
    }
}
