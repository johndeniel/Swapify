package barter.swapify.features.chatroom.domain.entity;
import android.annotation.SuppressLint;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.List;

public class FirebaseInitialization {

    // Firebase references
    private static final String USER_REFERENCE = "users";
    private static final String ITEMS_REFERENCE = "items";
    private static final String PHOTO_REFERENCE = "uploads";
    private static final String CHATROOM_REFERENCE = "chatroom";

    // Firebase User Information
    private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    private static final String userId = currentUser.getUid();
    private static final String currentUserDisplayName = currentUser.getDisplayName();
    private static final String currentUserDisplayPhotoUrl = currentUser.getPhotoUrl().toString();

    // Firebase Database And Storage
    @SuppressLint("StaticFieldLeak")
    private static final FirebaseFirestore store = FirebaseFirestore.getInstance();
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();

    /**
     * Get the current Firebase user.
     * @return The current Firebase user.
     */
    public static FirebaseUser getCurrentUser() {
        return currentUser;
    }

    /**
     * Get the ID of the current Firebase user.
     * @return The ID of the current Firebase user.
     */
    public static String getCurrentUserId() {
        return userId;
    }

    /**
     * Get the display name of the current Firebase user.
     * @return The display name of the current Firebase user.
     */
    public static String getCurrentUserDisplayName() { return currentUserDisplayName; }

    /**
     * Get the display photo URL of the current Firebase user.
     * @return The display photo URL of the current Firebase user.
     */
    public static String getCurrentUserDisplayPhotoUrl() { return currentUserDisplayPhotoUrl; }

    /**
     * Get the document reference for the current user in FireStore.
     * @return The document reference for the current user.
     */
    public static DocumentReference getUserDocumentReference(){
        return store.collection(USER_REFERENCE).document(userId);
    }

    /**
     * Get the document reference for a chatroom in FireStore.
     * @param chatroomId The ID of the chatroom.
     * @return The document reference for the chatroom.
     */
    public static DocumentReference getChatroomReference(String chatroomId){
        return store.collection(CHATROOM_REFERENCE).document(chatroomId);
    }

    /**
     * Get the collection reference for messages in a chatroom in FireStore.
     * @param chatroomId The ID of the chatroom.
     * @return The collection reference for messages in the chatroom.
     */
    public static CollectionReference getChatroomMessageReference(String chatroomId){
        return getChatroomReference(chatroomId).collection(CHATROOM_REFERENCE);
    }

    /**
     * Get the collection reference for all users in FireStore.
     * @return The collection reference for all users.
     */
    public static CollectionReference allUserCollectionReference(){
        return FirebaseFirestore.getInstance().collection("users");
    }

    /**
     * Get the collection reference for all ChatRooms in FireStore.
     * @return The collection reference for all ChatRooms.
     */
    public static CollectionReference allChatroomCollectionReference(){
        return FirebaseFirestore.getInstance().collection(CHATROOM_REFERENCE);
    }

    /**
     * Get the document reference for another user in a chatroom.
     * @param userIds The IDs of the users in the chatroom.
     * @return The document reference for the other user.
     */
    public static DocumentReference getOtherUserFromChatroom(List<String> userIds){
        if(userIds.get(0).equals(userId)){
            return allUserCollectionReference().document(userIds.get(1));
        }else{
            return allUserCollectionReference().document(userIds.get(0));
        }
    }

    /**
     * Get the database reference for items in FirebaseDatabase.
     * @return The database reference for items.
     */
    public static DatabaseReference getItemsDatabaseReference() {
        return database.getReference(ITEMS_REFERENCE);
    }

    /**
     * Get the storage reference for photos in FirebaseStorage.
     * @return The storage reference for photos.
     */
    public static StorageReference getPhotoStorageReferences() {
        return storage.getReference().child(PHOTO_REFERENCE).child(userId);
    }
}