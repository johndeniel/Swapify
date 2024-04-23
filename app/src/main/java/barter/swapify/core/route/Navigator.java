package barter.swapify.core.route;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import barter.swapify.R;
import barter.swapify.features.chat.presentation.pages.ContactPage;
import barter.swapify.features.explore.presentation.pages.ExplorePage;
import barter.swapify.features.post.presentation.pages.NewPostPage;
import barter.swapify.features.profile.presentation.pages.ProfilePage;
import barter.swapify.features.swipe.presentation.pages.SwipePage;

public class Navigator extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.core_route_navigator);

        hideNavigationBar();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.swipe_navigation);
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadDefaultFragment();
        }
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void setupBottomNavigation() {
        //noinspection deprecation
        bottomNavigationView.setOnNavigationItemSelectedListener(this::navigate);
    }

    private boolean navigate(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.explore_navigation) {
            replaceFragment(new ExplorePage());
            return true;
        } else if (itemId == R.id.swipe_navigation) {
            replaceFragment(new SwipePage());
            return true;
        } else if (itemId == R.id.post_navigation) {
            Intent intent = new Intent(this, NewPostPage.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.chat_navigation) {
            replaceFragment(new ContactPage());
            return true;
        } else if (itemId == R.id.me_navigation) {
            replaceFragment(new ProfilePage());
            return true;
        }

        return false;
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    private void loadDefaultFragment() {
        Fragment defaultFragment = new SwipePage();
        replaceFragment(defaultFragment);
    }
}
