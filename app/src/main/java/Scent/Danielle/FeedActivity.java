package Scent.Danielle;

// Android core components
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX components
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// AndroidX recyclerview imports
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Third-party library for image loading
import com.bumptech.glide.Glide;

// Google Sign-In API imports
import com.google.android.gms.auth.api.signin.GoogleSignIn;

// Google Material imports
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

// Firebase imports
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Java standard imports
import java.util.ArrayList;
import java.util.List;

// Custom class imports
import Scent.Danielle.Utils.Database.FirebaseInitialization;
import Scent.Danielle.Utils.DataModel.Item;

public class FeedActivity extends Fragment {
    private final String TAG = FeedActivity.class.getSimpleName();
    private static final int PICK_IMAGE_REQUEST = 1;
    private FeedItemListAdapter itemAdapter;
    private List<Item> itemList;
    private ImageView uploadImageView;
    private Uri imageUri;
    private EditText titleEditText;
    private EditText descriptionEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_feed, container, false);

        ExtendedFloatingActionButton extendedFab = rootView.findViewById(R.id.extended_fab);
        uploadImageView = new ImageView(requireContext());

        RecyclerView itemRecyclerView = rootView.findViewById(R.id.itemRecyclerView);
        itemRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        itemList = new ArrayList<>();
        itemAdapter = new FeedItemListAdapter(itemList);
        itemRecyclerView.setAdapter(itemAdapter);

        loadItemsFromFirebase();
        extendedFab.setOnClickListener(view -> showAddItemDialog(inflater, container));
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


    private void uploadFile() {
        // Gather necessary information before starting the upload process
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        // Ensure all required fields are present
        if (imageUri == null) {
            Toast.makeText(requireActivity(), "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }
        if (description.isEmpty()) {
            Toast.makeText(requireActivity(), "Please enter a description", Toast.LENGTH_SHORT).show();
            return;
        }

        String uniqueFileName = generateUniqueFileName();
        StorageReference storageReference = FirebaseInitialization.getPhotoStorageReferences().child(uniqueFileName);

        // Initiate file upload with chained success and failure listeners
        UploadTask uploadTask = storageReference.putFile(imageUri);
        uploadTask.continueWithTask(task -> storageReference.getDownloadUrl())
                .addOnCompleteListener(task -> {
                    try {
                        String key = FirebaseInitialization.getItemsDatabaseReference().push().getKey();
                        String fullName = GoogleSignIn.getLastSignedInAccount(requireContext()).getDisplayName();
                        String imageUrl = task.getResult().toString();

                        // Create and store the upload data
                        Item upload = new Item(key, fullName, title, description, uniqueFileName, imageUrl);
                        FirebaseInitialization.getItemsDatabaseReference().child(key).setValue(upload)
                                .addOnCompleteListener(databaseTask -> {
                                    if (databaseTask.isSuccessful()) {
                                        FirebaseInitialization.getGalleryDatabaseReference().push().setValue(key);
                                        Toast.makeText(requireActivity(), "Upload Successful", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.e(TAG, "Upload failed: " + databaseTask.getException().getMessage());
                                    }
                                });
                    } catch (Exception e) {
                        Log.e(TAG, "Upload failed: " + e.getMessage());
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "Upload failed: " + e.getMessage()));
    }


    private void showAddItemDialog(LayoutInflater inflater, ViewGroup container) {
        Log.d(TAG, "Showing Add Item dialog");
        View dialogView = inflater.inflate(R.layout.item_upload, container, false);
        titleEditText = dialogView.findViewById(R.id.title_edit_text);
        descriptionEditText = dialogView.findViewById(R.id.description_edit_text);
        uploadImageView = dialogView.findViewById(R.id.uploadImageView);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New Item")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> uploadFile())
                .setNegativeButton("Cancel", null)
                .create()
                .show();

        uploadImageView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(uploadImageView);
        }
    }


    private class FeedItemListAdapter extends RecyclerView.Adapter<FeedActivity.FeedItemDisplayHolder> {
        private final List<Item> itemList;

        public FeedItemListAdapter(List<Item> itemList) {
            this.itemList = itemList;
        }

        @Override
        public int getItemCount() { return itemList.size(); }

        @NonNull
        @Override
        public FeedItemDisplayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
            Log.d(TAG, "ViewHolder created for new item position");
            return new FeedItemDisplayHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FeedItemDisplayHolder holder, int position) {
            Item currentItem = itemList.get(position);
            holder.bindItem(currentItem);
            Log.d(TAG, "onBindViewHolder called for position " + position);
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


    private String generateUniqueFileName() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "image_" + timestamp + ".jpg";
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}