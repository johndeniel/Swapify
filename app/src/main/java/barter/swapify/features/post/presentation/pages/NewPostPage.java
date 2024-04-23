package barter.swapify.features.post.presentation.pages;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;

import barter.swapify.R;
import barter.swapify.core.route.Navigator;

public class NewPostPage extends AppCompatActivity {

    private ImageView postImageView;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_presentation_new_post_page);
        setupViews();
        setupToolbar();
        hideNavigationBar();
    }

    private void setupViews() {
        postImageView = findViewById(R.id.post_image_viewer);
        postImageView.setOnClickListener(v -> openGallery());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.new_post_tool_bar);
        toolbar.setNavigationOnClickListener(v -> navigateBack());
    }

    private void navigateBack() {
        Intent intent = new Intent(this, Navigator.class);
        startActivity(intent);
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    /** @noinspection deprecation*/
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            handleImageSelection(data.getData());
        }
    }

    private void handleImageSelection(Uri imageUri) {
        if (imageUri != null) {
            postImageView.setImageURI(imageUri);
        }
    }
}
