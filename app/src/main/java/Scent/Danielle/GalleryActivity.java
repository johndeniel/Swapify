package Scent.Danielle;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import Scent.Danielle.Utils.Database.FirebaseInitialization;
import Scent.Danielle.Utils.Item;

public class GalleryActivity extends Fragment {

    private final String TAG = GalleryActivity.class.getSimpleName();
    private final List<Item> itemList = new ArrayList<>();
    private ItemGalleryAdapter galleryAdapter;


    // Define a callback interface
    interface GalleryItemKeysCallback {
        void onGalleryItemKeysLoaded(ArrayList<String> galleryItemKeys);
    }

    // Define a methods for deleting data in Firebase
    interface FirebaseDataDeletionService {
        void deleteItemFromUserItems(final String itemKey);
        void deleteItemFromUserGallery(final String itemKey);
        void deleteItemFromStorageUploadImage(final String fileName);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_gallery, container, false);

        RecyclerView itemGalleryRecyclerView = rootView.findViewById(R.id.galleryRecyclerView);
        itemGalleryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        galleryAdapter = new ItemGalleryAdapter(itemList);
        itemGalleryRecyclerView.setAdapter(galleryAdapter);

        fetchGalleryItemKeys(new GalleryItemKeysCallback() {
            @Override
            public void onGalleryItemKeysLoaded(ArrayList<String> galleryItemKeys) {
                loadGalleryItems(galleryItemKeys);
            }
        });

        return rootView;
    }


    private void fetchGalleryItemKeys(GalleryItemKeysCallback callback) {
        ArrayList<String> galleryItemKeys = new ArrayList<>();
        FirebaseInitialization.getGalleryDatabaseReference().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String galleryItemKey = snapshot.getValue(String.class);
                    galleryItemKeys.add(galleryItemKey);
                    Log.i("GalleryData", "Fetched gallery item key: " + galleryItemKey);
                }
                callback.onGalleryItemKeysLoaded(galleryItemKeys);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching gallery item keys: " + databaseError.getMessage());
            }
        });
    }


    private void loadGalleryItems(ArrayList<String> galleryItemKeys) {
        FirebaseInitialization.getItemsDatabaseReference().addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    if (galleryItemKeys.contains(snapshot.getKey())) {
                        Item galleryItem = snapshot.getValue(Item.class);
                        itemList.add(galleryItem);
                    }
                }
                galleryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error loading gallery items: " + databaseError.getMessage());
            }
        });
    }


    private void updateItemFromLocalListAndFirebase(ViewGroup container, int position, Item currentItem) {
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
                    purgeExecutor.deleteItemFromUserGallery(itemKey);
                    purgeExecutor.deleteItemFromStorageUploadImage(fileName);
                })
                .setNegativeButton("No", null)
                .show();
    }


    private class FirebaseDataDeletionManager implements FirebaseDataDeletionService {
        @Override
        public void deleteItemFromUserItems(final String itemKey) {
            try {
                FirebaseInitialization.getItemsDatabaseReference().child(itemKey).removeValue();
                Log.d(TAG, "Item deleted successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error deleting item: " + e.getMessage(), e);
            }
        }

        @Override
        public void deleteItemFromUserGallery(final String itemKey) {
            FirebaseInitialization.getGalleryDatabaseReference().orderByValue().equalTo(itemKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        snapshot.getRef().removeValue();
                        Log.d(TAG, "Item deleted from gallery successfully");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error deleting item from user gallery: " + databaseError.getMessage());
                }
            });
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
        ImageView mediaImageView;
        TextView titleTextView, nameTextView, descriptionTextView;
        MaterialButton editButton, deleteButton;

        private ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        private void bindItem(Item currentItem) {
            titleTextView.setText(currentItem.getTitle());
            nameTextView.setText("By: " + currentItem.getFullName());
            descriptionTextView.setText(currentItem.getDescription());
            Glide.with(itemView.getContext()).load(currentItem.getImageUrl()).into(mediaImageView);
            editButton.setOnClickListener(v -> updateItemFromLocalListAndFirebase((ViewGroup) v.getParent(), getAdapterPosition(), currentItem));
            deleteButton.setOnClickListener(v -> deleteItemFromLocalListAndFirebase(getAdapterPosition()));
        }
    }
}