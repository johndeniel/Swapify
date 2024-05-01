package barter.swapify.features.chatroom.data.repositories;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.constants.Constants;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.network.ConnectionChecker;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.chatroom.data.datasource.ChatRemoteDataSource;
import barter.swapify.features.chatroom.domain.entity.ChatEntity;
import barter.swapify.features.chatroom.domain.repository.ChatRepository;

public class ChatRepositoryImpl implements ChatRepository {
    private final ConnectionChecker connectionChecker;
    private final ChatRemoteDataSource chatRemoteDataSource;

    public ChatRepositoryImpl(ConnectionChecker connectionChecker, ChatRemoteDataSource chatRemoteDataSource) {
        this.connectionChecker = connectionChecker;
        this.chatRemoteDataSource = chatRemoteDataSource;
    }

    @Override
    public CompletableFuture<Either<Failure, Boolean>> send(List<Object> obj) {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return chatRemoteDataSource.send(obj).thenApply(either -> either.map(aBoolean -> aBoolean));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }

    @Override
    public CompletableFuture<Either<Failure, FirestoreRecyclerOptions<ChatEntity>>> fetch(List<Object> obj) {
        return connectionChecker.isConnected()
                .thenCompose(isConnected -> {
                    if (isConnected) {
                        return chatRemoteDataSource.fetch(obj).thenApply(either -> either.map(result -> result));
                    } else {
                        return CompletableFuture.completedFuture(Either.left(new Failure(Constants.noConnectionErrorMessage)));
                    }
                });
    }
}
