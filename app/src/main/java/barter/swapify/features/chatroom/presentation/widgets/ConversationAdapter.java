package barter.swapify.features.chatroom.presentation.widgets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import barter.swapify.R;
import barter.swapify.features.chatroom.domain.entity.ChatEntity;

public class ConversationAdapter extends FirestoreRecyclerAdapter<ChatEntity, ConversationAdapter.ViewHolder> {
    private final Context context;
    private final String uid;

    public ConversationAdapter(@NonNull FirestoreRecyclerOptions<ChatEntity> options,
                               @NonNull String uid,
                               @NonNull Context context) {
        super(options);
        this.context = context;
        this.uid = uid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.temp_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull ChatEntity model) {
        holder.bind(model);
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout leftMessageContainer;
        private final LinearLayout rightMessageContainer;
        private final TextView leftChatTextview;
        private final TextView rightChatTextview;

        public ViewHolder(@NonNull View view) {
            super(view);
            leftMessageContainer = view.findViewById(R.id.leftMessageContainer);
            leftChatTextview = view.findViewById(R.id.left_chat_textview);
            rightMessageContainer = view.findViewById(R.id.rightMessageContainer);
            rightChatTextview = view.findViewById(R.id.right_chat_textview);
        }

        private void bind(@NonNull ChatEntity model) {
            if (model.getSenderId().equals(uid)) {
                leftMessageContainer.setVisibility(View.GONE);
                rightMessageContainer.setVisibility(View.VISIBLE);
                rightChatTextview.setText(model.getMessage());
            } else {
                rightMessageContainer.setVisibility(View.GONE);
                leftMessageContainer.setVisibility(View.VISIBLE);
                leftChatTextview.setText(model.getMessage());
            }
        }
    }
}
