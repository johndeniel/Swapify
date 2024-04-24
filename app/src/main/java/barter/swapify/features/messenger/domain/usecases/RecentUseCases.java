package barter.swapify.features.messenger.domain.usecases;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.messenger.domain.entity.Chatroom;
import barter.swapify.features.messenger.domain.repository.MessengerRepository;

public class RecentUseCases implements UseCase<FirestoreRecyclerOptions<Chatroom>, String> {
    private final MessengerRepository messengerRepository;

    public RecentUseCases(MessengerRepository messengerRepository) {
        this.messengerRepository = messengerRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, FirestoreRecyclerOptions<Chatroom>>> invoke(String s) {
        return messengerRepository.recent(s);
    }
}
