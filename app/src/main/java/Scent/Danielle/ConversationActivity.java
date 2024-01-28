package Scent.Danielle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import Scent.Danielle.Utils.DataModel.Chatroom;
import Scent.Danielle.Utils.Database.FirebaseInitialization;

public class ConversationActivity extends AppCompatActivity {
    // TAG for logging
    public static final String TAG = ConversationActivity.class.getSimpleName();

    // RecyclerView for displaying conversation messages
    private RecyclerView conversationRecyclerView;

    // EditText field for composing messages
    private EditText composeMessageField ;

    // Chatroom object to hold conversation data
    private Chatroom chatroom;

    // Variables to hold user and recipient information
    private String currentUserUuid;
    private String recipientUuid;
    private String chatRoomId;
    private String recipientAvatar;
    private String recipientName;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        // Get user and recipient information from intent
        currentUserUuid = FirebaseInitialization.getCurrentUserId();
        recipientUuid = getIntent().getStringExtra("id");
        recipientName = getIntent().getStringExtra("name");
        recipientAvatar = getIntent().getStringExtra("avatar");

        // Setup top app bar with recipient information
        handleSetupTopAppBar();

        // Setup compose message field
        handleSetupComposeAndSendMessageField();

        // Initialize views
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);

        // Generate chat room ID based on user IDs
        chatRoomId = getChatroomId(currentUserUuid, recipientUuid);

        // Retrieve existing chatroom data or create a new one
        handleSetupChatroom();

        // RecyclerView for displaying conversation messages
        handleSetupChatRecyclerView();
    }

    private void handleSetupTopAppBar() {
        ImageButton backButton = findViewById(R.id.backButton);
        TextView recipientUsernameTextView = findViewById(R.id.recipientUsernameTextView);
        ImageView recipientProfileImageView = findViewById(R.id.recipientProfileImageView);

        backButton.setOnClickListener(view -> onBackPressed());
        recipientUsernameTextView.setText(recipientName);
        Glide.with(this)
                .load(recipientAvatar)
                .into(recipientProfileImageView);
    }

    private void handleSetupComposeAndSendMessageField() {
        composeMessageField = findViewById(R.id.chat_message_input);
        ImageButton sendMessageButton = findViewById(R.id.sendMessageButton);
        sendMessageButton.setOnClickListener(v -> {
            String message = composeMessageField.getText().toString().trim();
            if (TextUtils.isEmpty(message)) {
                Log.d(TAG, "Empty message. No action taken.");
                return;
            }
            try {
                handleSendMessageToUser(message);
                Log.i(TAG, "Message sent successfully: " + message);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message:", e);
            }
        });
    }

    private void handleSetupChatroom() {
        // Retrieve the chatroom data from FireStore
        FirebaseInitialization.getChatroomReference(chatRoomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // If chatroom data retrieval is successful
                if (task.getResult() != null) {
                    // Extract the chatroom object from the FireStore document
                    chatroom = task.getResult().toObject(Chatroom.class);
                }
                if (chatroom == null) {
                    // If the chatroom doesn't exist, create a new one
                    handleCreateNewChatroom();
                }
            } else {
                Log.e(TAG, "Error fetching chatroom data", task.getException());
            }
        });
    }

    private void handleCreateNewChatroom() {
        // Initialize a new chatroom object
        chatroom = new Chatroom(chatRoomId, Arrays.asList(currentUserUuid, recipientUuid), Timestamp.now(), "");

        // Save the new chatroom to FireStore
        FirebaseInitialization.getChatroomReference(chatRoomId).set(chatroom)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "New chatroom created successfully: " + chatRoomId))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating new chatroom", e));
    }

    private void handleSetupChatRecyclerView() {
        // Prepare the query to fetch chat messages ordered by timestamp
        Query query = FirebaseInitialization.getChatroomMessageReference(chatRoomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Configure options for the FireStoreRecyclerAdapter
        FirestoreRecyclerOptions<ConversationMessageModel> options = new FirestoreRecyclerOptions.Builder<ConversationMessageModel>()
                .setQuery(query, ConversationMessageModel.class)
                .build();

        // Create the adapter for the RecyclerView
        ConversationThreadRecyclerAdapter adapter = new ConversationThreadRecyclerAdapter(options, getApplicationContext());

        // Set layout manager to display items in reverse chronological order
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        conversationRecyclerView.setLayoutManager(layoutManager);

        // Attach the adapter to the RecyclerView
        conversationRecyclerView.setAdapter(adapter);

        // Start listening for changes in the FireStore data
        adapter.startListening();

        // Scroll to the latest message when a new message is added
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                conversationRecyclerView.smoothScrollToPosition(0);
            }
        });
    }


    private void handleSendMessageToUser(String message){
        // Update last message information in chatroom
        chatroom.setLastMessageTimestamp(Timestamp.now());
        chatroom.setLastMessageSenderId(FirebaseInitialization.getCurrentUserId());
        chatroom.setLastMessage(message);

        // Save the updated chatroom data to FireStore
        FirebaseInitialization.getChatroomReference(chatRoomId).set(chatroom)
                .addOnSuccessListener(aVoid -> {
                    // Create a message object with the provided details
                    ConversationMessageModel chatMessageModel = new ConversationMessageModel(
                            message,
                            FirebaseInitialization.getCurrentUserId(),
                            Timestamp.now()
                    );

                    // Add the message to FireStore
                    FirebaseInitialization.getChatroomMessageReference(chatRoomId).add(chatMessageModel)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Clear the compose message field after sending the message
                                    composeMessageField.setText("");
                                    Log.i(TAG, "Message sent successfully");
                                } else {
                                    // Log an error message if message sending fails
                                    Log.e(TAG, "Error sending message", task.getException());
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    // Log an error message if updating the chatroom data fails
                    Log.e(TAG, "Error updating chatroom data", e);
                });
    }


    private String getChatroomId(final String currentUserUuid, final String recipientUuid) {
        return (currentUserUuid.hashCode() < recipientUuid.hashCode()) ? currentUserUuid + "_" + recipientUuid : recipientUuid + "_" + currentUserUuid;
    }


    private static class ConversationMessageModel {
        private String message;
        private String senderId;
        private Timestamp timestamp;

        public ConversationMessageModel() {
            // Required empty public constructor
        }

        public ConversationMessageModel(String message, String senderId, Timestamp timestamp) {
            this.message = message;
            this.senderId = senderId;
            this.timestamp = timestamp;
        }

        public String getMessage() {
            return message;
        }

        public String getSenderId() {
            return senderId;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }
    }


    private static class ConversationThreadRecyclerAdapter extends FirestoreRecyclerAdapter<ConversationMessageModel, ConversationActivity.ConversationThreadViewHolder> {

        private final Context context;

        public ConversationThreadRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ConversationMessageModel> options, @NonNull Context context) {
            super(options);
            this.context = context;
        }

        @NonNull
        @Override
        public ConversationActivity.ConversationThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.temp_conversation, parent, false);
            return new ConversationActivity.ConversationThreadViewHolder(view);
        }

        @Override
        protected void onBindViewHolder(@NonNull ConversationActivity.ConversationThreadViewHolder holder, int position, @NonNull ConversationMessageModel model) {
            holder.handleRenderConversationMessage(model);
        }
    }


    private static class ConversationThreadViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftMessageContainer;
        LinearLayout rightMessageContainer;
        TextView leftChatTextview;
        TextView rightChatTextview;

        public ConversationThreadViewHolder(@NonNull View itemView) {
            super(itemView);
            leftMessageContainer = itemView.findViewById(R.id.leftMessageContainer);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightMessageContainer = itemView.findViewById(R.id.rightMessageContainer);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
        }

        private void handleRenderConversationMessage( @NonNull ConversationMessageModel model) {
            if(model.getSenderId().equals(FirebaseInitialization.getCurrentUserId())){
                leftMessageContainer.setVisibility(View.GONE);
                rightMessageContainer.setVisibility(View.VISIBLE);
                rightChatTextview.setText(model.getMessage());
            }else{
                rightMessageContainer.setVisibility(View.GONE);
                leftMessageContainer.setVisibility(View.VISIBLE);
                leftChatTextview.setText(model.getMessage());
            }
        }
    }
}