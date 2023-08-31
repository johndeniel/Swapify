package Scent.Danielle;

// Android core components
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX components
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

// Google Sign-In components
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

// Google Material components
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

// Firebase components
import com.google.android.material.search.SearchBar;
import com.google.firebase.auth.FirebaseAuth;

// Third-party library for image loading
import com.bumptech.glide.Glide;

public class NavigationActivity extends AppCompatActivity {

    // Constants
    public static final String TAG = NavigationActivity.class.getSimpleName();

    // UI Elements
    private MaterialToolbar topAppBar;
    private NavigationView navigationView;
    private SearchBar searchBar;
    private SearchView searchView;
    private DrawerLayout drawerLayout;
    private ImageView avatarImageView;
    private TextView fullNameTextView;
    private TextView emailTextView;

    // Authentication
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // Initialize the Google Sign-In client for user authentication
        initializeGoogleSignInClient();
        // Initialize UI views
        initializeViews();
        // Setup listeners for UI elements
        setupListeners();
        // Display user information in the navigation header
        displayUserInfo();

        // If no saved instance state, replace content frame with FeedActivity
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new FeedActivity())
                    .commit();
        }
    }

    private void initializeGoogleSignInClient() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void initializeViews() {
        topAppBar = findViewById(R.id.topAppBar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        searchBar = findViewById(R.id.search_bar);
        searchView = findViewById(R.id.search_view);
        avatarImageView = navigationView.getHeaderView(0).findViewById(R.id.avatarImageView);
        fullNameTextView = navigationView.getHeaderView(0).findViewById(R.id.fullName);
        emailTextView = navigationView.getHeaderView(0).findViewById(R.id.gmailAddress);
    }

    private void setupListeners() {
        topAppBar.setOnMenuItemClickListener(this::handleMenuItemClick);
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        navigationView.setNavigationItemSelectedListener(this::handleNavigationItemClick);
    }

    private void displayUserInfo() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);

        if (account != null) {
            String fullName = account.getDisplayName();
            String email = account.getEmail();
            Uri photoUri = account.getPhotoUrl();

            fullNameTextView.setText(fullName);
            emailTextView.setText(email);
            if (photoUri != null) {
                Glide.with(this)
                        .load(photoUri)
                        .into(avatarImageView);
            }
        }
    }

    private boolean handleMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (itemId == R.id.heart) {
            handleHeartIconPress();
            return true;
        } else if (itemId == R.id.search) {
            handleSearchIconPress();
            return true;
        } else if (itemId == R.id.morePlan) {
            handleMorePlanItemPress();
            return true;
        } else if (itemId == R.id.moreAbout) {
            handleMoreAboutItemPress();
            return true;
        }
        return false;
    }

    private void handleHeartIconPress() {
        // TODO: Implement favorite icon press functionality
        Log.d(TAG, "Heart item clicked");
    }

    private void handleSearchIconPress() {
        Log.d(TAG, "Search item clicked");

        View customView = getLayoutInflater().inflate(R.layout.item_search, null);
        //SearchBar searchBar2 = customView.findViewById(R.id.search_bar);
        //SearchView searchView2 = customView.findViewById(R.id.search_view);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Search Item")
                .setView(customView)
                .setPositiveButton("Search", null)
                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .show();

        Button searchButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        searchButton.setOnClickListener(v -> {
            dialog.dismiss();
        });
    }

    private void handleMorePlanItemPress() {
        Log.d(TAG, "Plan item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new SubscriptionActivity())
                .commit();
    }
    private void handleMoreAboutItemPress() {
        Log.d(TAG, "About item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new AboutActivity())
                .commit();
    }

    private boolean handleNavigationItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (itemId == R.id.feed) {
            handleFeedItemClick();
            return true;
        } else if (itemId == R.id.chat) {
            handleChatItemClick();
            return true;

        } else if (itemId == R.id.about) {
            handleAboutItemClick();
            return true;
        }else if (itemId == R.id.logout) {
            handleLogoutItemClick();
            return true;
        }
        return false;
    }

    private void handleFeedItemClick() {
        Log.d(TAG, "Feed item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new FeedActivity())
                .commit();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void handleChatItemClick() {
        Log.d(TAG, "Chat item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new MessageActivity())
                .commit();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void handleAboutItemClick() {
        Log.d(TAG, "About item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new AboutActivity())
                .commit();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void handleLogoutItemClick() {
        Log.d(TAG, "Logout item clicked");

        // Sign out from Google account
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Google Sign-Out successful");
                            Toast.makeText(this, "Logout Successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Error during Google Sign-Out", task.getException());
                            Toast.makeText(this, "Error during Google Sign-Out", Toast.LENGTH_SHORT).show();
                        }
                        performLocalSignOut();
                    });
        } else {
            Log.e(TAG, "Error during Google Sign-Out");
            Toast.makeText(this, "Error during Google Sign-Out", Toast.LENGTH_SHORT).show();
        }
    }

    private void performLocalSignOut() {
        Log.d(TAG, "LocalSignOut Successful");

        // Sign out from Firebase authentication
        FirebaseAuth.getInstance().signOut();

        // Start AuthActivity and finish current activity
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }
}