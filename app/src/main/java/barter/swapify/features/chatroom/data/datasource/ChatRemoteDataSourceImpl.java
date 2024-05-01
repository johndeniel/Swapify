package barter.swapify.features.chatroom.data.datasource;

import static barter.swapify.core.constants.Constants.CHATROOM_REFERENCE;

import android.util.Log;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import barter.swapify.core.errors.Failure;
import barter.swapify.core.typedef.Either;
import barter.swapify.features.chatroom.domain.entity.ChatEntity;
import barter.swapify.features.messenger.domain.entity.Chatroom;

public class ChatRemoteDataSourceImpl implements ChatRemoteDataSource{
    private static final String TAG = ChatRemoteDataSourceImpl.class.getSimpleName();
    private Chatroom chatroom;

    @Override
    public CompletableFuture<Either<Failure, Boolean>> send(List<Object> obj) {
        CompletableFuture<Either<Failure, Boolean>> future = new CompletableFuture<>();

        // Extract message and user IDs from the input list
        String message = obj.get(0).toString();
        String currentUserUuid =  obj.get(1).toString();
        String recipientUuid =  obj.get(2).toString();

        // Generate a chatroom ID based on user IDs
        String room = getChatroomId(currentUserUuid, recipientUuid);

        // Get the Fire Store document reference for the chatroom
        DocumentReference docRef = FirebaseFirestore.getInstance().collection(CHATROOM_REFERENCE).document(room);
        CollectionReference collRef = docRef.collection(CHATROOM_REFERENCE);

        // Check if the chatroom exists
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult() != null) {
                    chatroom = task.getResult().toObject(Chatroom.class);
                }
                if (chatroom == null) {
                    chatroom = new Chatroom(room, Arrays.asList(currentUserUuid, recipientUuid), Timestamp.now(), "");
                    docRef.set(chatroom)
                            .addOnSuccessListener(aVoid -> Log.i(TAG, "New chatroom created successfully: " ))
                            .addOnFailureListener(e -> Log.e(TAG, "Error creating new chatroom", e));
                }
            }
        });


        // Create a chat entity and update the chatroom with the new message
        ChatEntity chatEntity = new ChatEntity(message, currentUserUuid, Timestamp.now());
        chatroom.setLastMessage(message);
        chatroom.setLastMessageSenderId(currentUserUuid);
        chatroom.setLastMessageTimestamp(Timestamp.now());

        // Update Fire Store with the new chatroom and message
        docRef.set(chatroom)
                .addOnSuccessListener(aVoid -> collRef.add(chatEntity)
                        .addOnSuccessListener(aVoid1 -> future.complete(Either.right(true)))
                        .addOnFailureListener(e -> future.complete(Either.left(new Failure("Failed to add message to sub-collection: " + e.getMessage())))))
                .addOnFailureListener(e -> future.complete(Either.left(new Failure("Failed to set message to Fire Store: " + e.getMessage()))));

        return future;
    }

    @Override
    public CompletableFuture<Either<Failure, FirestoreRecyclerOptions<ChatEntity>>> fetch(List<Object> obj) {
        CompletableFuture<Either<Failure, FirestoreRecyclerOptions<ChatEntity>>> future = new CompletableFuture<>();

        try {
            // Extract user IDs from the input list
            String currentUserUuid =  obj.get(0).toString();
            String recipientUuid =  obj.get(1).toString();

            // Generate a chatroom ID based on user IDs
            String room = getChatroomId(currentUserUuid, recipientUuid);

            // Get the Fire Store document reference for the chatroom
            DocumentReference docRef = FirebaseFirestore.getInstance().collection(CHATROOM_REFERENCE).document(room);
            CollectionReference collRef = docRef.collection(CHATROOM_REFERENCE);

            // Construct the Fire Store query for fetching chat messages
            Query query = collRef.orderBy("timestamp", Query.Direction.DESCENDING);

            // Build FireStoreRecyclerOptions for the query
            FirestoreRecyclerOptions<ChatEntity> options = new FirestoreRecyclerOptions.Builder<ChatEntity>()
                    .setQuery(query, ChatEntity.class)
                    .build();

            future.complete(Either.right(options));

        } catch (Exception e) {
            future.complete(Either.left(new Failure("Failed to fetch chat data: " + e.getMessage())));
        }

        return future;
    }


    private String getChatroomId(final String currentUserUuid, final String recipientUuid) {
        return (currentUserUuid.hashCode() < recipientUuid.hashCode()) ? currentUserUuid + "_" + recipientUuid : recipientUuid + "_" + currentUserUuid;
    }
}
