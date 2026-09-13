package com.akin.wallet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "akin_wallet.db";
    private static final int DATABASE_VERSION = 3;

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

    private static final String TABLE_BANK_CARDS = "bank_cards";
    private static final String COL_CARD_ID = "id";
    private static final String COL_CARD_TYPE = "card_type";
    private static final String COL_CARD_NETWORK = "card_network";
    private static final String COL_BANK_NAME = "bank_name";
    private static final String COL_HOLDER_NAME = "holder_name";
    private static final String COL_CARD_NUMBER = "card_number";
    private static final String COL_EXPIRY = "expiry";
    private static final String COL_CVV = "cvv";
    private static final String COL_CARD_PIN = "pin";
    private static final String COL_DESIGN = "design";

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

        String createBankCards = "CREATE TABLE " + TABLE_BANK_CARDS + " ("
                + COL_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CARD_TYPE + " TEXT, "
                + COL_CARD_NETWORK + " TEXT, "
                + COL_BANK_NAME + " TEXT, "
                + COL_HOLDER_NAME + " TEXT, "
                + COL_CARD_NUMBER + " TEXT, "
                + COL_EXPIRY + " TEXT, "
                + COL_CVV + " TEXT, "
                + COL_CARD_PIN + " TEXT, "
                + COL_DESIGN + " INTEGER)";
        db.execSQL(createBankCards);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ASSOCIATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGINS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BANK_CARDS);
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

    public long insertBankCard(BankCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CARD_TYPE, item.getCardType());
        cv.put(COL_CARD_NETWORK, item.getCardNetwork());
        cv.put(COL_BANK_NAME, item.getBankName());
        cv.put(COL_HOLDER_NAME, item.getHolderName());
        cv.put(COL_CARD_NUMBER, item.getCardNumber());
        cv.put(COL_EXPIRY, item.getExpiry());
        cv.put(COL_CVV, item.getCvv());
        cv.put(COL_CARD_PIN, item.getPin());
        cv.put(COL_DESIGN, item.getDesign());
        long id = db.insert(TABLE_BANK_CARDS, null, cv);
        db.close();
        return id;
    }

    public List<BankCardItem> getAllBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new BankCardItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_CARD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CARD_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CARD_NETWORK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_BANK_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_HOLDER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CARD_NUMBER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPIRY)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CVV)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CARD_PIN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_DESIGN))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public int updateBankCard(BankCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CARD_TYPE, item.getCardType());
        cv.put(COL_CARD_NETWORK, item.getCardNetwork());
        cv.put(COL_BANK_NAME, item.getBankName());
        cv.put(COL_HOLDER_NAME, item.getHolderName());
        cv.put(COL_CARD_NUMBER, item.getCardNumber());
        cv.put(COL_EXPIRY, item.getExpiry());
        cv.put(COL_CVV, item.getCvv());
        cv.put(COL_CARD_PIN, item.getPin());
        cv.put(COL_DESIGN, item.getDesign());
        int rows = db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        return rows;
    }

    public int deleteBankCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_BANK_CARDS, COL_CARD_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
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
