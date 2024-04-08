package barter.swapify.features.auth.data.datasource;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import java.util.concurrent.CompletableFuture;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.auth.data.model.UserModel;

public interface AuthRemoteDataSource {
    CompletableFuture<Either<Failure, UserModel>> getUser(GoogleSignInAccount account);
}
