package Scent.Danielle;

// Import necessary Android libraries
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

// Import AppCompatActivity and MaterialToolbar from androidx
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

// Import MaterialToolbar and NavigationView from com.google.android.material
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;

// Import FirebaseAuth from com.google.firebase.auth for user authentication
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize views and set up event listeners
        initViews();
        initListener();
    }

    /**
     * Initialize views from the layout XML.
     */
    private void initViews() {
        topAppBar = findViewById(R.id.topAppBar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
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
        } else if (itemId == R.id.more) {
            handleMoreItemPress();
            return true;
        }

        return false;
    }

    /**
     * Handle press of the favorite icon.
     */
    private void handleFavoriteIconPress() {
        // TODO: Implement favorite icon press functionality
    }

    /**
     * Handle press of the search icon.
     */
    private void handleSearchIconPress() {
        // TODO: Implement search icon press functionality
    }

    /**
     * Handle press of the more item in the app bar.
     */
    private void handleMoreItemPress() {
        // Sign out the user and navigate to the authentication screen
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Handle click events for navigation view items.
     */
    private boolean handleNavigationItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId();

        if (itemId == R.id.messages) {
            handleMessageItemClick();
            return true;
        } else if (itemId == R.id.store) {
            handleStoreItemClick();
            return true;
        } else if (itemId == R.id.purchases) {
            handlePurchasesItemClick();
            return true;
        } else if (itemId == R.id.faq) {
            handleFAQItemClick();
            return true;
        } else if (itemId == R.id.archive) {
            handleArchiveItemClick();
            return true;
        } else if (itemId == R.id.settings) {
            handleSettingsItemClick();
            return true;
        } else if (itemId == R.id.logout) {
            handleLogoutItemClick();
            return true;
        }

        return false;
    }

    /**
     * Handle press of the messages item in the navigation view.
     */
    private void handleMessageItemClick() {
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle press of the store item in the navigation view.
     */
    private void handleStoreItemClick() {
        Toast.makeText(this, "Messages.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle press of the purchases item in the navigation view.
     */
    private void handlePurchasesItemClick() {
        Toast.makeText(this, "Purchases.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle press of the FAQ item in the navigation view.
     */
    private void handleFAQItemClick() {
        Toast.makeText(this, "FAQ.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle press of the archive item in the navigation view.
     */
    private void handleArchiveItemClick() {
        Toast.makeText(this, "Archive.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle press of the settings item in the navigation view.
     */
    private void handleSettingsItemClick() {
        Toast.makeText(this, "Settings.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle press of the logout item in the navigation view.
     */
    private void handleLogoutItemClick() {
        Toast.makeText(this, "Logout.", Toast.LENGTH_SHORT).show();
    }
}