package Scent.Danielle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

import Scent.Danielle.Utils.ChatMessageModel;
import Scent.Danielle.Utils.ChatRecyclerAdapter;
import Scent.Danielle.Utils.ChatroomModel;
import Scent.Danielle.Utils.Database.FirebaseInitialization;


public class ChatActivity extends AppCompatActivity {

    public static final String TAG = ChatActivity.class.getSimpleName();

    private ChatroomModel chatroomModel;
    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private RecyclerView recyclerView;
    private ChatRecyclerAdapter adapter;

    private String currentUserUuid;
    private String recipientUuid;
    private String recipientName;
    private String chatRoomId;
    private String recipientAvatar;

    ImageButton backBtn;
    TextView otherUsername;
    ImageView imageView;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUsername = findViewById(R.id.other_username);
        backBtn = findViewById(R.id.back_btn);
        imageView = findViewById(R.id.profile_pic_layout);

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        recyclerView = findViewById(R.id.chat_recycler_view);

        currentUserUuid = FirebaseInitialization.getCurrentUserId();
        recipientUuid = getIntent().getStringExtra("id");
        recipientName = getIntent().getStringExtra("name");
        recipientAvatar = getIntent().getStringExtra("avatar");

        chatRoomId = getChatroomId(currentUserUuid, recipientUuid);

        initializeChatroom();

        otherUsername.setText(recipientName);
        Glide.with(this)
                .load(recipientAvatar)
                .into(imageView);


        sendMessageBtn.setOnClickListener((v -> {
            String message = messageInput.getText().toString().trim();
            if(message.isEmpty())
                return;
            sendMessageToUser(message);
        }));
        backBtn.setOnClickListener(view -> {
            onBackPressed();
        });
        setupChatRecyclerView();
    }

    private void initializeChatroom() {
        FirebaseInitialization.getChatroomReference(chatRoomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    // First time chat
                    chatroomModel = new ChatroomModel(
                            chatRoomId,
                            Arrays.asList(currentUserUuid, recipientUuid),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseInitialization.getChatroomReference(chatRoomId).set(chatroomModel);
                } else {
                    Log.e(TAG, "Error fetching chatroom data", task.getException());
                }
            }
        });
    }



    private void setupChatRecyclerView(){
        Query query = FirebaseInitialization.getChatroomMessageReference(chatRoomId).orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query,ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options,getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    private void sendMessageToUser(String message){

        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseInitialization.getCurrentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseInitialization.getChatroomReference(chatRoomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message,FirebaseInitialization.getCurrentUserId(),Timestamp.now());

        FirebaseInitialization.getChatroomMessageReference(chatRoomId).add(chatMessageModel)
                .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentReference> task) {
                        if(task.isSuccessful()){
                            messageInput.setText("");
                            Log.i("ChatActivity", "Message sent successfully");
                            //sendNotification(message);
                        } else {
                            Log.e("ChatActivity", "Error sending message", task.getException());
                        }
                    }
                });
    }


    private String getChatroomId(final String currentUserUuid, final String recipientUuid) {
        return (currentUserUuid.hashCode() < recipientUuid.hashCode()) ? currentUserUuid + "_" + recipientUuid : recipientUuid + "_" + currentUserUuid;
    }
}