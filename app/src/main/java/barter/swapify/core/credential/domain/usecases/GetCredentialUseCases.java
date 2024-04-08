package barter.swapify.core.credential.domain.usecases;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.domain.repository.CredentialRepository;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.core.usecase.UseCase;

public class GetCredentialUseCases implements UseCase<CredentialEntity, NoParams> {

    private final CredentialRepository credentialRepository;
    public GetCredentialUseCases(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }
    @Override
    public CompletableFuture<Either<Failure, CredentialEntity>> invoke(NoParams noParams) {
        return credentialRepository.getCredential();
    }
}
