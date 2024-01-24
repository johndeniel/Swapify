package Scent.Danielle.Utils.Database;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseInitialization {
    private static final String USER_REFERENCE = "users";
    private static final String CHATROOM_REFERENCE = "chatroom";
    private static final String ITEMS_REFERENCE = "items";
    private static final String GALLERY_REFERENCE = "gallery";
    private static final String PHOTO_REFERENCE = "uploads";


    // Firebase User Information
    private static final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    private static final FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    private static final String userId = currentUser.getUid();


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


    // FirebaseFireStore
    public static DocumentReference getUserDocumentReference(){
        return store.collection(USER_REFERENCE).document(userId);
    }
    public static DocumentReference getChatroomReference(String chatroomId){
        return store.collection(CHATROOM_REFERENCE).document(chatroomId);
    }


    // FirebaseDatabase
    public static DatabaseReference getItemsDatabaseReference() {
        return database.getReference(ITEMS_REFERENCE);
    }
    public static DatabaseReference getGalleryDatabaseReference() {
        return database.getReference(GALLERY_REFERENCE).child(userId);
    }


    // FirebaseStorage
    public static StorageReference getPhotoStorageReferences() {
        return storage.getReference().child(PHOTO_REFERENCE).child(userId);
    }
}