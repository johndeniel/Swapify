package barter.swapify.features.auth.domain.usecases;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.UseCase;
import java.util.concurrent.CompletableFuture;
import barter.swapify.features.auth.domain.entity.AuthEntity;
import barter.swapify.features.auth.domain.repository.AuthRepository;

public class AuthUseCases implements UseCase<AuthEntity, GoogleSignInAccount> {
    final AuthRepository authRepository;

    public AuthUseCases(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, AuthEntity>> invoke(GoogleSignInAccount account) {
        return authRepository.authentication(account);
    }
}
