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
import de.hdodenhof.circleimageview.CircleImageView;

public class SearchUserRecyclerAdapter extends FirestoreRecyclerAdapter<User, SearchUserRecyclerAdapter.UserModelViewHolder> {

    private static final String TAG  = SearchUserRecyclerAdapter.class.getSimpleName();
    private Context context;

    public SearchUserRecyclerAdapter(@NonNull FirestoreRecyclerOptions<User> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull User model) {
        Glide.with(context)
                .load(model.getAvatar())
                .into(holder.avatarImageView);

        holder.nameTextView.setText(model.getFullName());
        holder.messageTextView.setText("empty");

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Clicked on item at position: " + position);
            //navigate to chat activity

            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("id", model.getId());
            intent.putExtra("name", model.getFullName());
            intent.putExtra("avatar", model.getAvatar());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat,parent,false);
        return new UserModelViewHolder(view);
    }

    class UserModelViewHolder extends RecyclerView.ViewHolder{
        CircleImageView avatarImageView;
        TextView nameTextView;
        TextView messageTextView;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the views within the chat item layout
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }
}