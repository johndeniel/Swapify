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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
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

    private final class NonScrollableLayoutManager extends LinearLayoutManager {
        @Override
        public boolean canScrollVertically() {
            return false;
        }
        public NonScrollableLayoutManager() { super(requireContext()); }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_swipe, container, false);
        RecyclerView itemRecyclerView = rootView.findViewById(R.id.itemRecyclerView);
        itemRecyclerView.setLayoutManager(new NonScrollableLayoutManager());

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
        DatabaseReference itemsRef = FirebaseInitialization.getItemsDatabaseReference();
        String userId = FirebaseInitialization.getCurrentUserId();

        itemsRef.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if(task.isSuccessful()){
                    try {
                        DataSnapshot snapshot = task.getResult();
                        List<Item> fetchedItems = new ArrayList<>();

                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            Item item = childSnapshot.getValue(Item.class);
                            DatabaseReference ref = itemsRef.child(item.getKey()).child("swipe");
                            if (item != null && !item.getUserId().equals(userId) && !isSwipedByCurrentUser(ref, userId)) {
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
                }else{
                    Log.e(TAG, "Failed to fetch items: ", task.getException());
                }
            }
        });
    }

    private boolean isSwipedByCurrentUser(DatabaseReference itemSnapshot, String currentUserId) {
        Query query = itemSnapshot.orderByChild("userId").equalTo(currentUserId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Log the dataSnapshot
                Log.d(TAG, "Snapshot: " + dataSnapshot);

                if (dataSnapshot.exists()) {
                   // data is existing
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error querying swipe data: " + databaseError.getMessage());
            }
        });

        return false;
    }


    private final static class Swipe {
        private String userId;
        private boolean like;

        public Swipe() {}

        public Swipe(String userId, boolean like) {
            this.userId = userId;
            this.like = like;
        }

        public String getUserId() {
            return userId;
        }

        public boolean isLike() {
            return like;
        }
    }

    // Inner class for RecyclerView adapter for displaying feed items with swipe functionality.
    private final class FeedItemListAdapter extends RecyclerView.Adapter<SwipeActivity.FeedItemDisplayHolder> {
        private final List<Item> itemList;
        private String key;
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
            key = currentItem.getKey();
        }

        // Implement swipe left and swipe right
        private final ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT | ItemTouchHelper.UP | ItemTouchHelper.DOWN) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder view, int direction) {
                // Determine swipe direction
                String swipeDirection;

                if (direction == ItemTouchHelper.LEFT) {
                    handleSwipeEvent(false);
                    swipeDirection = "Left";

                } else if (direction == ItemTouchHelper.RIGHT) {
                    handleSwipeEvent(true);
                    swipeDirection = "Right";

                } else if (direction == ItemTouchHelper.UP) {
                    swipeDirection = "Up";

                } else {
                    swipeDirection = "Down";
                }

                int position = view.getAdapterPosition();
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

        private void handleSwipeEvent(final boolean value) {
            DatabaseReference swipeReference = FirebaseInitialization.getItemsDatabaseReference().child(key).child("swipe");
            DatabaseReference swipeActionRef = swipeReference.push();
            Swipe swipe = new Swipe(FirebaseInitialization.getCurrentUserId(), value);
            swipeActionRef.setValue(swipe).addOnCompleteListener(task1 -> {
                if(task1.isSuccessful()) {
                    Log.d(TAG, "Swipe Successful");
                }
            });
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