package barter.swapify.core.credential.domain.usecases;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.credential.domain.repository.CredentialRepository;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.core.usecase.UseCase;

public class ClearCredentialUseCases implements UseCase<Boolean, NoParams> {

    private final CredentialRepository credentialRepository;

    public ClearCredentialUseCases(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, Boolean>> invoke(NoParams noParams) {
        return credentialRepository.logout();
    }
}
