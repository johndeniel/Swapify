package Scent.Danielle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Query;
import java.util.Arrays;
import Scent.Danielle.Utils.ChatMessageModel;
import Scent.Danielle.Utils.ChatroomModel;
import Scent.Danielle.Utils.Database.FirebaseInitialization;


public class ChatActivity extends AppCompatActivity {

    private ChatroomModel chatroomModel;
    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private RecyclerView recyclerView;
    private ChatRecyclerAdapter adapter;
    private String chatRoomId;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        recyclerView = findViewById(R.id.chat_recycler_view);

        chatRoomId = getChatroomId(FirebaseInitialization.getCurrentUserId(), getIntent().getStringExtra("id"));
        FirebaseInitialization.getChatroomReference(chatRoomId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if(chatroomModel==null){
                    //first time chat
                    chatroomModel = new ChatroomModel(
                            chatRoomId,
                            Arrays.asList(FirebaseInitialization.getCurrentUserId(), getIntent().getStringExtra("id")),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseInitialization.getChatroomReference(chatRoomId).set(chatroomModel);
                }
            }
        });

        sendMessageBtn.setOnClickListener((v -> {
            String message = messageInput.getText().toString().trim();
            if(message.isEmpty())
                return;
            sendMessageToUser(message);
        }));

        setupChatRecyclerView();
    }


    private void setupChatRecyclerView() {
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
                            //sendNotification(message);
                        }
                    }
                });
    }


    private String getChatroomId(final String userId1, final String userId2) {
        return (userId1.hashCode() < userId2.hashCode()) ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }


    public class ChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatMessageModel, ChatActivity.ChatModelViewHolder> {
        Context context;

        public ChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatMessageModel> options,Context context) {
            super(options);
            this.context = context;
        }

        @NonNull
        @Override
        public ChatActivity.ChatModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row,parent,false);
            return new ChatActivity.ChatModelViewHolder(view);
        }

        @Override
        protected void onBindViewHolder(@NonNull ChatActivity.ChatModelViewHolder holder, int position, @NonNull ChatMessageModel model) {
            Log.i("haushd","asjd");
            holder.bind(model);
        }
    }


    private class ChatModelViewHolder extends RecyclerView.ViewHolder{
        LinearLayout leftChatLayout,rightChatLayout;
        TextView leftChatTextview,rightChatTextview;

        public ChatModelViewHolder(@NonNull View itemView) {
            super(itemView);

            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
        }

        private void bind(ChatMessageModel model) {
            if (model.getSenderId().equals(FirebaseInitialization.getCurrentUserId())) {
                leftChatLayout.setVisibility(View.GONE);
                rightChatLayout.setVisibility(View.VISIBLE);
                rightChatTextview.setText(model.getMessage());
            } else {
                rightChatLayout.setVisibility(View.GONE);
                leftChatLayout.setVisibility(View.VISIBLE);
                leftChatTextview.setText(model.getMessage());
            }
        }
    }
}