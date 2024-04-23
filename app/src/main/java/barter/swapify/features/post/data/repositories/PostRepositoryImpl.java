package barter.swapify.features.post.data.repositories;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.constants.Constants;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.network.ConnectionChecker;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.post.data.datasource.PostRemoteDataSource;
import barter.swapify.features.post.data.model.PostModel;
import barter.swapify.features.post.domain.entity.PostEntity;
import barter.swapify.features.post.domain.repository.PostRepository;

public class PostRepositoryImpl implements PostRepository {
    private final ConnectionChecker connectionChecker;
    private final PostRemoteDataSource postRemoteDataSource;

    public PostRepositoryImpl(ConnectionChecker connectionChecker, PostRemoteDataSource postRemoteDataSource) {
        this.connectionChecker = connectionChecker;
        this.postRemoteDataSource = postRemoteDataSource;
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostEntity>>> fetch(String uid) {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return postRemoteDataSource.fetch(uid).thenApply(either -> either.map(PostModel::toPostEntity));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostEntity>>> heart() {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return postRemoteDataSource.heart().thenApply(either -> either.map(PostModel::toPostEntity));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }

    @Override
    public CompletableFuture<Either<Failure, List<PostEntity>>> tag() {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return postRemoteDataSource.tag().thenApply(either -> either.map(PostModel::toPostEntity));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }
}

