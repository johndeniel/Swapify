package barter.swapify.features.explore.data.repositories;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.constants.Constants;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.network.ConnectionChecker;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.explore.data.datasource.ExploreRemoteDataSource;
import barter.swapify.features.explore.domain.repository.ExploreRepository;
public class ExploreRepositoryImpl implements ExploreRepository {

    private final ConnectionChecker connectionChecker;
    private final ExploreRemoteDataSource exploreRemoteDataSource;

    public ExploreRepositoryImpl(ConnectionChecker connectionChecker, ExploreRemoteDataSource exploreRemoteDataSource) {
        this.connectionChecker = connectionChecker;
        this.exploreRemoteDataSource = exploreRemoteDataSource;
    }

    @Override
    public CompletableFuture<Either<Failure, List<String>>> banner() {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return exploreRemoteDataSource.banner().thenApply(either -> either.map(strings -> strings));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }
}
