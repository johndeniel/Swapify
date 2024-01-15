package Scent.Danielle.Utils;
// Android core components
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX components
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// Third-party libraries
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// List of items
import java.util.List;

// Reference to your app's resources
import Scent.Danielle.R;

public class ItemGalleryAdapter extends RecyclerView.Adapter<ItemGalleryAdapter.ItemViewHolder> {

    private final List<Items> itemList;
    private static final String ITEMS_REFERENCE = "items";
    private static final String GALLERY_REFERENCE = "gallery";

    public ItemGalleryAdapter(List<Items> itemList) {
        this.itemList = itemList;
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

        // Bind data to your ViewHolder's views
        holder.titleTextView.setText(currentItem.getTitle());
        holder.nameTextView.setText("By: " + currentItem.getFullName());
        holder.descriptionTextView.setText(currentItem.getDescription());

        // Load image using Glide or any other image loading library
        Glide.with(holder.itemView.getContext()).load(currentItem.getImageUrl()).into(holder.mediaImageView);

        // Set click listeners for buttons
        holder.editButton.setOnClickListener(v -> showToast(holder.itemView.getContext(), "Edit"));

        holder.deleteButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                deleteItem(adapterPosition);
            }
        });

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // ViewHolder class to hold and manage views for each item in the RecyclerView
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaImageView;
        TextView titleTextView, nameTextView, descriptionTextView;
        MaterialButton editButton, deleteButton;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    // Delete item from Firebase and local list
    private void deleteItem(int position) {
        Items deletedItem = itemList.get(position);

        // Remove item from local list
        itemList.remove(position);
        notifyItemRemoved(position);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(ITEMS_REFERENCE);
        String itemKey = deletedItem.getKey();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        // Remove item from Firebase Realtime Database
        databaseReference.child(itemKey).removeValue();

        DatabaseReference galleryReference = FirebaseDatabase.getInstance()
                .getReference(GALLERY_REFERENCE)
                .child(firebaseAuth.getCurrentUser().getUid());

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
            }
        });
    }


    // Display a short toast message
    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
