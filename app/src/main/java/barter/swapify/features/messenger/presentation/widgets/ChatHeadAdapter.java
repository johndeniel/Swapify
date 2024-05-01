package barter.swapify.features.messenger.presentation.widgets;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;

import barter.swapify.R;
import barter.swapify.core.typedef.User;
import barter.swapify.core.widgets.shimmer.GlideShimmerHelper;
import barter.swapify.features.chatroom.presentation.pages.ChatRoomPage;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatHeadAdapter extends FirestoreRecyclerAdapter<Chatroom, ChatHeadAdapter.ViewHolder> {
    private static final String TAG = ChatHeadAdapter.class.getSimpleName();
    private final String uid;
    private final Context context;

    public ChatHeadAdapter(@NonNull FirestoreRecyclerOptions<Chatroom> options, String uid, Context context) {
        super(options);
        this.uid = uid;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.messenger_presentation_recent_chat_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Chatroom model) {
        holder.bindItem(uid, model);
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView messageTextView;
        private final TextView timeStampTextView;
        private final ShimmerFrameLayout shimmerFrameLayout;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            timeStampTextView = itemView.findViewById(R.id.timestamp);
            shimmerFrameLayout = itemView.findViewById(R.id.AvatarShimmerFrameLayout);
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
            // Set the name of the user
            nameTextView.setText(otherUserModel.getFullName());

            // Set the name of the user
            String messageText = buildMessageText(model, uid);
            messageTextView.setText(messageText);

            // Format the date to a String with 12 hour clock and AM/PM
            String timeString = formatTimestamp(model.getLastMessageTimestamp());
            timeStampTextView.setText(timeString);

            // Set the name of the user
            Glide.with(itemView.getContext())
                    .load(otherUserModel.getAvatar())
                    .placeholder(R.drawable.rectangle)
                    .centerCrop()
                    .listener(new GlideShimmerHelper(shimmerFrameLayout))
                    .into(avatarImageView);

            // Set the name of the user
            itemView.setOnClickListener(v -> navigateToConversation(otherUserModel));
        }
    }

    private String buildMessageText(@NonNull Chatroom model, String uid) {
        boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(uid);
        return lastMessageSentByMe ? "You: " + model.getLastMessage() : model.getLastMessage();
    }

    private void navigateToConversation(@NonNull User otherUserModel) {
        Intent intent = new Intent(context, ChatRoomPage.class);
        intent.putExtra("id", otherUserModel.getId());
        intent.putExtra("name", otherUserModel.getFullName());
        intent.putExtra("avatar", otherUserModel.getAvatar());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private String formatTimestamp(Timestamp timestamp) {
        long timestampInMillis = timestamp.getSeconds() * 1000;
        Date date = new Date(timestampInMillis);
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm aa");
        return formatter.format(date);
    }

    private static DocumentReference getOtherUserFromChatroom(String uid, List<String> userIds) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (userIds.get(0).equals(uid)) {
            return db.collection("users").document(userIds.get(1));
        } else {
            return db.collection("users").document(userIds.get(0));
        }
    }
}
