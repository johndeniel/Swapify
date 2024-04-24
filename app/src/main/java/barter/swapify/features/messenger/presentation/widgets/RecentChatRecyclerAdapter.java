package barter.swapify.features.messenger.presentation.widgets;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<Chatroom, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {
    private static final String TAG = RecentChatRecyclerAdapter.class.getSimpleName();

    private final Context context;
    private final String uid;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<Chatroom> options, @NonNull Context context, String uid) {
        super(options);
        this.context = context;
        this.uid = uid;
    }

    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatroomModelViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull Chatroom model) {
        holder.bindItem(uid, model);
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatarImageView;

        ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
        }

        void bindItem(String uid, @NonNull Chatroom model) {
            getOtherUserFromChatroom(uid, model.getUserIds())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            User otherUserModel = task.getResult().toObject(User.class);
                            if (otherUserModel != null) {
                                setupViews(otherUserModel);
                            } else {
                                // Handle null user model
                                Log.e(TAG, "User model is null");
                            }
                        } else {
                            // Handle Firestore task exception
                            Log.e(TAG, "Error getting other user data: " + task.getException());
                        }
                    });
        }

        private void setupViews(@NonNull User otherUserModel) {
            Glide.with(itemView.getContext())
                    .load(otherUserModel.getAvatar())
                    .into(avatarImageView);
            // Set other views and listeners as needed
        }
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
