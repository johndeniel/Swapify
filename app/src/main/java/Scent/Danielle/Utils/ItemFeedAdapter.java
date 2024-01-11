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

public class ItemFeedAdapter extends RecyclerView.Adapter<ItemFeedAdapter.ItemViewHolder> {
    private final List<Items> itemList;

    public ItemFeedAdapter(List<Items> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the RecyclerView
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        // Get the current item from the list
        Items currentItem = itemList.get(position);

        // Bind data to your ViewHolder's views
        holder.titleTextView.setText(currentItem.getTitle());
        holder.nameTextView.setText("By: " + currentItem.getFullName());
        holder.descriptionTextView.setText(currentItem.getDescription());

        // Load image using Glide or any other image loading library
        Glide.with(holder.itemView.getContext()).load(currentItem.getImageUrl()).into(holder.mediaImageView);

        // Set click listeners for buttons
        holder.messageButton.setOnClickListener(v -> showToast(holder.itemView.getContext(), "Message"));
        holder.wishListButton.setOnClickListener(v -> showToast(holder.itemView.getContext(), "Wish List"));
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // ViewHolder class to hold and manage views for each item in the RecyclerView
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaImageView;
        TextView titleTextView, nameTextView, descriptionTextView;
        MaterialButton messageButton, wishListButton;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            messageButton = itemView.findViewById(R.id.messageButton);
            wishListButton = itemView.findViewById(R.id.wishListButton);
        }
    }

    // Display a short toast message
    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}