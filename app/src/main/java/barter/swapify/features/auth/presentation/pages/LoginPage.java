package barter.swapify.features.auth.presentation.pages;

import android.content.Intent;
import android.os.Bundle;
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
import barter.swapify.features.auth.domain.entity.UserEntity;
import barter.swapify.features.auth.domain.repository.AuthRepository;
import barter.swapify.features.auth.domain.usecases.GetUserUseCases;
import barter.swapify.features.auth.presentation.notifiers.AuthNotifier;
import barter.swapify.features.auth.presentation.widgets.GoogleSignInPopup;
import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LoginPage extends DaggerAppCompatActivity {
    @Inject
    public AuthRepository provideAuthRepository;
    @Inject
    public CompositeDisposable provideCompositeDisposable;
    private ProgressBar loadingIndicator;
    private GoogleSignInPopup googleSignInPopup;
    private CredentialNotifiers credentialNotifiers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_presentation_pages_login);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);


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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        GoogleSignInAccount account = googleSignInPopup.handleSignInResult(requestCode, data);

        showLoading();

        AuthNotifier authNotifier = new AuthNotifier(account, new GetUserUseCases(provideAuthRepository));
        provideCompositeDisposable.add(authNotifier.getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    hideLoading();
                    if (result.isRight()) {
                        UserEntity userEntity = result.getRight();
                        credentialNotifiers.saveCredential(mapFrom(userEntity));
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

    private CredentialEntity mapFrom(UserEntity userEntity) {
        return new CredentialEntity(
                userEntity.getEmail(),
                userEntity.getDisplayName()
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
