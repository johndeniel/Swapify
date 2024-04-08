package barter.swapify.features.auth.presentation.notifiers;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.auth.domain.entity.UserEntity;
import barter.swapify.features.auth.domain.usecases.GetUserUseCases;
import io.reactivex.rxjava3.core.Single;

public class AuthNotifier {
    private final GoogleSignInAccount account;
    private final GetUserUseCases getUserUseCases;

    public AuthNotifier(GoogleSignInAccount account, GetUserUseCases getUserUseCases) {
        this.account = account;
        this.getUserUseCases = getUserUseCases;

    }

    public Single<Either<Failure, UserEntity>> getUser() {
        return Single.create(emitter -> getUserUseCases.invoke(account)
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
