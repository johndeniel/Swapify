package barter.swapify.features.auth.data.datasource;

import static barter.swapify.core.constants.Constants.USER_REFERENCE;
import static barter.swapify.features.auth.data.model.AuthModel.toAuthModel;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.typedef.User;
import barter.swapify.features.auth.data.model.AuthModel;

public class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
    private static final String TAG = AuthRemoteDataSourceImpl.class.getSimpleName();
    private final FirebaseAuth mAuth;
    private final FirebaseFirestore database;

    @Inject
    public AuthRemoteDataSourceImpl(FirebaseAuth mAuth, FirebaseFirestore database) {
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
                    future.complete(Either.right(toAuthModel(firebaseUser)));
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

    private void saveUserInfo(FirebaseUser firebaseUser) {
        try {
            String userId = firebaseUser.getUid();
            String userFullName = firebaseUser.getDisplayName();
            String userPhotoUrl = String.valueOf(firebaseUser.getPhotoUrl());

            Log.d(TAG, "User document set successfully. User ID: " + userId);
            Log.d(TAG, "User Full Name: " + userFullName);
            Log.d(TAG, "User Photo URL: " + userPhotoUrl);

            User user = new User(userId, userFullName, userPhotoUrl);

            DocumentReference docRef = database.collection(USER_REFERENCE).document(userId);
            docRef.set(user)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User document set successfully. User ID: " + userId))
                    .addOnFailureListener(e -> Log.e(TAG, "Error setting user document", e));
        } catch (Exception e) {
            Log.e(TAG, "Exception setting user info", e);
        }
    }
}
