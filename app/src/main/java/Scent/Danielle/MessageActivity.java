package Scent.Danielle;

// Android core components
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

// AndroidX components
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Java standard imports
import java.util.ArrayList;
import java.util.List;

// Custom class imports
import Scent.Danielle.Utils.Chat;
import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends Fragment {

    public MessageActivity() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.activity_message, container, false);

        // Create sample chat items
        List<Chat> chats = new ArrayList<>();
        chats.add(new Chat(R.drawable.about, "Alice", "Hi there! How's your day going? Everything alright?"));

        // Set up RecyclerView
        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ChatAdapter adapter = new ChatAdapter(chats);
        recyclerView.setAdapter(adapter);

        // Add divider decoration to RecyclerView
//        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
//        recyclerView.addItemDecoration(dividerItemDecoration);

        return rootView;
    }



    private class ChatAdapter extends RecyclerView.Adapter<MessageActivity.ChatViewHolder> {

        private final List<Chat> chats;

        public ChatAdapter(List<Chat> chats) {
            this.chats = chats;
        }

        @NonNull
        @Override
        public MessageActivity.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the chat item layout and create a new ChatViewHolder
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
            return new MessageActivity.ChatViewHolder(view);
        }

        // Bind data to the views within the ChatViewHolder
        @Override
        public void onBindViewHolder(@NonNull MessageActivity.ChatViewHolder holder, int position) {
            Chat chat = this.chats.get(position);
            holder.avatarImageView.setImageResource(chat.getAvatarResId());
            holder.nameTextView.setText(chat.getName());
            holder.messageTextView.setText(chat.getMessage());
        }

        // Return the number of chat items in the list
        @Override
        public int getItemCount() { return chats.size();}


    }

    private class ChatViewHolder extends RecyclerView.ViewHolder {
        CircleImageView avatarImageView;
        TextView nameTextView;
        TextView messageTextView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views within the chat item layout
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);

            // Set click listener for the entire item view
            itemView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Intent intent = new Intent(itemView.getContext(), ChatActivity.class);
                    intent.putExtra("id", "6sKKu36sF4P5pqPRPwlhFTZvy122");
                    intent.putExtra("name", "John Deniel");
                    intent.putExtra("avatar", "avatar");
                    startActivity(intent);
                }
            });
        }
    }
}