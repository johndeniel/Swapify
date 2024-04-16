package barter.swapify.features.swipe.presentation.widgets;

import android.annotation.SuppressLint;
import android.graphics.Canvas;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;

import barter.swapify.R;
import barter.swapify.features.swipe.domain.entity.SwipeEntity;

public class SwipeAdapter extends RecyclerView.Adapter<SwipeAdapter.SwipeHolder> {
    private static final String TAG = SwipeAdapter.class.getSimpleName();
    private static final float SWIPE_THRESHOLD = 0.5f;
    private final List<SwipeEntity> swipeEntityList;

    public SwipeAdapter(List<SwipeEntity> swipeEntityList) {
        this.swipeEntityList = swipeEntityList;
    }

    @Override
    public int getItemCount() {
        return swipeEntityList.isEmpty() ? 0 : 1; // Only one item is displayed at a time
    }

    @NonNull
    @Override
    public SwipeAdapter.SwipeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.swipe_content_media_card, parent, false);
        return new SwipeAdapter.SwipeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SwipeAdapter.SwipeHolder holder, int position) {
        SwipeEntity temp = swipeEntityList.get(position);

        // Reset view properties for the new item
        holder.itemView.setRotation(0);
        holder.itemView.setTranslationX(0);
        holder.itemView.setTranslationY(0);
        holder.itemView.setAlpha(1.0f);

        holder.bindItem(temp);
    }

    // Attaches swipe helper to the RecyclerView.
    public void attachSwipeHelperToRecyclerView(RecyclerView recyclerView) {
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
    }

    // Implement swipe left and swipe right
    private final ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN) {
        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return SWIPE_THRESHOLD;
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            // Customize the swipe animation
            final View itemView = viewHolder.itemView;
            final float itemHeight = (float) itemView.getHeight();
            final float itemWidth = (float) itemView.getWidth();

            // Calculate the rotation angle based on swipe distance
            float rotation = 0;
            if (dX != 0) {
                rotation = -dX / itemWidth * 45;
            }

            // Set up the rotation and translation effects
            itemView.setPivotX(itemWidth / 2);
            itemView.setPivotY(itemHeight / 2);
            itemView.setRotation(rotation);
            itemView.setTranslationX(dX);
            itemView.setTranslationY(dY);

            // Apply opacity based on swipe distance
            final float alpha = 1.0f - Math.abs(dX) / itemWidth;
            itemView.setAlpha(alpha);

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder view, int direction) {
            String swipeDirection;
            int position = view.getAdapterPosition();

            if (direction == ItemTouchHelper.LEFT) {
                swipeDirection = "Left";

            } else if (direction == ItemTouchHelper.RIGHT) {
                swipeDirection = "Right";

            } else if (direction == ItemTouchHelper.UP) {
                swipeDirection = "Up";

            } else {
                swipeDirection = "Down";
            }

            swipeEntityList.remove(position);
            notifyItemRemoved(position);
            Log.d(TAG, "Swiped " + swipeDirection);
        }
    };

    static class SwipeHolder extends RecyclerView.ViewHolder {
        private final ImageView mediaImageView;
        private final TextView titleTextView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final ShimmerFrameLayout shimmerFrameLayout;

        private SwipeHolder(@NonNull View view) {
            super(view);
            mediaImageView = view.findViewById(R.id.mediaImageView);
            titleTextView = view.findViewById(R.id.titleTextView);
            nameTextView = view.findViewById(R.id.nameTextView);
            descriptionTextView = view.findViewById(R.id.descriptionTextView);
            shimmerFrameLayout = view.findViewById(R.id.shimmer_view_container);
        }

        @SuppressLint("SetTextI18n")
        private void bindItem(@NonNull SwipeEntity item) {
            shimmerFrameLayout.startShimmer();
            titleTextView.setText(item.getTitle());
            nameTextView.setText("By: " + item.getFullName());
            descriptionTextView.setText(item.getDescription());
            Glide.with(itemView.getContext())
                    .load(item.getImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .placeholder(R.drawable.placeholder)
                    .listener(new GlideShimmer(shimmerFrameLayout))
                    .into(mediaImageView);
        }
    }
}