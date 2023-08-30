package Scent.Danielle;

// Android components
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// AndroidX components
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

// Google Sign-In components
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

// Google Material components
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

// Firebase components
import com.google.firebase.auth.FirebaseAuth;

// Third-party library
import com.bumptech.glide.Glide;

public class NavigationActivity extends AppCompatActivity {

    // Constants
    public static final String TAG = NavigationActivity.class.getSimpleName();

    // UI Elements
    private MaterialToolbar topAppBar;
    private NavigationView navigationView;
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

        // Initialize the GoogleSignInClient
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views and set up event listeners
        initViews();
        initListener();

        // Retrieve user information and display it
        displayUserInfo();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new FeedActivity())
                    .commit();
        }
    }

    /**
     * Initialize views from the layout XML.
     */
    private void initViews() {
        topAppBar = findViewById(R.id.topAppBar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        avatarImageView = navigationView.getHeaderView(0).findViewById(R.id.avatarImageView);
        fullNameTextView = navigationView.getHeaderView(0).findViewById(R.id.fullName);
        emailTextView = navigationView.getHeaderView(0).findViewById(R.id.gmailAddress);
    }

    /**
     * Display user's information in the views.
     */
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

    /**
     * Set up event listeners for the app bar and navigation drawer.
     */
    private void initListener() {
        // Set a click listener for app bar menu items
        topAppBar.setOnMenuItemClickListener(this::handleMenuItemClick);

        // Set a click listener for the app bar navigation icon
        topAppBar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Set a click listener for navigation view items
        navigationView.setNavigationItemSelectedListener(this::handleNavigationItemClick);
    }

    /**
     * Handle click events for app bar menu items.
     */
    private boolean handleMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (itemId == R.id.favorite) {
            handleFavoriteIconPress();
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

    private void handleFavoriteIconPress() {
        // TODO: Implement favorite icon press functionality
        Log.d(TAG, "Favorite item clicked");
    }

    private void handleSearchIconPress() {
        // TODO: Implement search icon press functionality
        Log.d(TAG, "Search item clicked");
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

    /**
     * Handle click events for navigation view items.
     */
    private boolean handleNavigationItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (itemId == R.id.feed) {
            handleUpgradeFeedItemClick();
            return true;
        } else if (itemId == R.id.messages) {
            handleMessageItemClick();
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

    private void handleUpgradeFeedItemClick() {
        Log.d(TAG, "Feed item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new FeedActivity())
                .commit();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void handleMessageItemClick() {
        Log.d(TAG, "Message item clicked");
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
            Toast.makeText(this, "Error during Google Sign-Out", Toast.LENGTH_SHORT).show();
        }
    }

    private void performLocalSignOut() {
        // Sign out from Firebase authentication
        FirebaseAuth.getInstance().signOut();

        // Start AuthActivity and finish current activity
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }
}