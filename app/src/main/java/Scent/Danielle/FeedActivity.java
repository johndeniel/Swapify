package Scent.Danielle;

// Android core components
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

// AndroidX components
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// AndroidX appcompat imports
import androidx.appcompat.app.AlertDialog;

// AndroidX recyclerview imports
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Third-party library for image loading
import com.bumptech.glide.Glide;

// Google Sign-In API imports
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

// Google Material imports
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

// Firebase imports
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Java standard imports
import java.util.ArrayList;
import java.util.List;

// Custom class imports
import Scent.Danielle.Utils.FirebaseInitialization;
import Scent.Danielle.Utils.ItemFeedAdapter;
import Scent.Danielle.Utils.Items;

public class FeedActivity extends Fragment {
    private final String TAG = FeedActivity.class.getSimpleName();
    private static final int PICK_IMAGE_REQUEST = 1;
    private RecyclerView itemRecyclerView;
    private ItemFeedAdapter itemAdapter;
    private List<Items> itemList;
    private ImageView uploadImageView;
    private Uri imageUri;
    private EditText titleEditText;
    private EditText descriptionEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_feed, container, false);

        ExtendedFloatingActionButton extendedFab = rootView.findViewById(R.id.extended_fab);
        uploadImageView = new ImageView(requireContext());

        itemRecyclerView = rootView.findViewById(R.id.itemRecyclerView);
        itemRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        itemList = new ArrayList<>();
        itemAdapter = new ItemFeedAdapter(itemList);
        itemRecyclerView.setAdapter(itemAdapter);

        loadItemsFromFirebase();

        extendedFab.setOnClickListener(view -> showAddItemDialog(inflater, container));

        return rootView;
    }


    private void loadItemsFromFirebase() {

        DatabaseReference itemsReference = FirebaseInitialization.getItemsDatabaseReference();

        if (itemsReference != null) {

            itemsReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    try {
                        if (itemList != null) {
                            itemList.clear();
                        } else {
                            itemList = new ArrayList<>();
                        }

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Items item = snapshot.getValue(Items.class);
                            if (item != null) {
                                itemList.add(item);
                            }
                        }

                        if (itemAdapter != null) {
                            itemAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Items updated successfully. Notifying adapter.");
                        } else {
                            Log.e(TAG, "Adapter is null. Unable to update UI.");
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error processing data", e);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Database error", databaseError.toException());
                }
            });
        } else {
            Log.e(TAG, "Database reference is null. Unable to fetch data.");
        }
    }


    private void showAddItemDialog(LayoutInflater inflater, ViewGroup container) {
        View dialogView = inflater.inflate(R.layout.item_upload, container, false);
        titleEditText = dialogView.findViewById(R.id.title_edit_text);
        descriptionEditText = dialogView.findViewById(R.id.description_edit_text);
        uploadImageView = dialogView.findViewById(R.id.uploadImageView);

        uploadImageView.setOnClickListener(v -> openFileChooser());

        AlertDialog alertDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Add New Item")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> uploadFile())
                .setNegativeButton("Cancel", null)
                .create();

        alertDialog.show();
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == requireActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(uploadImageView);
        }
    }

    private void uploadFile() {
        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();

        if (imageUri != null && !title.isEmpty() && !description.isEmpty()) {
            try {
                String uniqueFileName = generateUniqueFileName();
                StorageReference storageReference = FirebaseInitialization.getStorageReference().child("uploads/" + FirebaseInitialization.getUserId() + "/" + uniqueFileName);
                UploadTask uploadTask = storageReference.putFile(imageUri);

                uploadTask.addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {

                    DatabaseReference newUploadReference = FirebaseInitialization.getItemsDatabaseReference().push();

                    String key = newUploadReference.getKey();
                    String imageUrl = uri.toString();
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                    String fullName = account.getDisplayName();

                    Items upload = new Items(key, fullName, title, description, uniqueFileName, imageUrl);

                    newUploadReference.setValue(upload)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    DatabaseReference galleryDatabaseReference = FirebaseInitialization.getGalleryDatabaseReference();
                                    galleryDatabaseReference.push().setValue(key);
                                    showToast("Upload Successful");
                                } else {
                                    showErrorToast("Upload Failed: " + task.getException().getMessage());
                                }
                            });
                })).addOnFailureListener(e -> showErrorToast("Upload Failed: " + e.getMessage()));
            } catch (Exception e) {
                showErrorToast("Upload failed: " + e.getMessage());
            }
        } else {
            if (imageUri == null) {
                showErrorToast("Please select an image");
            } else if (title.isEmpty()) {
                showErrorToast("Please enter a title");
            } else if (description.isEmpty()) {
                showErrorToast("Please enter a description");
            }
        }
    }


    private String generateUniqueFileName() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "image_" + timestamp + ".jpg";
    }

    private void showToast(String message) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(requireActivity(), "Error: " + message, Toast.LENGTH_SHORT).show();
    }
}