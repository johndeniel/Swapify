package barter.swapify.features.auth.domain.repository;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import java.util.concurrent.CompletableFuture;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.auth.domain.entity.UserEntity;

public interface AuthRepository {
    CompletableFuture<Either<Failure, UserEntity>> getUser(GoogleSignInAccount account);
}
