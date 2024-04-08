package barter.swapify.core.credential.presentation.notifiers;

import android.content.Context;
import barter.swapify.core.credential.data.datasource.CredentialLocalDataSource;
import barter.swapify.core.credential.data.datasource.CredentialLocalDataSourceImpl;
import barter.swapify.core.credential.data.datasource.dao.CredentialDao;
import barter.swapify.core.credential.data.datasource.dao.CredentialDaoImpl;
import barter.swapify.core.credential.data.repositories.CredentialRepositoryImpl;
import barter.swapify.core.credential.domain.entity.CredentialEntity;
import barter.swapify.core.credential.domain.repository.CredentialRepository;
import barter.swapify.core.credential.domain.usecases.GetCredentialUseCases;
import barter.swapify.core.credential.domain.usecases.SaveCredentialUseCases;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import io.reactivex.rxjava3.core.Single;

public class CredentialNotifiers {

    private final GetCredentialUseCases getCredentialUseCases;
    private final SaveCredentialUseCases saveCredentialUseCases;

    public CredentialNotifiers(Context context) {
        CredentialDao credentialDao = new CredentialDaoImpl(context);
        CredentialLocalDataSource credentialLocalDataSource = new CredentialLocalDataSourceImpl(credentialDao);
        CredentialRepository credentialRepository = new CredentialRepositoryImpl(credentialLocalDataSource);

        getCredentialUseCases = new GetCredentialUseCases(credentialRepository);
        saveCredentialUseCases = new SaveCredentialUseCases(credentialRepository);
    }

    public Single<Either<Failure, CredentialEntity>> getCredential() {
        return Single.create(emitter -> getCredentialUseCases.invoke(new NoParams())
                .thenApplyAsync(result -> {
                    emitter.onSuccess(result);
                    return null;
                })
                .exceptionally(throwable -> {
                    emitter.onError(throwable);
                    return null;
                }));
    }

    public void saveCredential(CredentialEntity credentialEntity) {
        saveCredentialUseCases.invoke(credentialEntity);
    }
}
