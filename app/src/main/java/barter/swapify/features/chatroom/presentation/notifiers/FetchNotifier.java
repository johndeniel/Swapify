package barter.swapify.features.chatroom.presentation.notifiers;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.chatroom.domain.entity.ChatEntity;
import barter.swapify.features.chatroom.domain.usecases.FetchUseCases;
import io.reactivex.rxjava3.core.Single;

public class FetchNotifier {
    private final List<Object> obj;
    private final FetchUseCases fetchUseCases;

    public FetchNotifier(List<Object> obj, FetchUseCases fetchUseCases) {
        this.obj = obj;
        this.fetchUseCases = fetchUseCases;
    }


    public Single<Either<Failure, FirestoreRecyclerOptions<ChatEntity>>> fetch() {
        return Single.create(emitter -> fetchUseCases.invoke(obj)
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
