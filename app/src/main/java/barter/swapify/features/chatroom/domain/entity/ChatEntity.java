package barter.swapify.features.chatroom.domain.entity;

import com.google.firebase.Timestamp;

public class ChatEntity {
    private String message;
    private String senderId;
    private Timestamp timestamp;

    public ChatEntity() {
        // Required empty public constructor
    }

    public ChatEntity(final String message, final String senderId, final Timestamp timestamp) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getSenderId() {
        return senderId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
