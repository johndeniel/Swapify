package Scent.Danielle.Utils;

// Android core components
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

// AndroidX imports
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// Third-party library for image loading
import de.hdodenhof.circleimageview.CircleImageView;

// Java standard imports
import java.util.List;

// Reference to your app's resources
import Scent.Danielle.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatItem> chatItems;

    public ChatAdapter(List<ChatItem> chatItems) {
        this.chatItems = chatItems;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the chat item layout and create a new ChatViewHolder
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    // Bind data to the views within the ChatViewHolder
    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem chatItem = chatItems.get(position);
        holder.avatarImageView.setImageResource(chatItem.getAvatarResId());
        holder.nameTextView.setText(chatItem.getName());
        holder.messageTextView.setText(chatItem.getMessage());
    }

    // Return the number of chat items in the list
    @Override
    public int getItemCount() { return chatItems.size();}

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView avatarImageView;
        TextView nameTextView;
        TextView messageTextView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views within the chat item layout
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}