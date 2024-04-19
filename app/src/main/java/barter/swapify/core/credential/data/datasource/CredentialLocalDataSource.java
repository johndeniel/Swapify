package barter.swapify.core.credential.data.datasource;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.credential.data.model.CredentialModel;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;

public interface CredentialLocalDataSource {
    CompletableFuture<Either<Failure, CredentialModel>> getCredential();
    CompletableFuture<Either<Failure, Void>> saveCredential(CredentialModel credentialModel);

    CompletableFuture<Either<Failure, Boolean>> logout();
}
