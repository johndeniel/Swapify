package barter.swapify.features.auth.data.datasource;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.auth.data.model.UserModel;

public class AuthRemoteDataSourceImpl implements AuthRemoteDataSource {
    private final FirebaseAuth mAuth;

    @Inject
    public AuthRemoteDataSourceImpl (FirebaseAuth mAuth) {
        this.mAuth = mAuth;
    }
    @Override
    public CompletableFuture<Either<Failure, UserModel>> getUser(GoogleSignInAccount account) {
        CompletableFuture<Either<Failure, UserModel>> future = new CompletableFuture<>();
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            UserModel user = new UserModel(firebaseUser.getEmail(), firebaseUser.getDisplayName());
                            future.complete(Either.right(user));
                        } else {
                            Failure failure = new Failure("FirebaseUser is null");
                            future.complete(Either.left(failure));
                        }
                    } else {
                        Failure failure = new Failure("Error signing in with credential");
                        future.complete(Either.left(failure));
                    }
                });

        return future;
    }
}
