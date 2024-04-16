    package barter.swapify.features.swipe.data.repositories;

    import java.util.List;
    import java.util.concurrent.CompletableFuture;

    import barter.swapify.core.constants.Constants;
    import barter.swapify.core.errors.Failure;
    import barter.swapify.core.network.ConnectionChecker;
    import barter.swapify.core.typedef.Either;
    import barter.swapify.features.swipe.data.datasource.SwipeRemoteDataSource;
    import barter.swapify.features.swipe.data.model.SwipeModel;
    import barter.swapify.features.swipe.domain.entity.SwipeEntity;
    import barter.swapify.features.swipe.domain.repository.SwipeRepository;

    public class SwipeRepositoryImpl implements SwipeRepository {
        private final ConnectionChecker connectionChecker;
        private final SwipeRemoteDataSource swipeRemoteDataSource;

        public SwipeRepositoryImpl(ConnectionChecker connectionChecker, SwipeRemoteDataSource swipeRemoteDataSource) {
            this.connectionChecker = connectionChecker;
            this.swipeRemoteDataSource = swipeRemoteDataSource;
        }

        @Override
        public CompletableFuture<Either<Failure, List<SwipeEntity>>> fetch(String uid) {
            return connectionChecker.isConnected()
                    .thenCompose(isConnected -> {
                        if (isConnected) {
                            return swipeRemoteDataSource.fetch(uid).thenApply(either -> either.map(SwipeModel::toSwipeEntity));
                        } else {
                            return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                        }
                    });
        }
    }
