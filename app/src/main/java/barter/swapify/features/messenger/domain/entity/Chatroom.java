package barter.swapify.features.messenger.domain.entity;

import com.google.firebase.Timestamp;
import java.util.List;

public class Chatroom {
    String chatroomId; // Unique ID of the chatroom
    List<String> userIds; // IDs of the users participating in the chatroom
    Timestamp lastMessageTimestamp; // Timestamp of the last message sent in the chatroom
    String lastMessageSenderId; // ID of the user who sent the last message
    String lastMessage; // Content of the last message

    /**
     * Default constructor required for Firebase.
     */
    public Chatroom() {
        // Required empty public constructor
    }

    /**
     * Constructor to initialize a Chatroom object.
     * @param chatroomId The unique ID of the chatroom.
     * @param userIds The list of user IDs participating in the chatroom.
     * @param lastMessageTimestamp The timestamp of the last message sent in the chatroom.
     * @param lastMessageSenderId The ID of the user who sent the last message.
     */
    public Chatroom(String chatroomId, List<String> userIds, Timestamp lastMessageTimestamp, String lastMessageSenderId) {
        this.chatroomId = chatroomId;
        this.userIds = userIds;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getChatroomId() {
        return chatroomId;
    }

    public void setChatroomId(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}