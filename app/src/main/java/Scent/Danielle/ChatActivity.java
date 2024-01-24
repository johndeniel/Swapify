package Scent.Danielle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Arrays;

import Scent.Danielle.Utils.ChatroomModel;
import Scent.Danielle.Utils.Database.FirebaseInitialization;


public class ChatActivity extends AppCompatActivity {

    ChatroomModel chatroomModel;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        TextView textView = findViewById(R.id.nameTextView);
        textView.setText(getIntent().getStringExtra("name"));

        String chatRoomId = getChatroomId(FirebaseInitialization.getCurrentUserId(), getIntent().getStringExtra("id"));


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
    }



    private String getChatroomId(final String userId1, final String userId2){
        if(userId1.hashCode() < userId2.hashCode()){
            return userId1+"_"+userId2;
        }else{
            return userId2+"_"+userId1;
        }
    }
}