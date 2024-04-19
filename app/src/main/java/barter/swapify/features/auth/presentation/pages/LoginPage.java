package barter.swapify.features.auth.presentation.pages;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.route.Navigator;
import barter.swapify.core.widgets.snackbar.SnackBarHelper;
import barter.swapify.features.auth.domain.entity.AuthEntity;
import barter.swapify.features.auth.domain.repository.AuthRepository;
import barter.swapify.features.auth.domain.usecases.AuthUseCases;
import barter.swapify.features.auth.domain.usecases.LogoutUseCases;
import barter.swapify.features.auth.presentation.notifiers.LoginNotifier;
import barter.swapify.features.auth.presentation.widgets.GoogleSignInPopup;
import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginPage extends DaggerAppCompatActivity {
    private static final String TAG = LoginPage.class.getSimpleName();
    @Inject public AuthRepository provideAuthRepository;
    private CompositeDisposable compositeDisposable;
    private ProgressBar loadingIndicator;
    private GoogleSignInPopup googleSignInPopup;
    private CredentialNotifiers credentialNotifiers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_presentation_login_page);
        hideNavigation();
        compositeDisposable = new CompositeDisposable();
        credentialNotifiers = new CredentialNotifiers(this);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        googleSignInPopup = new GoogleSignInPopup(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Button signInButton = findViewById(R.id.google_login_button);
        signInButton.setOnClickListener(v -> googleSignInPopup.signIn());
        hideLoading();
    }

    private void hideNavigation() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        GoogleSignInAccount account = googleSignInPopup.handleSignInResult(requestCode, data);

        showLoading();

        AuthUseCases authUseCases = new AuthUseCases(provideAuthRepository);
        LogoutUseCases logoutUseCases = new LogoutUseCases(provideAuthRepository);

        LoginNotifier authNotifier = new LoginNotifier(account, authUseCases);
        compositeDisposable.add(authNotifier.getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    hideLoading();
                    if (result.isRight()) {
                        AuthEntity userEntity = result.getRight();
                        Log.d(TAG, "Uid: " + userEntity.getUid());
                        Log.d(TAG, "Email: " + userEntity.getEmail());
                        Log.d(TAG, "Display Name: " + userEntity.getDisplayName());
                        Log.d(TAG, "Photo Url: " + userEntity.getPhotoUrl());
                        credentialNotifiers.saveCredential(toCredentialEntity(userEntity));
                        Intent intent = new Intent(this, Navigator.class);
                        startActivity(intent);
                        finish();
                    }
                    if (result.isLeft()) {
                        Failure failure = result.getLeft();
                        showSnackBar(failure.getErrorMessage());
                    }
                }));
    }

    private CredentialEntity toCredentialEntity(AuthEntity userEntity) {
        return new CredentialEntity(
                userEntity.getUid(),
                userEntity.getEmail(),
                userEntity.getDisplayName(),
                userEntity.getPhotoUrl()
        );
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(ProgressBar.GONE);
    }

    private void showLoading() {
        loadingIndicator.setVisibility(ProgressBar.VISIBLE);
    }

    private void showSnackBar(String message) {
        SnackBarHelper.invoke(message, findViewById(R.id.main));
    }
}
