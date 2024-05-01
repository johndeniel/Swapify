package barter.swapify.features.chatroom.presentation.notifiers;

import java.util.List;
import java.util.Map;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.chatroom.domain.entity.ChatEntity;
import barter.swapify.features.chatroom.domain.usecases.SendUseCases;
import io.reactivex.rxjava3.core.Single;

public class ChatNotifier {
    private final List<Object> obj;
    private final SendUseCases sendUseCases;

    public ChatNotifier(List<Object> obj, SendUseCases sendUseCases) {
        this.obj = obj;
        this.sendUseCases = sendUseCases;
    }

    public Single<Either<Failure, Boolean>> send() {
        return Single.create(emitter -> sendUseCases.invoke(obj)
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
