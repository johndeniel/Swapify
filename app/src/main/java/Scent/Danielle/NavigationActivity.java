package Scent.Danielle;

// Android core components
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

// Third-party library for image loading
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
                    .replace(R.id.content_frame, new SwipeActivity())
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
        if (itemId == R.id.about) {
            handleMoreAboutItemPress();
            return true;
        }
        return false;
    }

    private void handleMoreAboutItemPress() {
        Log.d(TAG, "About item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new AboutActivity())
                .commit();
    }

    private boolean handleNavigationItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (itemId == R.id.swipe) {
            handleSwipeItemClick();
            return true;
        } else if (itemId == R.id.gallery) {
            handleGalleryItemClick();
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

    private void handleSwipeItemClick() {
        Log.d(TAG, "Swipe item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new SwipeActivity())
                .commit();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void handleGalleryItemClick() {
        Log.d(TAG, "Gallery item clicked");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new GalleryActivity())
                .commit();
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void handleChatItemClick() {
        Log.d(TAG, "Chat item clicked");
        Intent intent = new Intent(this, MessageActivity.class);
        startActivity(intent);
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
        Intent intent = new Intent(this, AuthenticationActivity.class);
        startActivity(intent);
        finish();
    }
}