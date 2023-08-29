package Scent.Danielle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

import Scent.Danielle.Utils.ItemAdapter;
import Scent.Danielle.Utils.Items;

public class FeedActivity extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage storage;
    private RecyclerView itemRecyclerView;
    private ItemAdapter itemAdapter;
    private List<Items> itemList;
    private ImageView uploadImageView;
    private Uri imageUri;
    private EditText titleEditText;
    private EditText descriptionEditText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_feed, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("items");

        checkAndCreateInitialData();

        ExtendedFloatingActionButton extendedFab = rootView.findViewById(R.id.extended_fab);
        uploadImageView = new ImageView(requireContext());

        itemRecyclerView = rootView.findViewById(R.id.itemRecyclerView);
        itemRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        itemList = new ArrayList<>();
        itemAdapter = new ItemAdapter(itemList);
        itemRecyclerView.setAdapter(itemAdapter);

        loadItemsFromFirebase();

        extendedFab.setOnClickListener(view -> showAddItemDialog(inflater, container));

        return rootView;
    }

    private void checkAndCreateInitialData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    createSampleUpload();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showErrorToast("Database error: " + databaseError.getMessage());
            }
        });
    }

    private void createSampleUpload() {
        // Create and upload sample data to Firebase
        try {
            String sampleUserId = "RZCVBq2uI6SErP4BUcC0qS8G4Az2";
            String sampleFullName = "John Deniel Dela Peña";
            String sampleTitle = "SuperSport S";
            String sampleDescription = "The Ducati SuperSport S is a suitable everyday two-wheeler, with its blend of comfort and versatility. Locally launched last 2018, this super sport bike has a sporty yet elegant design that’s sure to turn heads. ";
            String sampleImageUrl = "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D&auto=format&fit=crop&w=2070&q=80";

            Items sampleUpload = new Items(sampleUserId, sampleFullName, sampleTitle, sampleDescription, sampleImageUrl);

            DatabaseReference newUploadRef = databaseReference.push();
            newUploadRef.setValue(sampleUpload)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showToast("Sample Upload Created");
                        } else {
                            showErrorToast("Failed to Create Sample Upload: " + task.getException().getMessage());
                        }
                    });
        } catch (Exception e) {
            showErrorToast("Error creating sample upload: " + e.getMessage());
        }
    }

    private void loadItemsFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Items item = snapshot.getValue(Items.class);
                    itemList.add(item);
                }
                itemAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showErrorToast("Database error: " + databaseError.getMessage());
            }
        });
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

        // Check if title and description are not empty
        if (imageUri != null && !title.isEmpty() && !description.isEmpty()) {
            try {
                String uniqueFileName = generateUniqueFileName();
                StorageReference storageReference = storage.getReference().child("uploads/" + firebaseAuth.getCurrentUser().getUid() + "/" + uniqueFileName);
                UploadTask uploadTask = storageReference.putFile(imageUri);

                uploadTask.addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String userId = firebaseAuth.getCurrentUser().getUid();
                    String imageUrl = uri.toString();
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
                    String fullName = account.getDisplayName();

                    Items upload = new Items(userId, fullName, title, description, imageUrl);

                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("items");
                    databaseReference.push().setValue(upload)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
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
            // Show appropriate error message if any of the fields are empty
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