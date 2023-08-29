package Scent.Danielle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MessageActivity extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView uploadImageView;
    private EditText descriptionEditText;
    private Button uploadButton;

    private DatabaseReference databaseReference;

    private Uri imageUri;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage storage;

    public MessageActivity() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_message, container, false);

        uploadImageView = rootView.findViewById(R.id.uploadImageView);
        descriptionEditText = rootView.findViewById(R.id.descriptionEditText);
        uploadButton = rootView.findViewById(R.id.uploadButton);

        firebaseAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        uploadImageView.setOnClickListener(v -> openFileChooser());
        uploadButton.setOnClickListener(v -> uploadFile());



        firebaseAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("items");

        // Create initial data if needed
        checkAndCreateInitialData();

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
                // Handle errors if needed
            }
        });
    }

    private void createSampleUpload() {
        String sampleUserId = "sample_user_id";
        String sampleDescription = "Sample description";
        String sampleImageUrl = "sample_image_url";

        items sampleUpload = new items(sampleUserId, sampleDescription, sampleImageUrl);

        DatabaseReference newUploadRef = databaseReference.push();
        newUploadRef.setValue(sampleUpload)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(requireActivity(), "Sample Upload Created", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireActivity(), "Failed to Create Sample Upload: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(uploadImageView);
        }
    }

    private void uploadFile() {
        if (imageUri != null) {
            StorageReference storageReference = storage.getReference().child("uploads/" + firebaseAuth.getCurrentUser().getUid());
            UploadTask uploadTask = storageReference.putFile(imageUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String userId = firebaseAuth.getCurrentUser().getUid();
                    String description = descriptionEditText.getText().toString();
                    String imageUrl = uri.toString();

                    // Create an Upload object with the uploaded data
                    items upload = new items(userId, description, imageUrl);

                    // Get a reference to the Firebase Realtime Database and push the data
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("items");
                    databaseReference.push().setValue(upload)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(requireActivity(), "Upload Successful", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireActivity(), "Upload Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(requireActivity(), "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(requireActivity(), "No file selected", Toast.LENGTH_SHORT).show();
        }
    }
}



class items {
    private String userId;
    private String description;
    private String imageUrl;

    public items() {
        // Empty constructor required for Firebase
    }

    public items(String userId, String description, String imageUrl) {
        this.userId = userId;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
