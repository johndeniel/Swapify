package Scent.Danielle;

import static java.security.AccessController.getContext;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import android.widget.SearchView;
import com.google.firebase.firestore.Query;

import Scent.Danielle.Utils.ChatroomModel;
import Scent.Danielle.Utils.Database.FirebaseInitialization;
import Scent.Danielle.Utils.RecentChatRecyclerAdapter;
import Scent.Danielle.Utils.SearchUserRecyclerAdapter;
import Scent.Danielle.Utils.User;

public class MessageActivity extends AppCompatActivity {

    private static final String TAG = MessageActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private RecyclerView recentChats;
    private SearchView searchView;
    private SearchUserRecyclerAdapter adapter;
    RecentChatRecyclerAdapter adapter2;
    private MaterialToolbar chatTopAppBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // Initialize UI views
        initializeViews();
        // Setup listeners for UI elements
        setupListeners();

        setupHistory();
        handleSearchItemClick();
    }

    private void initializeViews() {
        chatTopAppBar = findViewById(R.id.chatTopAppBar);
        searchView = chatTopAppBar.findViewById(R.id.searches);
        recyclerView = findViewById(R.id.search_user_recycler_view);
        recentChats = findViewById(R.id.recentChats);
    }

    private void setupListeners() {
        chatTopAppBar.setNavigationOnClickListener(this::handleBackItemClick);
    }

    private void handleBackItemClick(View view) {
        onBackPressed();
    }

    private void setupHistory() {
        Query query = FirebaseInitialization.allChatroomCollectionReference()
                .whereArrayContains("userIds",FirebaseInitialization.getCurrentUserId())
                .orderBy("lastMessageTimestamp",Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<ChatroomModel> options = new FirestoreRecyclerOptions.Builder<ChatroomModel>()
                .setQuery(query, ChatroomModel.class).build();

        adapter2 = new RecentChatRecyclerAdapter(options,this);
        recentChats.setLayoutManager(new LinearLayoutManager(this));
        recentChats.setAdapter(adapter2);
        adapter2.startListening();
    }

    private void handleSearchItemClick() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                setupSearchRecyclerView(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                setupSearchRecyclerView(newText);
                return true;
            }
        });
    }

    private void setupSearchRecyclerView(String searchTerm) {
        try {
            Query query = FirebaseInitialization.allUserCollectionReference()
                    .whereGreaterThanOrEqualTo("fullName", searchTerm)
                    .whereLessThanOrEqualTo("fullName", searchTerm + '\uf8ff');

            FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                    .setQuery(query, User.class)
                    .build();

            adapter = new SearchUserRecyclerAdapter(options, this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);
            adapter.startListening();

            Log.d(TAG, "Search RecyclerView set up for term: " + searchTerm);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView", e);

        }
    }
}