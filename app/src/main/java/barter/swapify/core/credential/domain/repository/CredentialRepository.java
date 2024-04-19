package barter.swapify.core.credential.domain.repository;

import java.util.concurrent.CompletableFuture;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;

public interface CredentialRepository {
    CompletableFuture<Either<Failure, CredentialEntity>> getCredential();
    CompletableFuture<Either<Failure, Void>> saveCredential(CredentialEntity credentialEntity);
    CompletableFuture<Either<Failure, Boolean>> logout();
}
