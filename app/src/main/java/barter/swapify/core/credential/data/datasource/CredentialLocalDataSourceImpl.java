package barter.swapify.core.credential.data.datasource;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.credential.data.datasource.dao.CredentialDao;
import barter.swapify.core.credential.data.model.CredentialModel;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;

public class CredentialLocalDataSourceImpl implements CredentialLocalDataSource{
    private final CredentialDao credentialDao;

    public CredentialLocalDataSourceImpl(CredentialDao credentialDao) {
        this.credentialDao = credentialDao;
    }

    @Override
    public CompletableFuture<Either<Failure, CredentialModel>> getCredential() {
        CompletableFuture<Either<Failure, CredentialModel>> future = new CompletableFuture<>();
        try {
            CredentialModel credentials = credentialDao.getCredential();
            future.complete(Either.right(credentials));
        } catch (Exception e) {
            future.complete(Either.left(new Failure(e.getMessage())));
        }

        return future;
    }

    @Override
    public CompletableFuture<Either<Failure, Void>> saveCredential(CredentialModel credentialModel) {
        CompletableFuture<Either<Failure, Void>> future = new CompletableFuture<>();
        try {
            credentialDao.saveCredential(credentialModel);
            future.complete(Either.right(null));
        } catch (Exception e) {
            future.complete(Either.left(new Failure(e.getMessage())));
        }
        return future;
    }

    @Override
    public CompletableFuture<Either<Failure, Boolean>> logout() {
        CompletableFuture<Either<Failure, Boolean>> future = new CompletableFuture<>();
        try {
            credentialDao.logOut();
            future.complete(Either.right(true));
        } catch (Exception e) {
            future.complete(Either.left(new Failure(e.getMessage())));
        }
        return future;
    }
}
