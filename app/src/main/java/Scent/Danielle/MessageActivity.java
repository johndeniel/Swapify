package Scent.Danielle;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import android.widget.SearchView;
import com.google.firebase.firestore.Query;

import Scent.Danielle.Utils.Database.FirebaseInitialization;
import Scent.Danielle.Utils.SearchUserRecyclerAdapter;
import Scent.Danielle.Utils.User;

public class MessageActivity extends AppCompatActivity {

    private static final String TAG = MessageActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private SearchUserRecyclerAdapter adapter;
    private MaterialToolbar chatTopAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        initializeViews();

        chatTopAppBar.setNavigationOnClickListener(this::handleBackItemClick);
        SearchView searchView = (SearchView)  chatTopAppBar.findViewById(R.id.searches);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Handle the query submission (optional)
                if (isValidSearchTerm(query)) {
                    Log.d(TAG, "Search query submitted: " + query);
                    // Perform actions when the user submits the search query
                    setupSearchRecyclerView(query);
                } else {
                 //   showToast("Invalid search term. Please enter at least 3 characters.");
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Handle the query text changes
                if (isValidSearchTerm(newText)) {
                    Log.d(TAG, "Search query entered: " + newText);
                    // Perform actions as the user types in the search query
                    setupSearchRecyclerView(newText);
                } else {
                   // showToast("Invalid search term. Please enter at least 3 characters.");
                }
                return true;
            }
        });



    }


    private void handleBackItemClick(View view) {
        onBackPressed();
    }

    private void initializeViews() {
        chatTopAppBar = findViewById(R.id.chatTopAppBar);
        recyclerView = findViewById(R.id.search_user_recycler_view);
    }

    private boolean isValidSearchTerm(String searchTerm) {
        return !searchTerm.isEmpty() && searchTerm.length() >= 3;
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
            showToast("An error occurred while setting up the search. Please try again.");
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startAdapterListening();
        Log.d(TAG, "Activity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAdapterListening();
        Log.d(TAG, "Activity stopped");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAdapterListening();
        Log.d(TAG, "Activity resumed");
    }

    private void startAdapterListening() {
        try {
            if (adapter != null) {
                adapter.startListening();
                Log.d(TAG, "Adapter started listening");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting adapter listening", e);
        }
    }

    private void stopAdapterListening() {
        try {
            if (adapter != null) {
                adapter.stopListening();
                Log.d(TAG, "Adapter stopped listening");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping adapter listening", e);
        }
    }
}