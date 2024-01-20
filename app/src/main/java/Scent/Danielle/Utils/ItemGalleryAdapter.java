package Scent.Danielle.Utils;

// Android core components
import android.content.Context;
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
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

// Third-party libraries
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// List of items
import java.util.List;

// Reference to your app's resources
import Scent.Danielle.R;

public class ItemGalleryAdapter extends RecyclerView.Adapter<ItemGalleryAdapter.ItemViewHolder> {

    private final String ITEMS_REFERENCE = "items";
    private final String GALLERY_REFERENCE = "gallery";
    private final String TAG = ItemGalleryAdapter.class.getSimpleName();

    private final Context context;
    private final List<Items> itemList;

    private FirebaseAuth firebaseAuth;
    private String currentUserUid;
    private FirebaseStorage storage;

    public ItemGalleryAdapter(Context context, List<Items> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.currentUserUid = firebaseAuth.getCurrentUser().getUid();
        this.storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public ItemGalleryAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the RecyclerView
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery, parent, false);
        return new ItemGalleryAdapter.ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemGalleryAdapter.ItemViewHolder holder, int position) {
        // Get the current item from the list
        Items currentItem = itemList.get(position);
        holder.bindItem(currentItem);
    }

    @Override
    public int getItemCount() { return itemList.size(); }

    // ViewHolder class to hold and manage views for each item in the RecyclerView
     class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaImageView;
        TextView titleTextView, nameTextView, descriptionTextView;
        MaterialButton editButton, deleteButton;

        // ViewHolder constructor initializing views.
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        // Bind item data to views.
        void bindItem(Items currentItem) {
            titleTextView.setText(currentItem.getTitle());
            nameTextView.setText("By: " + currentItem.getFullName());
            descriptionTextView.setText(currentItem.getDescription());
            Glide.with(itemView.getContext()).load(currentItem.getImageUrl()).into(mediaImageView);

            // Set click listeners for edit and delete buttons.
            editButton.setOnClickListener(v -> showEditItemDialog((ViewGroup) v.getParent(), getAdapterPosition(), currentItem));
            deleteButton.setOnClickListener(v -> deleteItemFromLocalListAndFirebase(getAdapterPosition()));
        }
    }

    private void deleteItemFromLocalListAndFirebase(int position) {
        // Get the item key from the list
        String itemKey = itemList.get(position).getKey();
        String uniqueFileName = itemList.get(position).getFileName();

        // Build and show a confirmation dialog
        AlertDialog.Builder confirmationDialog = new AlertDialog.Builder(context);
        confirmationDialog.setTitle("Confirm Deletion");
        confirmationDialog.setMessage("Are you sure you want to delete this item?");

        confirmationDialog.setPositiveButton("Yes", (dialog, which) -> {
            // Remove item from local list
            itemList.remove(position);
            notifyItemRemoved(position);

            // Delete item from Firebase Realtime Database
            deleteItemFromUserItems(itemKey);
            deleteItemFromUserGallery(itemKey);

            // Delete item from Firebase Storage
            deleteItemFromStorageUploadImage(uniqueFileName);
        });

        confirmationDialog.setNegativeButton("No", (dialog, which) -> {
            // User canceled deletion, do nothing
        });

        confirmationDialog.show();
    }

    private void deleteItemFromUserItems(String itemKey) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference(ITEMS_REFERENCE);
        databaseReference.child(itemKey).removeValue();
    }

    private void deleteItemFromUserGallery(String itemKey) {
        DatabaseReference galleryReference = FirebaseDatabase.getInstance()
                .getReference(GALLERY_REFERENCE)
                .child(currentUserUid);
        galleryReference.orderByValue().equalTo(itemKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    snapshot.getRef().removeValue();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors here
                Log.e(TAG, "Error deleting item from user gallery: " + databaseError.getMessage());
            }
        });
    }

    private void deleteItemFromStorageUploadImage(String uniqueFileName) {
        try {
            if (itemList != null) {
                // Create a reference to the file you want to delete
                StorageReference storageReference = storage.getReference().child("uploads/" + firebaseAuth.getCurrentUser().getUid() + "/" + uniqueFileName);

                // Delete the file

                Log.d(TAG, "Deleting file: " + uniqueFileName);
                storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // File deleted successfully
                        // You can add any further actions or notifications here
                        Log.d(TAG, "File deleted successfully");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle any errors that occurred during the deletion
                        // You can log the error, show a message to the user, etc.
                        Log.e(TAG, "Error deleting item from storage: " + e.getMessage());
                    }
                });
            } else {
                Log.e(TAG, "Invalid position or itemList is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in deleteItemFromStorageUploadImage: " + e.getMessage());
        }
    }


    private void showEditItemDialog(ViewGroup container, int position, Items currentItem) {
        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.item_update, container, false);

        // Initialize UI components
        EditText titleEditText = dialogView.findViewById(R.id.update_title_edit_text);
        EditText descriptionEditText = dialogView.findViewById(R.id.update_description_edit_text);

        // Set current item values to the dialog
        titleEditText.setText(currentItem.getTitle());
        descriptionEditText.setText(currentItem.getDescription());

        // Create and configure the AlertDialog
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Edit Item")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    // Retrieve values when the "Update" button is clicked
                    String title = titleEditText.getText().toString();
                    String description = descriptionEditText.getText().toString();
                    updateItemInFirebase(position, title, description);
                })
                .setNegativeButton("Cancel", null)
                .create();

        // Display the AlertDialog
        alertDialog.show();
    }

    private void updateItemInFirebase(int position, String title, String description) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference(ITEMS_REFERENCE);

        String itemKey = itemList.get(position).getKey();

        databaseReference.child(itemKey).child("title").setValue(title);
        databaseReference.child(itemKey).child("description").setValue(description);
    }
}