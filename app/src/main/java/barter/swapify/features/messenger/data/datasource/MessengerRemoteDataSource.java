package barter.swapify.features.messenger.data.datasource;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.messenger.domain.entity.Chatroom;

public interface MessengerRemoteDataSource {
    CompletableFuture<Either<Failure, FirestoreRecyclerOptions<Chatroom>>> recent(String uid);
}
