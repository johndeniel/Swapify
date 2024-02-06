package Scent.Danielle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import Scent.Danielle.Utils.Database.FirebaseInitialization;
import de.hdodenhof.circleimageview.CircleImageView;

public class SwipeRightActivity extends AppCompatActivity {

    private static final String TAG = SwipeRightActivity.class.getSimpleName();
    private final List<Swipe> swipeList = new ArrayList<>();
    private SwipeRecyclerView swipeRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe_right);
        RecyclerView swipeRightRecyclerView = findViewById(R.id.swipeRightRecyclerView);
        swipeRightRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        swipeRecyclerView = new SwipeRecyclerView(swipeList, this);
        swipeRightRecyclerView.setAdapter(swipeRecyclerView);
        fetchSwipesFromFirebase();
    }

    private void fetchSwipesFromFirebase() {

        String recipientUuid = getIntent().getStringExtra("key");
        DatabaseReference itemSnapshot = FirebaseInitialization.getItemsDatabaseReference().child(recipientUuid).child("swipe");
        Query query = itemSnapshot.orderByChild("like").equalTo(true);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "Snapshot: " + dataSnapshot);
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Swipe item = snapshot.getValue(Swipe.class);
                    if (item != null) {
                        swipeList.add(item);
                    }
                }

                swipeRecyclerView.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error querying swipe data: " + databaseError.getMessage());
            }
        });
    }

    private final static class Swipe {
        private String userId;
        private String fullName;
        private String photoUrl;
        private boolean like;

        public Swipe() {}

        public Swipe(String userId, String fullName, String photoUrl, boolean like) {
            this.userId = userId;
            this.fullName = fullName;
            this.photoUrl = photoUrl;
            this.like = like;
        }

        public String getUserId() {
            return userId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public boolean isLike() {
            return like;
        }
    }

    private static class SwipeRecyclerView extends RecyclerView.Adapter<SwipeRightActivity.SwipeDisplayHolder>{
        private final List<Swipe> swipeList;
        private final Context context;

        public SwipeRecyclerView(List<Swipe> swipeList, Context context) {
            this.swipeList = swipeList;
            this.context = context;
        }

        @NonNull
        @Override
        public SwipeRightActivity.SwipeDisplayHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.temp_swiperight, parent, false);
            return new SwipeDisplayHolder(view, context);
        }

        @Override
        public void onBindViewHolder(@NonNull SwipeDisplayHolder holder, int position) {
            Swipe currentItem = swipeList.get(position);
            holder.setupViews(currentItem);
        }

        @Override
        public int getItemCount() {
            return swipeList.size();
        }
    }

    private static class SwipeDisplayHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatarImageView;
        private final TextView nameTextView;
        private final TextView chatTextView;
        private final Context context;

        public SwipeDisplayHolder(@NonNull View itemView, @NonNull Context context) {
            super(itemView);
            this.context = context;
            avatarImageView = itemView.findViewById(R.id.swipeAvatarImageView);
            nameTextView = itemView.findViewById(R.id.swipeNameTextView);
            chatTextView = itemView.findViewById(R.id.chatTextView);
        }

        private void setupViews(@NonNull Swipe swipe) {
            Glide.with(context)
                    .load(swipe.getPhotoUrl())
                    .into(avatarImageView);
            nameTextView.setText(swipe.getFullName());

            chatTextView.setOnClickListener(view -> {
                converstation(swipe);
            });
        }

        private void converstation(Swipe swipe) {
            Intent intent = new Intent(context, ConversationActivity.class);
            intent.putExtra("id", swipe.getUserId());
            intent.putExtra("name", swipe.getFullName());
            intent.putExtra("avatar", swipe.getPhotoUrl());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

        }
    }
}