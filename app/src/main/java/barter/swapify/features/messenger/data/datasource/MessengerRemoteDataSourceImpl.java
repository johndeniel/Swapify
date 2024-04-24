package barter.swapify.features.messenger.data.datasource;

import static barter.swapify.core.constants.Constants.CHATROOM_REFERENCE;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.messenger.domain.entity.Chatroom;

public class MessengerRemoteDataSourceImpl implements MessengerRemoteDataSource {
    @Override
    public CompletableFuture<Either<Failure, FirestoreRecyclerOptions<Chatroom>>> recent(String uid) {
        CompletableFuture<Either<Failure, FirestoreRecyclerOptions<Chatroom>>> future = new CompletableFuture<>();

        try {
            Query query = FirebaseFirestore.getInstance().collection(CHATROOM_REFERENCE)
                    .whereArrayContains("userIds", uid)
                    .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);

            FirestoreRecyclerOptions<Chatroom> options = new FirestoreRecyclerOptions.Builder<Chatroom>()
                    .setQuery(query, Chatroom.class)
                    .build();
            future.complete(Either.right(options));
        } catch (Exception e) {
            Failure failure = new Failure("FirestoreRecyclerOptions creation failed" + e.getMessage());
            future.complete(Either.left(failure));
        }

        return future;
    }
}
