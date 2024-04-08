package barter.swapify.core.credential.data.datasource.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import barter.swapify.core.credential.data.model.CredentialModel;

public class CredentialDaoImpl extends SQLiteOpenHelper implements CredentialDao {
    private static final String TAG = CredentialDaoImpl.class.getSimpleName();
    private static final String DATABASE_NAME = "trya.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CREDENTIAL = "credential";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_DISPLAY_NAME = "display_name";

    public CredentialDaoImpl(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + TABLE_CREDENTIAL + " (" +
                COLUMN_EMAIL + " TEXT NOT NULL, " +
                COLUMN_DISPLAY_NAME + " TEXT NOT NULL)";

        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    @Override
    public CredentialModel getCredential() {
        CredentialModel credential = null;
        try (SQLiteDatabase db = getReadableDatabase(); Cursor cursor = db.query(TABLE_CREDENTIAL,
                new String[]{COLUMN_EMAIL, COLUMN_DISPLAY_NAME},
                null, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int emailIndex = cursor.getColumnIndex(COLUMN_EMAIL);
                int displayNameIndex = cursor.getColumnIndex(COLUMN_DISPLAY_NAME);

                if (emailIndex >= 0 && displayNameIndex >= 0) {
                    String email = cursor.getString(emailIndex);
                    String displayName = cursor.getString(displayNameIndex);
                    credential = new CredentialModel(email, displayName);
                    Log.i(TAG, "Credential retrieved successfully: " + credential.getEmail());
                }
            } else {
                Log.w(TAG, "No credential found in the database");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving credential: " + e.getMessage());
        }

        return credential;
    }


    @Override
    public void saveCredential(CredentialModel credentialModel) {
        try (SQLiteDatabase db = getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_EMAIL, credentialModel.getEmail());
            values.put(COLUMN_DISPLAY_NAME, credentialModel.getDisplayName());

            long newRowId = db.insert(TABLE_CREDENTIAL, null, values);
            if (newRowId == -1) {
                Log.e(TAG, "Error inserting credential");
            } else {
                Log.i(TAG, "Success inserting credential: ");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting credential: " + e.getMessage());
        }
    }

    @Override
    public boolean logOut() {
        try (SQLiteDatabase db = getWritableDatabase()) {
            db.delete(TABLE_CREDENTIAL, null, null);
            Log.i(TAG, "Logged out successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error logging out: " + e.getMessage());
            return false;
        }
    }
}
