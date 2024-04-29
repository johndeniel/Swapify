package barter.swapify.features.chatroom.presentation.pages;

import static barter.swapify.core.constants.Constants.CHATROOM_REFERENCE;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.chatroom.domain.entity.ConversationMessageModel;
import barter.swapify.features.chatroom.domain.entity.FirebaseInitialization;
import barter.swapify.features.chatroom.presentation.widgets.ConversationThreadRecyclerAdapter;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ChatRoomPage extends AppCompatActivity {
    public static final String TAG = ChatRoomPage.class.getSimpleName();

    // RecyclerView for displaying conversation messages
    private RecyclerView conversationRecyclerView;

    // EditText field for composing messages
    private EditText composeMessageField;

    private CompositeDisposable disposable;
    // Chatroom object to hold conversation data
    private Chatroom chatroom;

    // Variables to hold user and recipient information
    private String currentUserUuid;
    private String recipientUuid;
    private String chatRoomId;
    private String recipientAvatar;
    private String recipientName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room_page);
        disposable = new CompositeDisposable();

        // Initialize views
        initializeViews();

        // Get user and recipient information from intent extras
        handleIntentExtras();

        // Setup top app bar with recipient information
        handleSetupTopAppBar();

        // Setup compose message field
        handleSetupComposeAndSendMessageField();

        // Initialize and fetch chatroom data
        handleSetupChatroom();

        // Setup RecyclerView for displaying conversation messages
        handleSetupChatRecyclerView();
    }


    // Method to initialize views
    private void initializeViews() {
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);
        composeMessageField = findViewById(R.id.chat_message_input);
    }

    // Method to extract recipient information from intent extras
    private void handleIntentExtras() {
        currentUserUuid = FirebaseInitialization.getCurrentUserId();
        recipientUuid = getIntent().getStringExtra("id");
        recipientName = getIntent().getStringExtra("name");
        recipientAvatar = getIntent().getStringExtra("avatar");
    }

    // Method to setup top app bar with recipient information
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

    // Method to setup compose message field
    private void handleSetupComposeAndSendMessageField() {
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

    // Method to initialize and fetch chatroom data
    private void handleSetupChatroom() {
        chatRoomId = getChatroomId(currentUserUuid, recipientUuid);
        FirebaseInitialization.getChatroomReference(chatRoomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    chatroom = task.getResult().toObject(Chatroom.class);
                }
                if (chatroom == null) {
                    handleCreateNewChatroom();
                }
            } else {
                Log.e(TAG, "Error fetching chatroom data", task.getException());
            }
        });
    }

    // Method to create a new chatroom if it doesn't exist
    private void handleCreateNewChatroom() {
        chatroom = new Chatroom(chatRoomId, Arrays.asList(currentUserUuid, recipientUuid), Timestamp.now(), "");
        FirebaseInitialization.getChatroomReference(chatRoomId).set(chatroom)
                .addOnSuccessListener(aVoid -> Log.i(TAG, "New chatroom created successfully: " + chatRoomId))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating new chatroom", e));
    }

    // Method to setup RecyclerView for displaying conversation messages
    private void handleSetupChatRecyclerView() {
        Query query = FirebaseInitialization.getChatroomMessageReference(chatRoomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ConversationMessageModel> options = new FirestoreRecyclerOptions.Builder<ConversationMessageModel>()
                .setQuery(query, ConversationMessageModel.class)
                .build();

        ConversationThreadRecyclerAdapter adapter = new ConversationThreadRecyclerAdapter(options, this, currentUserUuid);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        conversationRecyclerView.setLayoutManager(layoutManager);

        conversationRecyclerView.setAdapter(adapter);
        adapter.startListening();

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                conversationRecyclerView.smoothScrollToPosition(0);
            }
        });
    }

    // Method to send a message to the recipient
    private void handleSendMessageToUser(final String message) {
        chatroom.setLastMessageTimestamp(Timestamp.now());
        chatroom.setLastMessageSenderId(FirebaseInitialization.getCurrentUserId());
        chatroom.setLastMessage(message);

        FirebaseInitialization.getChatroomReference(chatRoomId).set(chatroom)
                .addOnSuccessListener(aVoid -> {
                    ConversationMessageModel chatMessageModel = new ConversationMessageModel(
                            message,
                            FirebaseInitialization.getCurrentUserId(),
                            Timestamp.now()
                    );

                    FirebaseInitialization.getChatroomMessageReference(chatRoomId).add(chatMessageModel)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    composeMessageField.setText("");
                                    Log.i(TAG, "Message sent successfully");
                                } else {
                                    Log.e(TAG, "Error sending message", task.getException());
                                }
                            });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating chatroom data", e));
    }

    // Method to generate a chatroom ID based on user IDs
    private String getChatroomId(final String currentUserUuid, final String recipientUuid) {
        return (currentUserUuid.hashCode() < recipientUuid.hashCode()) ? currentUserUuid + "_" + recipientUuid : recipientUuid + "_" + currentUserUuid;
    }
}