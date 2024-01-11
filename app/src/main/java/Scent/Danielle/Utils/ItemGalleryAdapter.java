package Scent.Danielle.Utils;
// Android core components
import android.content.Context;
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

// List of items
import java.util.List;

// Reference to your app's resources
import Scent.Danielle.R;

public class ItemGalleryAdapter extends RecyclerView.Adapter<ItemGalleryAdapter.ItemViewHolder> {

    private final List<Items> itemList;

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
        holder.deleteButton.setOnClickListener(v -> showToast(holder.itemView.getContext(), "Delete"));
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

    // Display a short toast message
    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
