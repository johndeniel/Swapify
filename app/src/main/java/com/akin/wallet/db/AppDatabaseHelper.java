package com.akin.wallet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "akin_wallet.db";
    private static final int DATABASE_VERSION = 5;

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
    // Epoch millis (INTEGER). Added in v5; older rows backfilled on upgrade.
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_UPDATED_AT = "updated_at";

    private static final String TABLE_ID_CARDS = "id_cards";
    private static final String COL_ID_CARD_ID = "id";
    private static final String COL_ID_TYPE = "id_type";
    private static final String COL_ID_FIELDS_JSON = "fields_json";
    private static final String COL_ID_DESIGN = "design";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOGINS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PLATFORM + " TEXT, "
                + COL_USERNAME + " TEXT, "
                + COL_PASSWORD + " TEXT, "
                + COL_PIN + " TEXT, "
                + COL_ICON_RES + " INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSOCIATIONS + " ("
                + COL_LOGIN_ID + " INTEGER, "
                + COL_ASSOCIATED_ID + " INTEGER, "
                + "PRIMARY KEY (" + COL_LOGIN_ID + ", " + COL_ASSOCIATED_ID + "))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BANK_CARDS + " ("
                + COL_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CARD_TYPE + " TEXT, "
                + COL_CARD_NETWORK + " TEXT, "
                + COL_BANK_NAME + " TEXT, "
                + COL_HOLDER_NAME + " TEXT, "
                + COL_CARD_NUMBER + " TEXT, "
                + COL_EXPIRY + " TEXT, "
                + COL_CVV + " TEXT, "
                + COL_CARD_PIN + " TEXT, "
                + COL_DESIGN + " INTEGER, "
                + COL_CREATED_AT + " INTEGER DEFAULT 0, "
                + COL_UPDATED_AT + " INTEGER DEFAULT 0)");
        db.execSQL(getCreateIdCardsSql());
    }

    private static String getCreateIdCardsSql() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_ID_CARDS + " ("
                + COL_ID_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ID_TYPE + " TEXT, "
                + COL_ID_FIELDS_JSON + " TEXT, "
                + COL_ID_DESIGN + " INTEGER)";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Non-destructive: never drop user data. Create missing tables only.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOGINS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PLATFORM + " TEXT, "
                + COL_USERNAME + " TEXT, "
                + COL_PASSWORD + " TEXT, "
                + COL_PIN + " TEXT, "
                + COL_ICON_RES + " INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ASSOCIATIONS + " ("
                + COL_LOGIN_ID + " INTEGER, "
                + COL_ASSOCIATED_ID + " INTEGER, "
                + "PRIMARY KEY (" + COL_LOGIN_ID + ", " + COL_ASSOCIATED_ID + "))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_BANK_CARDS + " ("
                + COL_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CARD_TYPE + " TEXT, "
                + COL_CARD_NETWORK + " TEXT, "
                + COL_BANK_NAME + " TEXT, "
                + COL_HOLDER_NAME + " TEXT, "
                + COL_CARD_NUMBER + " TEXT, "
                + COL_EXPIRY + " TEXT, "
                + COL_CVV + " TEXT, "
                + COL_CARD_PIN + " TEXT, "
                + COL_DESIGN + " INTEGER, "
                + COL_CREATED_AT + " INTEGER DEFAULT 0, "
                + COL_UPDATED_AT + " INTEGER DEFAULT 0)");
        db.execSQL(getCreateIdCardsSql());

        // v5: timestamp columns on bank_cards. CREATE TABLE IF NOT EXISTS is a
        // no-op for existing installs, so ALTER explicitly and backfill.
        if (oldVersion < 5) {
            addColumnIfMissing(db, TABLE_BANK_CARDS, COL_CREATED_AT);
            addColumnIfMissing(db, TABLE_BANK_CARDS, COL_UPDATED_AT);
            // Pre-migration rows carry 0; stamp them so sort/display works.
            long now = System.currentTimeMillis();
            db.execSQL("UPDATE " + TABLE_BANK_CARDS
                    + " SET " + COL_CREATED_AT + "=? WHERE " + COL_CREATED_AT + " IS NULL OR "
                    + COL_CREATED_AT + "=0", new Object[]{now});
            db.execSQL("UPDATE " + TABLE_BANK_CARDS
                    + " SET " + COL_UPDATED_AT + "=? WHERE " + COL_UPDATED_AT + " IS NULL OR "
                    + COL_UPDATED_AT + "=0", new Object[]{now});
        }
    }

    /** Idempotent ADD COLUMN: SQLite throws if the column exists, so probe first. */
    private static void addColumnIfMissing(SQLiteDatabase db, String table, String column) {
        boolean missing = true;
        try (android.database.Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIdx = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) {
                if (column.equals(c.getString(nameIdx))) {
                    missing = false;
                    break;
                }
            }
        }
        if (missing) {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " INTEGER DEFAULT 0");
        }
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
        // Functional timestamps: a fresh row is both created and updated now.
        // Honour caller-supplied values (e.g. imports) when present.
        long now = System.currentTimeMillis();
        cv.put(COL_CREATED_AT, item.getCreatedAt() > 0 ? item.getCreatedAt() : now);
        cv.put(COL_UPDATED_AT, item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
        long id = db.insert(TABLE_BANK_CARDS, null, cv);
        db.close();
        return id;
    }

    public List<BankCardItem> getAllBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS, null);
        if (cursor.moveToFirst()) {
            // New columns may be absent on a DB that hasn't run the v5 upgrade
            // in a test harness; fall back to 0 instead of crashing.
            int createdIdx = cursor.getColumnIndex(COL_CREATED_AT);
            int updatedIdx = cursor.getColumnIndex(COL_UPDATED_AT);
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
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_DESIGN)),
                        createdIdx != -1 ? cursor.getLong(createdIdx) : 0,
                        updatedIdx != -1 ? cursor.getLong(updatedIdx) : 0
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
        // created_at is immutable: never overwritten, so creation order is kept.
        // Every edit bumps updated_at; honour an explicit value if the caller set one.
        cv.put(COL_UPDATED_AT,
                item.getUpdatedAt() > 0 ? item.getUpdatedAt() : System.currentTimeMillis());
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

    public long insertIdCard(IdCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ID_TYPE, item.getIdType());
        cv.put(COL_ID_FIELDS_JSON, item.getFieldsJson());
        cv.put(COL_ID_DESIGN, item.getDesign());
        long id = db.insert(TABLE_ID_CARDS, null, cv);
        db.close();
        return id;
    }

    public List<IdCardItem> getAllIdCards() {
        List<IdCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS, null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new IdCardItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_CARD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_TYPE)),
                        IdCardItem.parseFieldsJson(
                                cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_FIELDS_JSON))),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_DESIGN))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public int updateIdCard(IdCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ID_TYPE, item.getIdType());
        cv.put(COL_ID_FIELDS_JSON, item.getFieldsJson());
        cv.put(COL_ID_DESIGN, item.getDesign());
        int rows = db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        return rows;
    }

    public int deleteIdCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_ID_CARDS, COL_ID_CARD_ID + "=?", new String[]{String.valueOf(id)});
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
