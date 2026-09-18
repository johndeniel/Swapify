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
    // v7 adds audit timestamps to id_cards (created_at/updated_at), mirroring
    // bank_cards (v5) and logins (v6), so Government IDs sort newest-first.
    // v8 adds the mobile column to logins (contact number beside PIN).
    // v9 adds soft-delete (deleted_at) to logins/bank_cards/id_cards so
    // deletes move to Trash (Settings) instead of vanishing. 0 = active.
    private static final int DATABASE_VERSION = 9;

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
    // Epoch millis (INTEGER). Added in v5; older rows backfilled on upgrade.
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_UPDATED_AT = "updated_at";
    // Soft-delete stamp (INTEGER epoch millis). 0/NULL = active, >0 = in Trash.
    // Added in v9; deletes set this, Trash lists/restores/clears it.
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
        db.execSQL(getCreateIdCardsSql());
    }

    private static String getCreateIdCardsSql() {
        // Audit columns live beside the row identity (id), never inside
        // fields_json — the JSON blob carries only per-type document data.
        // deleted_at (v9) powers Trash; 0 means the ID is active.
        return "CREATE TABLE IF NOT EXISTS " + TABLE_ID_CARDS + " ("
                + COL_ID_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ID_TYPE + " TEXT, "
                + COL_ID_FIELDS_JSON + " TEXT, "
                + COL_ID_DESIGN + " INTEGER, "
                + COL_CREATED_AT + " INTEGER DEFAULT 0, "
                + COL_UPDATED_AT + " INTEGER DEFAULT 0, "
                + COL_DELETED_AT + " INTEGER DEFAULT 0)";
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

        // v6: same timestamp columns on logins (social accounts), newest-first.
        if (oldVersion < 6) {
            addColumnIfMissing(db, TABLE_LOGINS, COL_CREATED_AT);
            addColumnIfMissing(db, TABLE_LOGINS, COL_UPDATED_AT);
            long now = System.currentTimeMillis();
            db.execSQL("UPDATE " + TABLE_LOGINS
                    + " SET " + COL_CREATED_AT + "=? WHERE " + COL_CREATED_AT + " IS NULL OR "
                    + COL_CREATED_AT + "=0", new Object[]{now});
            db.execSQL("UPDATE " + TABLE_LOGINS
                    + " SET " + COL_UPDATED_AT + "=? WHERE " + COL_UPDATED_AT + " IS NULL OR "
                    + COL_UPDATED_AT + "=0", new Object[]{now});
        }

        // v7: audit timestamps on id_cards. ALTER is idempotent via
        // addColumnIfMissing; backfill stamps old rows so sort/display works.
        if (oldVersion < 7) {
            addColumnIfMissing(db, TABLE_ID_CARDS, COL_CREATED_AT);
            addColumnIfMissing(db, TABLE_ID_CARDS, COL_UPDATED_AT);
            long now = System.currentTimeMillis();
            db.execSQL("UPDATE " + TABLE_ID_CARDS
                    + " SET " + COL_CREATED_AT + "=? WHERE " + COL_CREATED_AT + " IS NULL OR "
                    + COL_CREATED_AT + "=0", new Object[]{now});
            db.execSQL("UPDATE " + TABLE_ID_CARDS
                    + " SET " + COL_UPDATED_AT + "=? WHERE " + COL_UPDATED_AT + " IS NULL OR "
                    + COL_UPDATED_AT + "=0", new Object[]{now});
        }

        // v8: mobile column on logins (contact number beside PIN).
        if (oldVersion < 8) {
            addTextColumnIfMissing(db, TABLE_LOGINS, COL_MOBILE);
        }

        // v9: soft-delete stamp on all three vault tables. Active rows stay 0;
        // deletes set epoch millis so Trash can list newest-deleted first.
        if (oldVersion < 9) {
            addColumnIfMissing(db, TABLE_LOGINS, COL_DELETED_AT);
            addColumnIfMissing(db, TABLE_BANK_CARDS, COL_DELETED_AT);
            addColumnIfMissing(db, TABLE_ID_CARDS, COL_DELETED_AT);
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

    /** TEXT variant of the idempotent ADD COLUMN above (defaults to ''). */
    private static void addTextColumnIfMissing(SQLiteDatabase db, String table, String column) {
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
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " TEXT DEFAULT ''");
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
        // Newest-first by recency; id breaks ties (equal stamps, legacy rows).
        Cursor cursor = queryActiveOrFallback(db, TABLE_LOGINS,
                COL_UPDATED_AT + " DESC, " + COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            // New columns may be absent on a DB that hasn't run the v6 upgrade
            // in a test harness; fall back to 0 instead of crashing.
            int createdIdx = cursor.getColumnIndex(COL_CREATED_AT);
            int updatedIdx = cursor.getColumnIndex(COL_UPDATED_AT);
            int mobileIdx = cursor.getColumnIndex(COL_MOBILE);
            do {
                list.add(new CredentialItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PLATFORM)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PIN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ICON_RES)),
                        mobileIdx != -1 ? cursor.getString(mobileIdx) : "",
                        createdIdx != -1 ? cursor.getLong(createdIdx) : 0,
                        updatedIdx != -1 ? cursor.getLong(updatedIdx) : 0
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * Permanent delete: removes the row plus every association touching it.
     * Used by Trash "delete forever" / "empty trash" and by the social edit
     * save (delete+reinsert). User-facing deletes must call
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
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_LOGINS
                    + " WHERE " + COL_DELETED_AT + " IS NOT NULL AND "
                    + COL_DELETED_AT + " != 0"
                    + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID + " DESC", null);
        } catch (Exception e) {
            // Pre-v9 DB without the column: nothing can be trashed yet.
            db.close();
            return list;
        }
        if (cursor.moveToFirst()) {
            int createdIdx = cursor.getColumnIndex(COL_CREATED_AT);
            int updatedIdx = cursor.getColumnIndex(COL_UPDATED_AT);
            int mobileIdx = cursor.getColumnIndex(COL_MOBILE);
            do {
                list.add(new CredentialItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PLATFORM)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PIN)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ICON_RES)),
                        mobileIdx != -1 ? cursor.getString(mobileIdx) : "",
                        createdIdx != -1 ? cursor.getLong(createdIdx) : 0,
                        updatedIdx != -1 ? cursor.getLong(updatedIdx) : 0
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
        // Newest-first by recency; id breaks ties (equal stamps, legacy rows).
        Cursor cursor = queryActiveOrFallback(db, TABLE_BANK_CARDS,
                COL_UPDATED_AT + " DESC, " + COL_CARD_ID + " DESC");
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
        Cursor cursor;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                    + " WHERE " + COL_DELETED_AT + " IS NOT NULL AND "
                    + COL_DELETED_AT + " != 0"
                    + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_CARD_ID + " DESC", null);
        } catch (Exception e) {
            db.close();
            return list;
        }
        if (cursor.moveToFirst()) {
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
        // ties (equal stamps, legacy rows).
        Cursor cursor = queryActiveOrFallback(db, TABLE_ID_CARDS,
                COL_UPDATED_AT + " DESC, " + COL_ID_CARD_ID + " DESC");
        if (cursor.moveToFirst()) {
            // New columns may be absent on a DB that hasn't run the v7 upgrade
            // in a test harness; fall back to 0 instead of crashing.
            int createdIdx = cursor.getColumnIndex(COL_CREATED_AT);
            int updatedIdx = cursor.getColumnIndex(COL_UPDATED_AT);
            do {
                list.add(new IdCardItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_CARD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_TYPE)),
                        IdCardItem.parseFieldsJson(
                                cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_FIELDS_JSON))),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_DESIGN)),
                        createdIdx != -1 ? cursor.getLong(createdIdx) : 0,
                        updatedIdx != -1 ? cursor.getLong(updatedIdx) : 0
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
        Cursor cursor;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                    + " WHERE " + COL_DELETED_AT + " IS NOT NULL AND "
                    + COL_DELETED_AT + " != 0"
                    + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID_CARD_ID + " DESC", null);
        } catch (Exception e) {
            db.close();
            return list;
        }
        if (cursor.moveToFirst()) {
            int createdIdx = cursor.getColumnIndex(COL_CREATED_AT);
            int updatedIdx = cursor.getColumnIndex(COL_UPDATED_AT);
            do {
                list.add(new IdCardItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_CARD_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_TYPE)),
                        IdCardItem.parseFieldsJson(
                                cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_FIELDS_JSON))),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_DESIGN)),
                        createdIdx != -1 ? cursor.getLong(createdIdx) : 0,
                        updatedIdx != -1 ? cursor.getLong(updatedIdx) : 0
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // ------------------------------------------------------------------
    // Trash helpers
    // ------------------------------------------------------------------

    /**
     * Active-rows query with pre-v9 fallback: if deleted_at is missing (old
     * test DB that skipped the v9 upgrade), return every row instead of
     * crashing — nothing can be trashed there yet.
     */
    private Cursor queryActiveOrFallback(SQLiteDatabase db, String table, String orderBy) {
        try {
            return db.rawQuery("SELECT * FROM " + table
                    + " WHERE " + COL_DELETED_AT + " IS NULL OR " + COL_DELETED_AT + "=0"
                    + " ORDER BY " + orderBy, null);
        } catch (Exception e) {
            return db.rawQuery("SELECT * FROM " + table + " ORDER BY " + orderBy, null);
        }
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
