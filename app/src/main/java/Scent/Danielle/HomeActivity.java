package Scent.Danielle;

// Import necessary Android libraries
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

// Import AppCompatActivity and MaterialToolbar from androidx
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

// Import FirebaseAuth from com.google.firebase.auth for user authentication
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupAppBar();
    }

    private void initViews() {
        topAppBar = findViewById(R.id.topAppBar);
    }

    private void setupAppBar() {
        topAppBar.setOnMenuItemClickListener(new MaterialToolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
                return handleMenuItemClick(menuItem); // Call the method to handle menu item clicks
            }
        });
    }

    private boolean handleMenuItemClick(MenuItem menuItem) {
        int itemId = menuItem.getItemId(); // Get the ID of the clicked menu item

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

    private void handleFavoriteIconPress() {
        // Handle favorite icon press
    }

    private void handleSearchIconPress() {
        // Handle search icon press
    }

    private void handleMoreItemPress() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }
}