package barter.swapify.features.chatroom.domain.usecases;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.core.usecase.UseCase;
import barter.swapify.features.chatroom.domain.repository.ChatRepository;

public class SendUseCases implements UseCase<Boolean, List<Object>> {

    private final ChatRepository chatRepository;

    public SendUseCases(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public CompletableFuture<Either<Failure, Boolean>> invoke(List<Object> obj) {
        return chatRepository.send(obj);
    }
}
