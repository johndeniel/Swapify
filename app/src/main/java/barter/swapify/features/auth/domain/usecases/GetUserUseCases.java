package barter.swapify.features.auth.domain.usecases;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.UseCase;
import java.util.concurrent.CompletableFuture;
import barter.swapify.features.auth.domain.entity.UserEntity;
import barter.swapify.features.auth.domain.repository.AuthRepository;

public class GetUserUseCases implements UseCase<UserEntity, GoogleSignInAccount> {
    final AuthRepository authRepository;

    public GetUserUseCases(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, UserEntity>> invoke(GoogleSignInAccount account) {
        return authRepository.getUser(account);
    }
}
