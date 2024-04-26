package barter.swapify.features.auth.presentation.notifiers;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.NoParams;
import barter.swapify.features.auth.domain.entity.AuthEntity;
import barter.swapify.features.auth.domain.usecases.AuthUseCases;
import barter.swapify.features.auth.domain.usecases.LogoutUseCases;
import io.reactivex.rxjava3.core.Single;

public class LoginNotifier {
    private final GoogleSignInAccount account;
    private final AuthUseCases getUserUseCases;

    public LoginNotifier(GoogleSignInAccount account, AuthUseCases getUserUseCases) {
        this.account = account;
        this.getUserUseCases = getUserUseCases;

    }

    public Single<Either<Failure, AuthEntity>> getUser() {
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