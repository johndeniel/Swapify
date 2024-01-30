package Scent.Danielle;

// Android core components
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX components
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

// AndroidX recyclerview imports
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Third-party library for image loading
import com.bumptech.glide.Glide;

// Google Sign-In API imports

// Google Material imports
import com.google.android.material.button.MaterialButton;

// Firebase imports
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

// Java standard imports
import java.util.ArrayList;
import java.util.List;

// Custom class imports
import Scent.Danielle.Utils.Database.FirebaseInitialization;
import Scent.Danielle.Utils.DataModel.Item;

public class SwipeActivity extends Fragment {
    private final String TAG = SwipeActivity.class.getSimpleName();
    private FeedItemListAdapter itemAdapter;
    private List<Item> itemList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_swipe, container, false);
        RecyclerView itemRecyclerView = rootView.findViewById(R.id.itemRecyclerView);
        itemRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        itemList = new ArrayList<>();
        itemAdapter = new FeedItemListAdapter(itemList);
        itemRecyclerView.setAdapter(itemAdapter);
        loadItemsFromFirebase();

        // Attach swipe functionality to the RecyclerView
        itemAdapter.attachSwipeHelperToRecyclerView(itemRecyclerView);
        return rootView;
    }


    private void loadItemsFromFirebase() {
        FirebaseInitialization.getItemsDatabaseReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    List<Item> fetchedItems = new ArrayList<>();

                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        fetchedItems.add(snapshot.getValue(Item.class));
                    }

                    if (!fetchedItems.isEmpty() && itemAdapter != null) {
                        itemList.clear();
                        itemList.addAll(fetchedItems);
                        itemAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Items retrieved and UI updated successfully.");
                    } else {
                        Log.d(TAG, "No items to display or adapter is unavailable.");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing data", e);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error: " + databaseError.getMessage(), databaseError.toException());
            }
        });
    }


    public class FeedItemListAdapter extends RecyclerView.Adapter<SwipeActivity.FeedItemDisplayHolder> {
        private final List<Item> itemList;
        private static final float SWIPE_THRESHOLD = 0.5f;

        public FeedItemListAdapter(List<Item> itemList) {
            this.itemList = itemList;
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        @NonNull
        @Override
        public SwipeActivity.FeedItemDisplayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
            return new SwipeActivity.FeedItemDisplayHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SwipeActivity.FeedItemDisplayHolder holder, int position) {
            Item currentItem = itemList.get(position);
            holder.bindItem(currentItem);
        }

        // Implement swipe left and swipe right
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Determine swipe direction
                String swipeDirection;
                if (direction == ItemTouchHelper.LEFT) {
                    swipeDirection = "Left";
                } else if (direction == ItemTouchHelper.RIGHT) {
                    swipeDirection = "Right";
                } else if (direction == ItemTouchHelper.UP) {
                    swipeDirection = "Up";
                } else {
                    swipeDirection = "Down";
                }

                // Handle swipe left or right
                int position = viewHolder.getAdapterPosition();
                itemList.remove(position);
                notifyItemRemoved(position);

                // Perform actions based on swipe direction
                Toast.makeText(requireActivity(),"Swiped " + swipeDirection, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Swiped " + swipeDirection);
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return SWIPE_THRESHOLD;
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
        };

        public void attachSwipeHelperToRecyclerView(RecyclerView recyclerView) {
            new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
        }
    }


    private class FeedItemDisplayHolder extends RecyclerView.ViewHolder {
        ImageView mediaImageView;
        TextView titleTextView, nameTextView, descriptionTextView;
        MaterialButton messageButton, wishListButton;

        private FeedItemDisplayHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            messageButton = itemView.findViewById(R.id.messageButton);
            wishListButton = itemView.findViewById(R.id.wishListButton);
            Log.d(TAG, "ViewHolder created");
        }

        private void bindItem(Item currentItem) {
            titleTextView.setText(currentItem.getTitle());
            nameTextView.setText("By: " + currentItem.getFullName());
            descriptionTextView.setText(currentItem.getDescription());
            Glide.with(itemView.getContext()).load(currentItem.getImageUrl()).into(mediaImageView);
            messageButton.setOnClickListener(v -> showToast(itemView.getContext(), "Message"));
            wishListButton.setOnClickListener(v -> showToast(itemView.getContext(), "Wish List"));
            Log.d(TAG, "Item bound: " + currentItem.getTitle());
        }
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}