package barter.swapify.features.auth.data.repositories;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.util.concurrent.CompletableFuture;
import barter.swapify.core.constants.Constants;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.network.ConnectionChecker;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.auth.data.datasource.AuthRemoteDataSource;
import barter.swapify.features.auth.data.model.UserModel;
import barter.swapify.features.auth.domain.entity.UserEntity;
import barter.swapify.features.auth.domain.repository.AuthRepository;

public class AuthRepositoryImpl implements AuthRepository {

    private final ConnectionChecker connectionChecker;
    private final AuthRemoteDataSource authRemoteDataSource;

    public AuthRepositoryImpl(ConnectionChecker connectionChecker, AuthRemoteDataSource authRemoteDataSource) {
        this.connectionChecker = connectionChecker;
        this.authRemoteDataSource = authRemoteDataSource;
    }

    @Override
    public CompletableFuture<Either<Failure, UserEntity>> getUser(GoogleSignInAccount account) {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return authRemoteDataSource.getUser(account).thenApply(either -> either.map(UserModel::toUserEntity));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }
}
