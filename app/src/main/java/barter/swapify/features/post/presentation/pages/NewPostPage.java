package barter.swapify.features.post.presentation.pages;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.route.Navigator;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.widgets.snackbar.SnackBarHelper;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.repository.PostRepository;
import barter.swapify.features.post.domain.usecases.PostUseCases;
import barter.swapify.features.post.presentation.notifiers.NewPostNotifiers;
import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NewPostPage extends DaggerAppCompatActivity {
    private static final String TAG = NewPostPage.class.getSimpleName();
    @Inject
    PostRepository postRepository;
    private ImageView postImageView;
    private EditText title;
    private EditText description;
    private CompositeDisposable compositeDisposable;
    private Uri imageUri;
    private ProgressBar loadingIndicator;
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
        loadingIndicator = findViewById(R.id.new_post_loading_indicator);
        hideLoading();
        compositeDisposable = new CompositeDisposable();
        postImageView = findViewById(R.id.post_image_viewer);
        title = findViewById(R.id.new_post_title);
        description = findViewById(R.id.new_post_description);
        Button share = findViewById(R.id.new_post_share_button);

        share.setOnClickListener(v -> fetchCredential());
        postImageView.setOnClickListener(v -> openGallery());
    }

    private void fetchCredential() {
        showLoading();
        CredentialNotifiers credentialNotifiers = new CredentialNotifiers(this);
        compositeDisposable.add(credentialNotifiers.getCredential()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleCredentialResult));
    }

    private void handleCredentialResult(Either<Failure, CredentialEntity> result) {
        if (result.isRight()) {
            CredentialEntity credentialEntity = result.getRight();
            processCredentialSuccess(credentialEntity);
        } else {
            showSnackBar(result.getLeft().getErrorMessage());
        }
    }


    private void processCredentialSuccess(CredentialEntity credentialEntity) {
        Log.d(TAG, "Uid: " + credentialEntity.getUid());
        Log.d(TAG, "Email: " + credentialEntity.getEmail());
        Log.d(TAG, "Display Name: " + credentialEntity.getDisplayName());
        Log.d(TAG, "Photo Url: " + credentialEntity.getPhotoUrl());

        String postTitle = title.getText().toString();
        String postDescription = description.getText().toString();

        if (postTitle.trim().isEmpty()) {
            showSnackBar("Title is required");
            hideLoading();
            return;
        }

        if (postDescription.trim().isEmpty()) {
            showSnackBar("Description is required");
            hideLoading();
            return;
        }

        if (imageUri == null) {
            showSnackBar("Image is required");
            hideLoading();
            return;
        }

        String uniqueFileName = "image_" + System.currentTimeMillis() + ".jpg";

        PostEntity post = new PostEntity(
                null,
                credentialEntity.getUid(),
                credentialEntity.getPhotoUrl(),
                credentialEntity.getDisplayName(),
                postTitle,
                postDescription,
                uniqueFileName,
                imageUri.toString()
        );

        postItems(post);
    }

    private void postItems(PostEntity post) {
        NewPostNotifiers newPostNotifiers = new NewPostNotifiers(post, new PostUseCases(postRepository));
        compositeDisposable.add(newPostNotifiers.post()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleSwipeFetchResult));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void handleSwipeFetchResult(Either<Failure, Boolean> result) {
        if (result.getRight()) {
            startActivity(new Intent(this, Navigator.class));
        } else {
            showSnackBar(result.getLeft().getErrorMessage());
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.new_post_tool_bar);
        toolbar.setNavigationOnClickListener(v -> navigateBack());
    }

    private void navigateBack() {
        startActivity(new Intent(this, Navigator.class));
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
            this.imageUri = imageUri;
            postImageView.setImageURI(imageUri);
        }
    }

    private void showSnackBar(String message) {
        View rootView = findViewById(android.R.id.content);
        SnackBarHelper.invoke(message, rootView);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
    }

    private void showLoading() {
        loadingIndicator.setVisibility(View.VISIBLE);
    }
}
