package barter.swapify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.route.Navigator;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.auth.presentation.pages.LoginPage;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainEntryPoint extends AppCompatActivity {
    private static final String TAG = MainEntryPoint.class.getSimpleName();
    public static final long SPLASH_DELAY = 2000;
    private CompositeDisposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_entry_point_splash_screen);
    }

    @Override
    protected void onStart() {
        super.onStart();
        hideNavigation();
        animateLogo();
        startSplashDelay();
    }

    private void hideNavigation() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void animateLogo() {
        ImageView logo = findViewById(R.id.logoImageView);
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1500);
        logo.startAnimation(fadeIn);
    }

    private void startSplashDelay() {
        new Handler().postDelayed(() -> {
            disposable = new CompositeDisposable();
            fetchCredential();
        }, SPLASH_DELAY);
    }

    private void fetchCredential() {
        CredentialNotifiers credentialNotifiers = new CredentialNotifiers(this);
        disposable.add(credentialNotifiers.getCredential()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleCredentialResult));
    }

    private void handleCredentialResult(Either<Failure, CredentialEntity> credentialResult) {
        if (credentialResult.isRight()) {
            CredentialEntity credentialEntity = credentialResult.getRight();
            Log.d(TAG, "Uid: " + credentialEntity.getUid());
            Log.d(TAG, "Email: " + credentialEntity.getEmail());
            Log.d(TAG, "Display Name: " + credentialEntity.getDisplayName());
            Log.d(TAG, "Photo Url: " + credentialEntity.getPhotoUrl());
            navigateToMainScreen();

        } else {
            navigateToLoginPage();
        }
    }

    private void navigateToMainScreen() {
        Log.i(TAG, "Intent to Main Screen");
        Intent intent = new Intent(this, Navigator.class);
        startActivity(intent);
        finish();
    }

    private void navigateToLoginPage() {
        Log.i(TAG, "Intent to Login Page");
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
