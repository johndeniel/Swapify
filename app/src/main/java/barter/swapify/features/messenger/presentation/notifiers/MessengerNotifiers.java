package barter.swapify.features.messenger.presentation.notifiers;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import barter.swapify.features.messenger.domain.usecases.RecentUseCases;
import barter.swapify.features.post.domain.entity.PostEntity;
import io.reactivex.rxjava3.core.Single;

public class MessengerNotifiers {
    private final String uid;
    private final RecentUseCases recentUseCases;

    public MessengerNotifiers(String uid, RecentUseCases recentUseCases) {
        this.uid = uid;
        this.recentUseCases = recentUseCases;
    }


    public Single<Either<Failure, FirestoreRecyclerOptions<Chatroom>>> recent() {
        return Single.create(emitter -> recentUseCases.invoke(uid)
                .thenApplyAsync(result -> {
                    emitter.onSuccess(result);
                    return null;
                })
                .exceptionally(throwable -> {
                    emitter.onError(throwable);
                    return null;
                }));
    }
}
