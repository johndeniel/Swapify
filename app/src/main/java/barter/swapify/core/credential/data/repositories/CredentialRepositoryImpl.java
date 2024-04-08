package barter.swapify.core.credential.data.repositories;

import static barter.swapify.core.credential.data.model.CredentialModel.mapToModel;
import java.util.concurrent.CompletableFuture;
import barter.swapify.core.credential.data.datasource.CredentialLocalDataSource;
import barter.swapify.core.credential.data.model.CredentialModel;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.domain.repository.CredentialRepository;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;


public class CredentialRepositoryImpl implements CredentialRepository {

    private final CredentialLocalDataSource credentialLocalDataSource;

    public CredentialRepositoryImpl(CredentialLocalDataSource credentialLocalDataSource) {
        this.credentialLocalDataSource = credentialLocalDataSource;
    }


    @Override
    public CompletableFuture<Either<Failure, CredentialEntity>> getCredential() {
        return credentialLocalDataSource.getCredential().thenApply(either -> either.map(CredentialModel::toCredentialEntity));
    }

    @Override
    public CompletableFuture<Either<Failure, Void>> saveCredential(CredentialEntity credentialEntity) {
        return credentialLocalDataSource.saveCredential(mapToModel(credentialEntity))
                .thenApply(either -> either.map(__ -> null));
    }
}
