package barter.swapify.core.route;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import barter.swapify.R;
import barter.swapify.features.swipe.presentation.pages.SwipePage;

public class Navigator extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.core_route_navigator);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SwipePage())
                    .commit();
        }
    }
}
