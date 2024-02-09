package Scent.Danielle.Utils.DataModel;

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

    /**
     * Get the unique ID of the chatroom.
     * @return The chatroom ID.
     */
    public String getChatroomId() {
        return chatroomId;
    }

    /**
     * Set the unique ID of the chatroom.
     * @param chatroomId The chatroom ID to set.
     */
    public void setChatroomId(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    /**
     * Get the list of user IDs participating in the chatroom.
     * @return The list of user IDs.
     */
    public List<String> getUserIds() {
        return userIds;
    }

    /**
     * Set the list of user IDs participating in the chatroom.
     * @param userIds The list of user IDs to set.
     */
    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    /**
     * Get the timestamp of the last message sent in the chatroom.
     * @return The timestamp of the last message.
     */
    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    /**
     * Set the timestamp of the last message sent in the chatroom.
     * @param lastMessageTimestamp The timestamp of the last message to set.
     */
    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    /**
     * Get the ID of the user who sent the last message in the chatroom.
     * @return The ID of the user who sent the last message.
     */
    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    /**
     * Set the ID of the user who sent the last message in the chatroom.
     * @param lastMessageSenderId The ID of the user who sent the last message to set.
     */
    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    /**
     * Get the content of the last message sent in the chatroom.
     * @return The content of the last message.
     */
    public String getLastMessage() {
        return lastMessage;
    }

    /**
     * Set the content of the last message sent in the chatroom.
     * @param lastMessage The content of the last message to set.
     */
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}