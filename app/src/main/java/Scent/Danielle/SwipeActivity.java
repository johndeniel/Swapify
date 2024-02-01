package Scent.Danielle;

// Android core components
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

// Firebase imports
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

// Java standard imports
import java.util.ArrayList;
import java.util.List;

// Custom imports from the project
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
        handleRetrieveItemsFromFirebase();

        // Attach swipe functionality to the RecyclerView
        itemAdapter.attachSwipeHelperToRecyclerView(itemRecyclerView);
        return rootView;
    }

    // Retrieve items from Firebase database and update the UI accordingly.
    private void handleRetrieveItemsFromFirebase() {

        // It follows a sequential approach:
        // 1. Retrieves all data from the database and stores it in a DataSnapshot.
        // 2. Iterates through each element of the DataSnapshot.
        // 3. Checks if the user ID associated with each item is equal to the current user's ID.
        // 4. If the IDs do not match, the item is added to the list of fetched items.

        // Overview:
        // Fetching and processing a large dataset of 1 million records incurs substantial time overhead.
        // Despite the exhaustive process, only 50,000 records meet the criteria, leading to the wastage of 950,000 records.

        // Memory Considerations:
        // Storing such a dataset can strain memory resources, risking exhaustion in resource-constrained environments.

        // Algorithmic Efficiency:
        // The linear time complexity results in longer processing times, aggravated by substantial unused fetched data, indicating suboptimal resource utilization.

        DatabaseReference itemsRef = FirebaseInitialization.getItemsDatabaseReference();
        String userId = FirebaseInitialization.getCurrentUserId();
        itemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    List<Item> fetchedItems = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Item item = snapshot.getValue(Item.class);
                        if (item != null && !item.getUserId().equals(userId)) {
                            fetchedItems.add(item);
                        }
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

    // Inner class for RecyclerView adapter for displaying feed items with swipe functionality.
    private class FeedItemListAdapter extends RecyclerView.Adapter<SwipeActivity.FeedItemDisplayHolder> {
        private final List<Item> itemList;
        private static final float SWIPE_THRESHOLD = 0.5f;

        public FeedItemListAdapter(List<Item> itemList) {
            this.itemList = itemList;
        }

        @Override
        public int getItemCount() {
            return itemList.isEmpty() ? 0 : 1; // Only one item is displayed at a time
        }

        @NonNull
        @Override
        public SwipeActivity.FeedItemDisplayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_swipe, parent, false);
            return new SwipeActivity.FeedItemDisplayHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SwipeActivity.FeedItemDisplayHolder holder, int position) {
            Item currentItem = itemList.get(position);
            holder.bindItem(currentItem);
        }

        // Implement swipe left and swipe right
        private final ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN) {
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
                Toast.makeText(requireActivity(), "Swiped " + swipeDirection, Toast.LENGTH_SHORT).show();
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

        // Attaches swipe helper to the RecyclerView.
        public void attachSwipeHelperToRecyclerView(RecyclerView recyclerView) {
            new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);
        }
    }

    private class FeedItemDisplayHolder extends RecyclerView.ViewHolder {
        private final ImageView mediaImageView;
        private final TextView titleTextView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;

        private FeedItemDisplayHolder(@NonNull View view) {
            super(view);
            mediaImageView = view.findViewById(R.id.mediaImageView);
            titleTextView = view.findViewById(R.id.titleTextView);
            nameTextView = view.findViewById(R.id.nameTextView);
            descriptionTextView = view.findViewById(R.id.descriptionTextView);
            Log.d(TAG, "ViewHolder created");
        }

        private void bindItem(@NonNull Item item) {
            titleTextView.setText(item.getTitle());
            nameTextView.setText("By: " + item.getFullName());
            descriptionTextView.setText(item.getDescription());
            Glide.with(itemView.getContext()).load(item.getImageUrl()).into(mediaImageView);
            Log.d(TAG, "Item bound: " + item.getTitle());
        }
    }
}