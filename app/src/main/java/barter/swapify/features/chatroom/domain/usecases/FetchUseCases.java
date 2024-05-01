package barter.swapify.features.chatroom.domain.usecases;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.chatroom.domain.entity.ChatEntity;
import barter.swapify.features.chatroom.domain.repository.ChatRepository;

public class FetchUseCases implements UseCase<FirestoreRecyclerOptions<ChatEntity>, List<Object>> {

    private final ChatRepository chatRepository;

    public FetchUseCases(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, FirestoreRecyclerOptions<ChatEntity>>> invoke(List<Object> obj) {
        return chatRepository.fetch(obj);
    }
}
