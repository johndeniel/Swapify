package Scent.Danielle.Utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import Scent.Danielle.ChatActivity;
import Scent.Danielle.R;
import Scent.Danielle.Utils.Database.FirebaseInitialization;
import de.hdodenhof.circleimageview.CircleImageView;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder>  {
    private Context context;
    private static final String TAG  = RecentChatRecyclerAdapter.class.getSimpleName();
    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull RecentChatRecyclerAdapter.ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {
        FirebaseInitialization.getOtherUserFromChatroom(model.getUserIds())
                .get().addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseInitialization.getCurrentUserId());


                        User otherUserModel = task.getResult().toObject(User.class);
                        Glide.with(context)
                                .load(otherUserModel.getAvatar())
                                .into(holder.avatarImageView);
                        holder.nameTextView.setText(otherUserModel.getFullName());

                        if(lastMessageSentByMe)
                            holder.messageTextView.setText("You : "+model.getLastMessage());
                        else
                            holder.messageTextView.setText(model.getLastMessage());

                    }
                });
    }

    @NonNull
    @Override
    public RecentChatRecyclerAdapter.ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat,parent,false);
        return new ChatroomModelViewHolder(view);
    }


    class ChatroomModelViewHolder extends RecyclerView.ViewHolder{
        CircleImageView avatarImageView;
        TextView nameTextView;
        TextView messageTextView;

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views within the chat item layout
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}
