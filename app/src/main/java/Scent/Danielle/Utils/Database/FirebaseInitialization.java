package Scent.Danielle.Utils.Database;

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
    private static final String USER_REFERENCE = "users";
    private static final String CHATROOM_REFERENCE = "chatroom";
    private static final String ITEMS_REFERENCE = "items";
    private static final String PHOTO_REFERENCE = "uploads";


    // Firebase User Information
    private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    private static final String userId = currentUser.getUid();
    private static final String currentUserDisplayName = currentUser.getDisplayName();
    private static final String currentUserDisplayPhotoUrl = currentUser.getPhotoUrl().toString();


    // Firebase Database And Storage
    private static final FirebaseFirestore store = FirebaseFirestore.getInstance();
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();


    // Authentication
    public static FirebaseUser getCurrentUser() {
        return currentUser;
    }
    public static String getCurrentUserId() {
        return userId;
    }
    public static String getCurrentUserDisplayName() { return currentUserDisplayName; };
    public static String getCurrentUserDisplayPhotoUrl() { return currentUserDisplayPhotoUrl; };


    // FirebaseFireStore
    public static DocumentReference getUserDocumentReference(){
        return store.collection(USER_REFERENCE).document(userId);
    }
    public static DocumentReference getChatroomReference(String chatroomId){
        return store.collection(CHATROOM_REFERENCE).document(chatroomId);
    }
    public static CollectionReference getChatroomMessageReference(String chatroomId){
        return getChatroomReference(chatroomId).collection(CHATROOM_REFERENCE);
    }

    public static CollectionReference allUserCollectionReference(){
        return FirebaseFirestore.getInstance().collection("users");
    }
    public static CollectionReference allChatroomCollectionReference(){
        return FirebaseFirestore.getInstance().collection(CHATROOM_REFERENCE);
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds){
        if(userIds.get(0).equals(userId)){
            return allUserCollectionReference().document(userIds.get(1));
        }else{
            return allUserCollectionReference().document(userIds.get(0));
        }
    }


    // FirebaseDatabase
    public static DatabaseReference getItemsDatabaseReference() {
        return database.getReference(ITEMS_REFERENCE);
    }


    // FirebaseStorage
    public static StorageReference getPhotoStorageReferences() {
        return storage.getReference().child(PHOTO_REFERENCE).child(userId);
    }
}