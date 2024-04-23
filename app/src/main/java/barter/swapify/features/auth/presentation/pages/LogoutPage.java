package barter.swapify.features.auth.presentation.pages;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.credential.presentation.notifiers.CredentialNotifiers;
import barter.swapify.core.errors.Failure;

import barter.swapify.core.typedef.Either;
import barter.swapify.features.auth.domain.repository.AuthRepository;
import barter.swapify.features.auth.domain.usecases.LogoutUseCases;
import barter.swapify.features.auth.presentation.notifiers.LogoutNotifier;
import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LogoutPage extends DaggerAppCompatActivity {
    private static final String TAG = LogoutPage.class.getSimpleName();
    @Inject
    public AuthRepository authRepository;
    private CompositeDisposable compositeDisposable;
    private CompositeDisposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_presentation_logout_page);
        hideNavigationBar();

        disposable = new CompositeDisposable();
        compositeDisposable = new CompositeDisposable();
        clearCredential();
    }

    private void clearCredential() {
        CredentialNotifiers credentialNotifiers = new CredentialNotifiers(this);
        disposable.add(credentialNotifiers.logout()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleCredentialResult));
    }

    private void logout() {
        LogoutUseCases logoutUseCases = new LogoutUseCases(authRepository);
        LogoutNotifier logoutNotifier = new LogoutNotifier(logoutUseCases);

        compositeDisposable.add(logoutNotifier.logout()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result.isRight()) {
                        new Handler().postDelayed(() -> {
                            navigateToLoginPage();
                            finish();
                        }, 2000);
                    } else {
                        Log.e(TAG, "Logout failed: " + result.getLeft().getErrorMessage());
                    }
                }));
    }

    private void handleCredentialResult(Either<Failure, Boolean> result) {
        if (result.isRight()) {
            logout();
        } else {
            Log.e(TAG, "Clearing credential failed: " + result.getLeft().getErrorMessage());
        }
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void navigateToLoginPage() {
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
    }
}
