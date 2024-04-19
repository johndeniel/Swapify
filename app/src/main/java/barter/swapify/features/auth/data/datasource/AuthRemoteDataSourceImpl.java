package barter.swapify.features.auth.data.datasource;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import barter.swapify.R;
import barter.swapify.core.constants.Constants;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.typedef.User;
import barter.swapify.features.auth.data.model.AuthModel;

public class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
    private static final String TAG = AuthRemoteDataSourceImpl.class.getSimpleName();
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore database;
    private final Context context;
    private GoogleSignInClient mGoogleSignInClient;

    @Inject
    public AuthRemoteDataSourceImpl(Context context, FirebaseAuth mAuth, FirebaseFirestore database) {
        this.context = context;
        this.mAuth = mAuth;
        this.database = database;
    }

    @Override
    public CompletableFuture<Either<Failure, AuthModel>> authentication(GoogleSignInAccount account) {
        CompletableFuture<Either<Failure, AuthModel>> future = new CompletableFuture<>();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser != null) {
                    saveUserInfo(firebaseUser);
                    future.complete(Either.right(AuthModel.toAuthModel(firebaseUser)));
                    Log.d(TAG, "User signed in successfully. User ID: " + firebaseUser.getUid());
                } else {
                    future.complete(Either.left(new Failure("FirebaseUser is null")));
                    Log.e(TAG, "FirebaseUser is null");
                }
            } else {
                Failure failure = new Failure("Error signing in with credential");
                future.complete(Either.left(failure));
                Log.e(TAG, "Error signing in with credential", task.getException());
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Either<Failure, Boolean>> logout() {
        CompletableFuture<Either<Failure, Boolean>> future = new CompletableFuture<>();
        try {
            initializeGoogleSignInClient();
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
            if (account != null) {
                mGoogleSignInClient.signOut()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                mAuth.signOut();
                                future.complete(Either.right(true));
                            } else {
                                Failure failure = new Failure("Error signing out");
                                future.complete(Either.left(failure));
                            }
                        });
            } else {
                Failure failure = new Failure("No signed-in Google account found");
                future.complete(Either.left(failure));
            }
        } catch (Exception e) {
            Failure failure = new Failure("Error logging out");
            future.complete(Either.left(failure));
        }
        return future;
    }

    private void initializeGoogleSignInClient() {
        if (mGoogleSignInClient == null) {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        }
    }

    private void saveUserInfo(@NonNull FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        String userFullName = firebaseUser.getDisplayName();
        String userPhotoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        Log.d(TAG, "User document set successfully. User ID: " + userId);
        Log.d(TAG, "User Full Name: " + userFullName);
        Log.d(TAG, "User Photo URL: " + userPhotoUrl);

        User user = new User(userId, userFullName, userPhotoUrl);

        DocumentReference docRef = database.collection(Constants.USER_REFERENCE).document(userId);
        docRef.set(user)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User document set successfully. User ID: " + userId))
                .addOnFailureListener(e -> Log.e(TAG, "Error setting user document", e));
    }
}
