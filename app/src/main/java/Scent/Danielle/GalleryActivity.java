package Scent.Danielle;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import Scent.Danielle.Utils.DataModel.User;
import Scent.Danielle.Utils.Database.FirebaseInitialization;
import Scent.Danielle.Utils.DataModel.Item;

public class GalleryActivity extends Fragment {

    private final String TAG = GalleryActivity.class.getSimpleName();
    private static final int PICK_IMAGE_REQUEST = 1;
    private final List<Item> itemList = new ArrayList<>();
    private ItemGalleryAdapter galleryAdapter;
    private ImageView uploadImageView;
    private Uri imageUri;
    private EditText titleEditText;
    private EditText descriptionEditText;

    // Define a methods for deleting data in Firebase
    interface FirebaseDataDeletionService {
        void deleteItemFromUserItems(final String itemKey);
        void deleteItemFromStorageUploadImage(final String fileName);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(uploadImageView);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_gallery, container, false);

        ExtendedFloatingActionButton extendedFab = rootView.findViewById(R.id.extended_fab);
        extendedFab.setOnClickListener(view -> showAddItemDialog(inflater, container));

        RecyclerView itemGalleryRecyclerView = rootView.findViewById(R.id.galleryRecyclerView);
        itemGalleryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        galleryAdapter = new ItemGalleryAdapter(itemList);
        itemGalleryRecyclerView.setAdapter(galleryAdapter);

        loadGalleryItems();
        return rootView;
    }

    private void loadGalleryItems() {
        DatabaseReference databaseReference = FirebaseInitialization.getItemsDatabaseReference();
        String currentUserID = FirebaseInitialization.getCurrentUserId();
        Query query = databaseReference.orderByChild("userId").equalTo(currentUserID);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear(); // Clear the existing list
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        itemList.add(item);
                    }
                }
                galleryAdapter.notifyDataSetChanged(); // Notify the adapter of changes
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading gallery items: " + databaseError.getMessage());
                // Handle the error (e.g., show a toast)
            }
        });
    }


    private void updateItemFromLocalListAndFirebase(@NonNull ViewGroup container, int position, @NonNull Item currentItem) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_update, container, false);

        // Initialize UI components
        EditText titleEditText = dialogView.findViewById(R.id.update_title_edit_text);
        EditText descriptionEditText = dialogView.findViewById(R.id.update_description_edit_text);

        // Set current item values to the dialog
        titleEditText.setText(currentItem.getTitle());
        descriptionEditText.setText(currentItem.getDescription());

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Edit Item")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    String itemKey = itemList.get(position).getKey();
                    String title = titleEditText.getText().toString();
                    String description = descriptionEditText.getText().toString();
                    FirebaseInitialization.getItemsDatabaseReference().child(itemKey).child("title").setValue(title);
                    FirebaseInitialization.getItemsDatabaseReference().child(itemKey).child("description").setValue(description);
                    Toast.makeText(requireActivity(), "Update Successful", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteItemFromLocalListAndFirebase(int position) {
        // Get the item key from the list
        String itemKey = itemList.get(position).getKey();
        String fileName = itemList.get(position).getFileName();

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Confirm Deletion")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Remove item from local list and notify adapter
                    itemList.remove(position);
                    galleryAdapter.notifyItemRemoved(position);

                    FirebaseDataDeletionManager purgeExecutor = new FirebaseDataDeletionManager();
                    purgeExecutor.deleteItemFromUserItems(itemKey);
                    purgeExecutor.deleteItemFromStorageUploadImage(fileName);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showAddItemDialog(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
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

        String uniqueFileName = "image_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageReference = FirebaseInitialization.getPhotoStorageReferences().child(uniqueFileName);

        // Initiate file upload with chained success and failure listeners
        UploadTask uploadTask = storageReference.putFile(imageUri);
        uploadTask.continueWithTask(task -> storageReference.getDownloadUrl())
                .addOnCompleteListener(task -> {
                    try {
                        // Generate a unique key for the new item
                        String key = FirebaseInitialization.getItemsDatabaseReference().push().getKey();
                        String fullName = GoogleSignIn.getLastSignedInAccount(requireContext()).getDisplayName();
                        String imageUrl = task.getResult().toString();
                        String userId = FirebaseInitialization.getCurrentUserId();
                        String avatar = FirebaseInitialization.getCurrentUserDisplayPhotoUrl();
                        // Create and store the upload data
                        Item upload = new Item(key, userId, avatar, fullName, title, description, uniqueFileName, imageUrl);
                        FirebaseInitialization.getItemsDatabaseReference().child(key).setValue(upload)
                                .addOnCompleteListener(databaseTask -> {
                                    if (databaseTask.isSuccessful()) {
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


    private class FirebaseDataDeletionManager implements FirebaseDataDeletionService {
        @Override
        public void deleteItemFromUserItems(final String itemKey) {
            try {
                FirebaseInitialization.getItemsDatabaseReference().child(itemKey).removeValue();
                Log.d(TAG, "Item deleted successfully");
                Toast.makeText(requireActivity(), "Delete Successful", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error deleting item: " + e.getMessage(), e);
            }
        }

        @Override
        public void deleteItemFromStorageUploadImage(final String fileName) {
            FirebaseInitialization
                    .getPhotoStorageReferences()
                    .child(fileName)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "File deleted successfully: " + fileName))
                    .addOnFailureListener(e -> Log.e(TAG, "Error deleting file: " + fileName, e));

        }
    }

    private class ItemGalleryAdapter extends RecyclerView.Adapter<GalleryActivity.ItemViewHolder> {
        private final List<Item> itemList;
        public ItemGalleryAdapter(List<Item> itemList) {
            this.itemList = itemList;
        }

        @Override
        public int getItemCount() { return itemList.size(); }

        @NonNull
        @Override
        public GalleryActivity.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery, parent, false);
            return new GalleryActivity.ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GalleryActivity.ItemViewHolder holder, int position) {
            Item currentItem = itemList.get(position);
            holder.bindItem(currentItem);
        }
    }


    private class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mediaImageView;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final MaterialButton editButton;
        private final MaterialButton deleteButton;

        private ItemViewHolder(@NonNull View view) {
            super(view);
            mediaImageView = view.findViewById(R.id.mediaImageView);
            titleTextView = view.findViewById(R.id.titleTextView);
            descriptionTextView = view.findViewById(R.id.descriptionTextView);
            editButton = view.findViewById(R.id.editButton);
            deleteButton = view.findViewById(R.id.deleteButton);

            // Set OnClickListener for mediaImageView
            mediaImageView.setOnClickListener(v -> {
                // Call the navigateToSwipeRight method with the currentItem
                Item currentItem = itemList.get(getAdapterPosition());
                navigateToSwipeRight(currentItem);
            });
        }

        private void bindItem(@NonNull Item currentItem) {
            titleTextView.setText(currentItem.getTitle());
            descriptionTextView.setText(currentItem.getDescription());
            Glide.with(itemView.getContext()).load(currentItem.getImageUrl()).into(mediaImageView);
            editButton.setOnClickListener(v -> updateItemFromLocalListAndFirebase((ViewGroup) v.getParent(), getAdapterPosition(), currentItem));
            deleteButton.setOnClickListener(v -> deleteItemFromLocalListAndFirebase(getAdapterPosition()));
        }

        private void navigateToSwipeRight(@NonNull Item currentItem) {
            Intent intent = new Intent(requireContext(), SwipeRightActivity.class);
            intent.putExtra("key", currentItem.getKey());
            intent.putExtra("title", currentItem.getTitle());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            requireActivity().startActivity(intent);
        }
    }
}