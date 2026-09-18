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
    // v10 is the consolidated schema: logins/bank_cards/id_cards each carry
    // created_at + updated_at (newest-first ordering), logins carries mobile,
    // and all three carry deleted_at (0 = active, epoch millis = in Trash).
    // Every older install upgrades to exactly this schema in one step below;
    // no per-version legacy paths are kept.
    private static final int DATABASE_VERSION = 10;

    private static final String TABLE_LOGINS = "logins";
    private static final String COL_ID = "id";
    private static final String COL_PLATFORM = "platform";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_PIN = "pin";
    private static final String COL_ICON_RES = "icon_res";
    private static final String COL_MOBILE = "mobile";

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
    // Epoch millis (INTEGER): recency ordering, newest-first.
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_UPDATED_AT = "updated_at";
    // Soft-delete stamp (INTEGER epoch millis). 0/NULL = active, >0 = in Trash.
    private static final String COL_DELETED_AT = "deleted_at";

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
        createAllTables(db);
    }

    /** Full v10 schema. Shared by onCreate and onUpgrade so both land identical. */
    private static void createAllTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOGINS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PLATFORM + " TEXT, "
                + COL_USERNAME + " TEXT, "
                + COL_PASSWORD + " TEXT, "
                + COL_PIN + " TEXT, "
                + COL_ICON_RES + " INTEGER, "
                + COL_MOBILE + " TEXT DEFAULT '', "
                + COL_CREATED_AT + " INTEGER DEFAULT 0, "
                + COL_UPDATED_AT + " INTEGER DEFAULT 0, "
                + COL_DELETED_AT + " INTEGER DEFAULT 0)");
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
                + COL_UPDATED_AT + " INTEGER DEFAULT 0, "
                + COL_DELETED_AT + " INTEGER DEFAULT 0)");
        // Audit columns live beside the row identity (id), never inside
        // fields_json — the JSON blob carries only per-type document data.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ID_CARDS + " ("
                + COL_ID_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ID_TYPE + " TEXT, "
                + COL_ID_FIELDS_JSON + " TEXT, "
                + COL_ID_DESIGN + " INTEGER, "
                + COL_CREATED_AT + " INTEGER DEFAULT 0, "
                + COL_UPDATED_AT + " INTEGER DEFAULT 0, "
                + COL_DELETED_AT + " INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Single consolidated migration, non-destructive (never drops data):
        // bring every older install to the exact v10 schema. Missing tables
        // are created; any missing column is added in place. No per-version
        // branches, no backfills, no legacy fallbacks anywhere else.
        createAllTables(db);
        if (oldVersion < 10) {
            ensureColumn(db, TABLE_LOGINS, COL_CREATED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_LOGINS, COL_UPDATED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_LOGINS, COL_MOBILE, "TEXT DEFAULT ''");
            ensureColumn(db, TABLE_LOGINS, COL_DELETED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_BANK_CARDS, COL_CREATED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_BANK_CARDS, COL_UPDATED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_BANK_CARDS, COL_DELETED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_ID_CARDS, COL_CREATED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_ID_CARDS, COL_UPDATED_AT, "INTEGER DEFAULT 0");
            ensureColumn(db, TABLE_ID_CARDS, COL_DELETED_AT, "INTEGER DEFAULT 0");
        }
    }

    /** Idempotent ADD COLUMN: probes PRAGMA first since SQLite throws if it exists. */
    private static void ensureColumn(SQLiteDatabase db, String table, String column,
                                     String definition) {
        try (android.database.Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIdx = c.getColumnIndexOrThrow("name");
            while (c.moveToNext()) {
                if (column.equals(c.getString(nameIdx))) {
                    return;
                }
            }
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    public long insertLogin(CredentialItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PLATFORM, item.getPlatform());
        cv.put(COL_USERNAME, item.getUsername());
        cv.put(COL_PASSWORD, item.getPassword());
        cv.put(COL_PIN, item.getPin());
        cv.put(COL_ICON_RES, item.getIconRes());
        cv.put(COL_MOBILE, item.getMobile() != null ? item.getMobile() : "");
        // Functional timestamps: a fresh row is both created and updated now.
        // Honour caller-supplied values (edit reinsert) when present.
        long now = System.currentTimeMillis();
        cv.put(COL_CREATED_AT, item.getCreatedAt() > 0 ? item.getCreatedAt() : now);
        cv.put(COL_UPDATED_AT, item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
        // Fresh rows are always active (not in Trash).
        cv.put(COL_DELETED_AT, 0);
        long id = db.insert(TABLE_LOGINS, null, cv);
        db.close();
        return id;
    }

    public List<CredentialItem> getAllLogins() {
        List<CredentialItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Active rows only: trashed rows live in Trash (Settings).
        // Newest-first by recency; id breaks ties on equal stamps.
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LOGINS
                + " WHERE " + COL_DELETED_AT + " IS NULL OR " + COL_DELETED_AT + "=0"
                + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new CredentialItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PLATFORM)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PIN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ICON_RES)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_MOBILE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * Permanent delete: removes the row plus every association touching it.
     * Used by Trash "delete forever" and by the social edit save
     * (delete+reinsert). User-facing deletes must call
     * {@link #moveLoginToTrash(int)} instead so the item lands in Trash.
     */
    public int deleteLogin(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ASSOCIATIONS, COL_LOGIN_ID + "=? OR " + COL_ASSOCIATED_ID + "=?",
                new String[]{String.valueOf(id), String.valueOf(id)});
        int rows = db.delete(TABLE_LOGINS, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /**
     * Soft-delete: stamps deleted_at so the account disappears from the
     * dashboard and appears in Settings > Trash. Associations are kept so a
     * restore brings links back; permanent delete cleans them up.
     */
    public int moveLoginToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DELETED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_LOGINS, cv, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /** Restores a trashed account back to the dashboard (deleted_at = 0). */
    public int restoreLogin(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DELETED_AT, 0);
        int rows = db.update(TABLE_LOGINS, cv, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /** Trashed social accounts, newest-deleted first. */
    public List<CredentialItem> getTrashedLogins() {
        List<CredentialItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LOGINS
                + " WHERE " + COL_DELETED_AT + " IS NOT NULL AND "
                + COL_DELETED_AT + " != 0"
                + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new CredentialItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PLATFORM)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PIN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ICON_RES)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_MOBILE)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * Marks an account as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called when the
     * account is opened for editing; the list re-sorts on the next refresh.
     */
    public int touchLoginUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_UPDATED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_LOGINS, cv, COL_ID + "=?",
                new String[]{String.valueOf(id)});
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
        cv.put(COL_DELETED_AT, 0);
        long id = db.insert(TABLE_BANK_CARDS, null, cv);
        db.close();
        return id;
    }

    public List<BankCardItem> getAllBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Active rows only; trashed cards live in Trash.
        // Newest-first by recency; id breaks ties on equal stamps.
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                + " WHERE " + COL_DELETED_AT + " IS NULL OR " + COL_DELETED_AT + "=0"
                + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_CARD_ID + " DESC", null);
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
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_DESIGN)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
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

    /**
     * Soft-delete: moves the card to Trash (Settings). The dashboard hides it
     * until restored or permanently deleted.
     */
    public int moveBankCardToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DELETED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /** Restores a trashed card back to the dashboard. */
    public int restoreBankCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DELETED_AT, 0);
        int rows = db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /** Trashed bank cards, newest-deleted first. */
    public List<BankCardItem> getTrashedBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                + " WHERE " + COL_DELETED_AT + " IS NOT NULL AND "
                + COL_DELETED_AT + " != 0"
                + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_CARD_ID + " DESC", null);
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
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_DESIGN)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * Marks a card as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called on
     * dashboard tap; the list re-sorts on the next refresh.
     */
    public int touchBankCardUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_UPDATED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public long insertIdCard(IdCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ID_TYPE, item.getIdType());
        cv.put(COL_ID_FIELDS_JSON, item.getFieldsJson());
        cv.put(COL_ID_DESIGN, item.getDesign());
        // Functional timestamps: a fresh row is both created and updated now.
        // Honour caller-supplied values (e.g. imports) when present. The JSON
        // blob is untouched — stamps live in their own columns.
        long now = System.currentTimeMillis();
        cv.put(COL_CREATED_AT, item.getCreatedAt() > 0 ? item.getCreatedAt() : now);
        cv.put(COL_UPDATED_AT, item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
        cv.put(COL_DELETED_AT, 0);
        long id = db.insert(TABLE_ID_CARDS, null, cv);
        db.close();
        return id;
    }

    public List<IdCardItem> getAllIdCards() {
        List<IdCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Active rows only; trashed IDs live in Trash.
        // Newest-first by recency, matching bank cards and logins; id breaks
        // ties on equal stamps.
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                + " WHERE " + COL_DELETED_AT + " IS NULL OR " + COL_DELETED_AT + "=0"
                + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_ID_CARD_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new IdCardItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_CARD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_TYPE)),
                        IdCardItem.parseFieldsJson(
                                cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_FIELDS_JSON))),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_DESIGN)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
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
        // created_at is immutable: never overwritten, so creation order is kept.
        // Every edit bumps updated_at; honour an explicit value if the caller set one.
        cv.put(COL_UPDATED_AT,
                item.getUpdatedAt() > 0 ? item.getUpdatedAt() : System.currentTimeMillis());
        int rows = db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        return rows;
    }

    /**
     * Marks an ID as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called on
     * dashboard tap, mirroring the bank-card and login touch helpers; the
     * list re-sorts on the next refresh.
     */
    public int touchIdCardUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_UPDATED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public int deleteIdCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_ID_CARDS, COL_ID_CARD_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /**
     * Soft-delete: moves the ID to Trash (Settings). The dashboard hides it
     * until restored or permanently deleted.
     */
    public int moveIdCardToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DELETED_AT, System.currentTimeMillis());
        int rows = db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /** Restores a trashed ID back to the dashboard. */
    public int restoreIdCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_DELETED_AT, 0);
        int rows = db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /** Trashed government IDs, newest-deleted first. */
    public List<IdCardItem> getTrashedIdCards() {
        List<IdCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                + " WHERE " + COL_DELETED_AT + " IS NOT NULL AND "
                + COL_DELETED_AT + " != 0"
                + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID_CARD_ID + " DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new IdCardItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_CARD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_TYPE)),
                        IdCardItem.parseFieldsJson(
                                cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_FIELDS_JSON))),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_DESIGN)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                        cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /** Total items currently in Trash (all three vault tables). */
    public int getTrashCount() {
        return getTrashedLogins().size()
                + getTrashedBankCards().size()
                + getTrashedIdCards().size();
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
