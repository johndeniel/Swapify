package barter.swapify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Optional;

import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.route.Navigator;
import barter.swapify.features.auth.presentation.pages.LoginPage;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainEntryPoint extends AppCompatActivity {
    private static final long SPLASH_DELAY = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        ImageView logoImageView = findViewById(R.id.logoImageView);
        animateLogo(logoImageView);

        new Handler().postDelayed(() -> {
            CredentialNotifiers credentialNotifiers = new CredentialNotifiers(this);
            new CompositeDisposable().add(credentialNotifiers.getCredential()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(result -> {
                        if (result.isRight()) {
                            CredentialEntity credentialEntity = result.getRight();
                            Intent intent = new Intent(this,
                                    Optional.ofNullable(credentialEntity).isPresent()
                                            ? Navigator.class : LoginPage.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Intent intent = new Intent(this, LoginPage.class);
                            startActivity(intent);
                            finish();
                        }
                    }));
        }, SPLASH_DELAY);
    }

    private void animateLogo(ImageView imageView) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setDuration(1500);
        imageView.startAnimation(fadeIn);
    }
}
