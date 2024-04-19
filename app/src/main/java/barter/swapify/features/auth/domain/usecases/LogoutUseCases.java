package barter.swapify.features.auth.domain.usecases;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.auth.domain.repository.AuthRepository;

public class LogoutUseCases implements UseCase<Boolean, NoParams> {

    private final AuthRepository authRepository;

    public LogoutUseCases(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, Boolean>> invoke(NoParams noParams) {
        return authRepository.logout();
    }
}
