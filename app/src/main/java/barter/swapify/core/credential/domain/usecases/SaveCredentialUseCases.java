package barter.swapify.core.credential.domain.usecases;

import java.util.concurrent.CompletableFuture;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.domain.repository.CredentialRepository;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.core.usecase.UseCase;

public class SaveCredentialUseCases implements UseCase<Void, CredentialEntity> {
    private final CredentialRepository credentialRepository;
    public SaveCredentialUseCases(CredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }
    @Override
    public CompletableFuture<Either<Failure, Void>> invoke(CredentialEntity credentialEntity) {
        return credentialRepository.saveCredential(credentialEntity);
    }
}
