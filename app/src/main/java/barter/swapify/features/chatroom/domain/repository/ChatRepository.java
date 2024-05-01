package barter.swapify.features.chatroom.domain.repository;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.chatroom.domain.entity.ChatEntity;


public interface ChatRepository {
    CompletableFuture<Either<Failure, Boolean>> send(List<Object> obj);
    CompletableFuture<Either<Failure, FirestoreRecyclerOptions<ChatEntity>>> fetch(List<Object> obj);
}
