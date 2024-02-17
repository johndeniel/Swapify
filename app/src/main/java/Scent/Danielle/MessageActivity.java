package Scent.Danielle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.Query;
import de.hdodenhof.circleimageview.CircleImageView;
import Scent.Danielle.Utils.DataModel.Chatroom;
import Scent.Danielle.Utils.Database.FirebaseInitialization;
import Scent.Danielle.Utils.DataModel.User;

public class MessageActivity extends AppCompatActivity {

    private static final String TAG = MessageActivity.class.getSimpleName();
    private RecyclerView searchRecipientRecyclerView;
    private RecyclerView recentChatRecyclerView;
    private SearchRecipientRecyclerAdapter searchRecipientRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        handleSetupTopAppBar();

        searchRecipientRecyclerView = findViewById(R.id.search_user_recycler_view);
        recentChatRecyclerView = findViewById(R.id.recentChats);
        handleRecentChatActivity();
    }

    private void handleSetupTopAppBar() {
        MaterialToolbar chatTopAppBar = findViewById(R.id.chatTopAppBar);
        SearchView searchRecipient = chatTopAppBar.findViewById(R.id.searches);
        chatTopAppBar.setOnClickListener(view -> onBackPressed());
        searchRecipient.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(final String search) {
                handleSearchRecipientResults(search);
                return true;
            }

            @Override
            public boolean onQueryTextChange(final String search) {
                handleSearchRecipientResults(search);
                return true;
            }
        });
    }


    private void handleRecentChatActivity() {
        Query query = FirebaseInitialization.allChatroomCollectionReference()
                .whereArrayContains("userIds", FirebaseInitialization.getCurrentUserId())
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Chatroom> options = new FirestoreRecyclerOptions.Builder<Chatroom>()
                .setQuery(query, Chatroom.class).build();

        RecentChatRecyclerAdapter recentChatRecyclerAdapter = new RecentChatRecyclerAdapter(options, this);
        recentChatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentChatRecyclerView.setAdapter(recentChatRecyclerAdapter);
        recentChatRecyclerAdapter.startListening();
    }

    private void handleSearchRecipientResults(final String search) {
        if(search.length() > 2){
            handleSearchRecyclerView(search);
        }else {
            if (searchRecipientRecyclerAdapter != null) {
                searchRecipientRecyclerAdapter.stopListening();
                searchRecipientRecyclerAdapter = null;
                searchRecipientRecyclerView.setAdapter(null);
                Log.d(TAG, "Cleared search results");
            }
        }
    }

    private void handleSearchRecyclerView(final String search) {
        try {
            Query query = FirebaseInitialization.allUserCollectionReference()
                    .whereGreaterThanOrEqualTo("fullName", search)
                    .whereLessThanOrEqualTo("fullName", search + '\uf8ff');

            FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                    .setQuery(query, User.class)
                    .build();

            searchRecipientRecyclerAdapter = new SearchRecipientRecyclerAdapter(options, this);
            searchRecipientRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            searchRecipientRecyclerView.setAdapter(searchRecipientRecyclerAdapter);
            searchRecipientRecyclerAdapter.startListening();
            Log.d(TAG, "Search RecyclerView set up for term: " + search);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);
        }
    }


    private static class SearchRecipientRecyclerAdapter extends FirestoreRecyclerAdapter<User, MessageActivity.UserModelViewHolder> {
        private final Context context;

        public SearchRecipientRecyclerAdapter(@NonNull FirestoreRecyclerOptions<User> options, @NonNull Context context) {
            super(options);
            this.context = context;
        }

        @NonNull
        @Override
        public MessageActivity.UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat_search, parent, false);
            return new MessageActivity.UserModelViewHolder(view, context);
        }

        @Override
        protected void onBindViewHolder(@NonNull MessageActivity.UserModelViewHolder holder, int position, @NonNull User model) {
            holder.bindItem(model);
        }
    }


    private static class UserModelViewHolder extends RecyclerView.ViewHolder{
        private final CircleImageView avatarImageView;
        private final TextView nameTextView;
        private final Context context;

        public UserModelViewHolder(@NonNull View itemView, @NonNull Context context) {
            super(itemView);
            this.context = context;
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
        }

        private void bindItem(@NonNull User model) {
            Glide.with(context)
                    .load(model.getAvatar())
                    .into(avatarImageView);

            nameTextView.setText(model.getFullName());

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ConversationActivity.class);
                intent.putExtra("id", model.getId());
                intent.putExtra("name", model.getFullName());
                intent.putExtra("avatar", model.getAvatar());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        }
    }


    public static class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<Chatroom, MessageActivity.ChatroomModelViewHolder> {
        private final Context context;

        public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<Chatroom> options, @NonNull Context context) {
            super(options);
            this.context = context;
        }

        @NonNull
        @Override
        public MessageActivity.ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
            return new ChatroomModelViewHolder(view, context);
        }

        @Override
        protected void onBindViewHolder(@NonNull MessageActivity.ChatroomModelViewHolder holder, int position, @NonNull Chatroom model) {
            holder.bindItem(model);
        }
    }


    private static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView messageTextView;
        private final Context context;

        public ChatroomModelViewHolder(@NonNull View itemView, @NonNull Context context) {
            super(itemView);
            this.context = context;
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        private void bindItem(@NonNull Chatroom model) {
            FirebaseInitialization.getOtherUserFromChatroom(model.getUserIds())
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            try {
                                User otherUserModel = task.getResult().toObject(User.class);
                                setupViews(otherUserModel, model);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user data", e);
                            }
                        } else {
                            Log.e(TAG, "Error getting other user data: " + task.getException());
                        }
                    });
        }

        private void setupViews(@NonNull User otherUserModel, @NonNull Chatroom model) {
            // Display user information
            Glide.with(context)
                    .load(otherUserModel.getAvatar())
                    .into(avatarImageView);
            nameTextView.setText(otherUserModel.getFullName());

            // Set message text appropriately
            String messageText = buildMessageText(model);
            messageTextView.setText(messageText);

            // Log for debugging
            Log.d(TAG, "Setting Message: " + messageText);

            // Set click listener for conversation
            itemView.setOnClickListener(v -> navigateToConversation(otherUserModel));
        }

        private String buildMessageText(@NonNull Chatroom model) {
            boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseInitialization.getCurrentUserId());
            return lastMessageSentByMe ? "You: " + model.getLastMessage() : model.getLastMessage();
        }

        private void navigateToConversation(@NonNull User otherUserModel) {
            Intent intent = new Intent(context, ConversationActivity.class);
            intent.putExtra("id", otherUserModel.getId());
            intent.putExtra("name", otherUserModel.getFullName());
            intent.putExtra("avatar", otherUserModel.getAvatar());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}