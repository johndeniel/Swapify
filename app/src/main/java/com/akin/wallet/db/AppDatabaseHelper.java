package com.akin.wallet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.akin.wallet.model.CredentialItem;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "akin_wallet.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_LOGINS = "logins";
    private static final String COL_ID = "id";
    private static final String COL_PLATFORM = "platform";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_PIN = "pin";
    private static final String COL_ICON_RES = "icon_res";

    private static final String TABLE_ASSOCIATIONS = "associations";
    private static final String COL_LOGIN_ID = "login_id";
    private static final String COL_ASSOCIATED_ID = "associated_login_id";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createLogins = "CREATE TABLE " + TABLE_LOGINS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PLATFORM + " TEXT, "
                + COL_USERNAME + " TEXT, "
                + COL_PASSWORD + " TEXT, "
                + COL_PIN + " TEXT, "
                + COL_ICON_RES + " INTEGER)";
        db.execSQL(createLogins);

        String createAssociations = "CREATE TABLE " + TABLE_ASSOCIATIONS + " ("
                + COL_LOGIN_ID + " INTEGER, "
                + COL_ASSOCIATED_ID + " INTEGER, "
                + "PRIMARY KEY (" + COL_LOGIN_ID + ", " + COL_ASSOCIATED_ID + "))";
        db.execSQL(createAssociations);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ASSOCIATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGINS);
        onCreate(db);
    }

    public long insertLogin(CredentialItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PLATFORM, item.getPlatform());
        cv.put(COL_USERNAME, item.getUsername());
        cv.put(COL_PASSWORD, item.getPassword());
        cv.put(COL_PIN, item.getPin());
        cv.put(COL_ICON_RES, item.getIconRes());
        long id = db.insert(TABLE_LOGINS, null, cv);
        db.close();
        return id;
    }

    public List<CredentialItem> getAllLogins() {
        List<CredentialItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LOGINS, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new CredentialItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PLATFORM)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PIN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ICON_RES))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public int deleteLogin(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ASSOCIATIONS, COL_LOGIN_ID + "=? OR " + COL_ASSOCIATED_ID + "=?",
                new String[]{String.valueOf(id), String.valueOf(id)});
        int rows = db.delete(TABLE_LOGINS, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public void insertAssociation(long loginId, long associatedId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LOGIN_ID, loginId);
        cv.put(COL_ASSOCIATED_ID, associatedId);
        db.insert(TABLE_ASSOCIATIONS, null, cv);
        db.close();
    }

    public List<Integer> getAssociations(long loginId) {
        List<Integer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_ASSOCIATED_ID + " FROM " + TABLE_ASSOCIATIONS
                + " WHERE " + COL_LOGIN_ID + "=?", new String[]{String.valueOf(loginId)});
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getInt(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}
