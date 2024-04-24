package barter.swapify.features.messenger.data.repositories;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.constants.Constants;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.network.ConnectionChecker;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.messenger.data.datasource.MessengerRemoteDataSource;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import barter.swapify.features.messenger.domain.repository.MessengerRepository;

public class MessengerRepositoryImpl implements MessengerRepository {
    private final ConnectionChecker connectionChecker;
    private final MessengerRemoteDataSource messengerRemoteDataSource;

    public MessengerRepositoryImpl(ConnectionChecker connectionChecker, MessengerRemoteDataSource messengerRemoteDataSource) {
        this.connectionChecker = connectionChecker;
        this.messengerRemoteDataSource = messengerRemoteDataSource;
    }

    @Override
    public CompletableFuture<Either<Failure, FirestoreRecyclerOptions<Chatroom>>> recent(String uid) {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return messengerRemoteDataSource.recent(uid).thenApply(either -> either.map(a -> a));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }
}
