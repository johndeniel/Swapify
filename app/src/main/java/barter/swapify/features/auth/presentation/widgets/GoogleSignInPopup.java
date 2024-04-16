package barter.swapify.features.auth.presentation.widgets;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import barter.swapify.features.auth.presentation.pages.LoginPage;
import barter.swapify.R;

public class GoogleSignInPopup {
    private static final String TAG = GoogleSignInPopup.class.getSimpleName();
    public static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;
    private final LoginPage loginPage;

    public GoogleSignInPopup(LoginPage loginPage) {
        this.loginPage = loginPage;
        initializeGoogleSignIn();
    }

    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(loginPage.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(loginPage, gso);
        Log.d(TAG, "Google Sign-In initialized successfully.");
    }

    /** @noinspection deprecation*/
    public void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        loginPage.startActivityForResult(signInIntent, RC_SIGN_IN);
        Log.d(TAG, "Starting Google Sign-In activity.");
    }

    public GoogleSignInAccount handleSignInResult(int requestCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                Log.d(TAG, "Google Sign-In successful.");
                return task.getResult(ApiException.class);
            } catch (ApiException e) {
                Log.e(TAG, "Google Sign-In failed with exception: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Unexpected requestCode received: " + requestCode);
        }
        return null;
    }
}
