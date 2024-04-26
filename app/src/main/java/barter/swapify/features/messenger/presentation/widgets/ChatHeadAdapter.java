package barter.swapify.features.messenger.presentation.widgets;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import barter.swapify.R;
import barter.swapify.core.typedef.User;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatHeadAdapter extends FirestoreRecyclerAdapter<Chatroom, ChatHeadAdapter.ViewHolder> {
    private static final String TAG = ChatHeadAdapter.class.getSimpleName();

    private final String uid;

    public ChatHeadAdapter(@NonNull FirestoreRecyclerOptions<Chatroom> options, String uid) {
        super(options);
        this.uid = uid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Chatroom model) {
        holder.bindItem(uid, model);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView messageTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        void bindItem(String uid, @NonNull Chatroom model) {
            getOtherUserFromChatroom(uid, model.getUserIds())
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            try {
                                User otherUserModel = task.getResult().toObject(User.class);
                                setupViews(otherUserModel, model, uid);
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing user data", e);
                            }
                        } else {
                            Log.e(TAG, "Error getting other user data: " + task.getException());
                        }
                    });
        }

        private void setupViews(@NonNull User otherUserModel, @NonNull Chatroom model, String uid) {
            Glide.with(itemView.getContext())
                    .load(otherUserModel.getAvatar())
                    .into(avatarImageView);

            nameTextView.setText(otherUserModel.getFullName());

            // Set message text appropriately
            String messageText = buildMessageText(model, uid);
            messageTextView.setText(messageText);
        }
    }

    private String buildMessageText(@NonNull Chatroom model, String uid) {
        boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(uid);
        return lastMessageSentByMe ? "You: " + model.getLastMessage() : model.getLastMessage();
    }


    private static DocumentReference getOtherUserFromChatroom(String uid, List<String> userIds) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (userIds.get(0).equals(uid)) {
            return firestore.collection("users").document(userIds.get(1));
        } else {
            return firestore.collection("users").document(userIds.get(0));
        }
    }
}
