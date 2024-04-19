package barter.swapify.core.constants;

public class Constants {
   // Firebase
   public static final String USER_REFERENCE = "users";
   public static final String ITEMS_REFERENCE = "items";
   public static final String PHOTO_REFERENCE = "uploads";
   public static final String CHATROOM_REFERENCE = "chatroom";

   // Sqlite
   public static final int DATABASE_VERSION = 1;
   public static final String DATABASE_NAME = "credential.db";
   public static final String TABLE_CREDENTIAL = "credential";
   public static final String COLUMN_UID = "uid";
   public static final String COLUMN_EMAIL = "email";
   public static final String COLUMN_DISPLAY_NAME = "display_name";
   public static final String COLUMN_DISPLAY_PHOTO_URL = "photo_url";


   public static final String noConnectionErrorMessage = "wifi disconnected!";
   public static final String errorLoggingOutMessage = "Error logging out";
}
