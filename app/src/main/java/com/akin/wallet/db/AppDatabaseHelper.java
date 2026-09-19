package com.akin.wallet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.akin.wallet.model.BankCardItem;
import com.akin.wallet.model.CredentialItem;
import com.akin.wallet.model.IdCardItem;
import com.akin.wallet.security.DbKeyManager;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "akin_wallet.db";
    // v1 schema: SQLCipher-encrypted vault (AES-256) with a Keystore-wrapped
    // random key, audit timestamp columns on every table, and indexes for
    // the dashboard/trash ordering plus the account-links join.
    // Encrypted-only: plaintext databases are not opened or converted, and
    // no older schema version is supported (development stage).
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_SOCIAL_ACCOUNTS = "social_accounts";
    private static final String COL_ID = "id";
    private static final String COL_PLATFORM = "platform";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COL_PIN = "pin";
    private static final String COL_ICON_RES = "icon_res";
    private static final String COL_MOBILE = "mobile";

    private static final String TABLE_ACCOUNT_LINKS = "account_links";
    private static final String COL_ACCOUNT_ID = "account_id";
    private static final String COL_LINKED_ACCOUNT_ID = "linked_account_id";

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

    private final Context appContext;

    public AppDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
        SQLiteDatabase.loadLibs(appContext);
    }

    // All opens flow through the vault key. SQLCipher's helper only offers
    // password-taking getters, so these same-named no-arg wrappers are new
    // methods (not overrides) that keep every call site working.
    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase(DbKeyManager.getPassphrase(appContext));
    }

    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase(DbKeyManager.getPassphrase(appContext));
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
        createIndexes(db);
    }

    /** Full v1 schema. Shared by onCreate and onUpgrade so both land identical. */
    private static void createAllTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SOCIAL_ACCOUNTS + " ("
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
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ACCOUNT_LINKS + " ("
                + COL_ACCOUNT_ID + " INTEGER, "
                + COL_LINKED_ACCOUNT_ID + " INTEGER, "
                + "PRIMARY KEY (" + COL_ACCOUNT_ID + ", " + COL_LINKED_ACCOUNT_ID + "))");
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
        // fields_json-- — the JSON blob carries only per-type document data.
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ID_CARDS + " ("
                + COL_ID_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ID_TYPE + " TEXT, "
                + COL_ID_FIELDS_JSON + " TEXT, "
                + COL_ID_DESIGN + " INTEGER, "
                + COL_CREATED_AT + " INTEGER DEFAULT 0, "
                + COL_UPDATED_AT + " INTEGER DEFAULT 0, "
                + COL_DELETED_AT + " INTEGER DEFAULT 0)");
    }

    /** v1: indexes for the dashboard/trash ordering and account-links joins. */
    private static void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_social_accounts_active ON " + TABLE_SOCIAL_ACCOUNTS
                + " (" + COL_DELETED_AT + ", " + COL_UPDATED_AT + " DESC, " + COL_ID + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bank_cards_active ON " + TABLE_BANK_CARDS
                + " (" + COL_DELETED_AT + ", " + COL_UPDATED_AT + " DESC, " + COL_CARD_ID + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_id_cards_active ON " + TABLE_ID_CARDS
                + " (" + COL_DELETED_AT + ", " + COL_UPDATED_AT + " DESC, " + COL_ID_CARD_ID + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_account_links_account ON " + TABLE_ACCOUNT_LINKS
                + " (" + COL_ACCOUNT_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_account_links_linked ON " + TABLE_ACCOUNT_LINKS
                + " (" + COL_LINKED_ACCOUNT_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 only: no per-version migrations. Any older database is brought
        // to the exact current schema (missing tables created, indexes
        // ensured), never patched column-by-column.
        createAllTables(db);
        createIndexes(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Development stage, v1 only: the pre-v1 social tables are discarded
        // and rebuilt under their v1 names. Bank cards and IDs are untouched,
        // so everything else survives. Never migrated.
        db.execSQL("DROP TABLE IF EXISTS logins");
        db.execSQL("DROP TABLE IF EXISTS associations");
        createAllTables(db);
        createIndexes(db);
    }

    // ---- Shared row mapping (single definition per table) ----

    private static CredentialItem mapSocialAccount(Cursor cursor) {
        return new CredentialItem(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PLATFORM)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PIN)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ICON_RES)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_MOBILE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT)));
    }

    private static BankCardItem mapBankCard(Cursor cursor) {
        return new BankCardItem(
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
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT)));
    }

    private static IdCardItem mapIdCard(Cursor cursor) {
        return new IdCardItem(
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_CARD_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_TYPE)),
                IdCardItem.parseFieldsJson(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID_FIELDS_JSON))),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID_DESIGN)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT)));
    }

    private static final String ACTIVE_SOCIAL_ACCOUNTS_WHERE =
            "(" + COL_DELETED_AT + " IS NULL OR " + COL_DELETED_AT + "=0)";
    private static final String TRASHED_WHERE =
            COL_DELETED_AT + " IS NOT NULL AND " + COL_DELETED_AT + " != 0";

    // ---- Social accounts ----

    private static ContentValues socialAccountValues(CredentialItem item, long now, boolean fresh) {
        ContentValues cv = new ContentValues();
        cv.put(COL_PLATFORM, item.getPlatform());
        cv.put(COL_USERNAME, item.getUsername());
        cv.put(COL_PASSWORD, item.getPassword());
        cv.put(COL_PIN, item.getPin());
        cv.put(COL_ICON_RES, item.getIconRes());
        cv.put(COL_MOBILE, item.getMobile() != null ? item.getMobile() : "");
        if (fresh) {
            // Fresh rows are both created and updated now; honor
            // caller-supplied values when present.
            cv.put(COL_CREATED_AT, item.getCreatedAt() > 0 ? item.getCreatedAt() : now);
            cv.put(COL_UPDATED_AT, item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
            cv.put(COL_DELETED_AT, 0);
        } else {
            // created_at is immutable; every edit bumps updated_at.
            cv.put(COL_UPDATED_AT,
                    item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
        }
        return cv;
    }

    private static long insertSocialAccount(SQLiteDatabase db, CredentialItem item) {
        return db.insert(TABLE_SOCIAL_ACCOUNTS, null,
                socialAccountValues(item, System.currentTimeMillis(), true));
    }

    /**
     * In-place update: preserves row id, created_at and every reverse link
     * pointing at this account.
     */
    private static void updateSocialAccount(SQLiteDatabase db, CredentialItem item) {
        db.update(TABLE_SOCIAL_ACCOUNTS,
                socialAccountValues(item, System.currentTimeMillis(), false),
                COL_ID + "=?", new String[]{String.valueOf(item.getId())});
    }

    /**
     * Atomic save: insert-or-update the row plus its forward links in one
     * transaction on one connection. Reverse links from other accounts are
     * untouched (only this row's outgoing edges are replaced).
     *
     * @return the row id (existing id for edits, new id for adds).
     */
    public long saveSocialAccountWithLinks(CredentialItem item, List<Integer> linkedIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            long id;
            if (item.getId() > 0) {
                updateSocialAccount(db, item);
                id = item.getId();
            } else {
                id = insertSocialAccount(db, item);
                if (id < 0) {
                    return -1;
                }
            }
            String[] args = {String.valueOf(id)};
            db.delete(TABLE_ACCOUNT_LINKS, COL_ACCOUNT_ID + "=?", args);
            if (linkedIds != null) {
                for (Integer linkedId : linkedIds) {
                    if (linkedId == null || linkedId == id) {
                        continue;
                    }
                    ContentValues cv = new ContentValues();
                    cv.put(COL_ACCOUNT_ID, id);
                    cv.put(COL_LINKED_ACCOUNT_ID, linkedId);
                    db.insert(TABLE_ACCOUNT_LINKS, null, cv);
                }
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public CredentialItem getSocialAccountById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SOCIAL_ACCOUNTS
                + " WHERE " + COL_ID + "=?", new String[]{String.valueOf(id)})) {
            if (cursor.moveToFirst()) {
                return mapSocialAccount(cursor);
            }
            return null;
        } finally {
            db.close();
        }
    }

    public List<CredentialItem> getAllSocialAccounts() {
        List<CredentialItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Active rows only: trashed rows live in Trash (Settings).
        // Newest-first by recency; id breaks ties on equal stamps.
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SOCIAL_ACCOUNTS
                + " WHERE " + ACTIVE_SOCIAL_ACCOUNTS_WHERE
                + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_ID + " DESC", null)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapSocialAccount(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            db.close();
        }
    }

    /** Permanent delete for a set of ids in one transaction. */
    public void deleteSocialAccounts(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                String[] both = {String.valueOf(id), String.valueOf(id)};
                db.delete(TABLE_ACCOUNT_LINKS,
                        COL_ACCOUNT_ID + "=? OR " + COL_LINKED_ACCOUNT_ID + "=?", both);
                db.delete(TABLE_SOCIAL_ACCOUNTS, COL_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Soft-delete: stamps deleted_at so the account disappears from the
     * dashboard and appears in Settings > Trash. Links are kept so a
     * restore brings links back; permanent delete cleans them up.
     */
    public void moveSocialAccountToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, System.currentTimeMillis());
            db.update(TABLE_SOCIAL_ACCOUNTS, cv, COL_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Restores trashed accounts back to the dashboard (deleted_at = 0). */
    public void restoreSocialAccounts(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                db.update(TABLE_SOCIAL_ACCOUNTS, cv, COL_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /** Trashed social accounts, newest-deleted first. */
    public List<CredentialItem> getTrashedSocialAccounts() {
        List<CredentialItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SOCIAL_ACCOUNTS
                + " WHERE " + TRASHED_WHERE
                + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID + " DESC", null)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapSocialAccount(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            db.close();
        }
    }

    /**
     * Marks an account as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called when the
     * account is opened for editing; the list re-sorts on the next refresh.
     */
    public void touchSocialAccountUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_UPDATED_AT, System.currentTimeMillis());
            db.update(TABLE_SOCIAL_ACCOUNTS, cv, COL_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    // ---- Bank cards ----

    private static ContentValues bankCardValues(BankCardItem item, long now, boolean fresh) {
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
        if (fresh) {
            // Fresh rows are both created and updated now; honor
            // caller-supplied values (e.g. imports) when present.
            cv.put(COL_CREATED_AT, item.getCreatedAt() > 0 ? item.getCreatedAt() : now);
            cv.put(COL_UPDATED_AT, item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
            cv.put(COL_DELETED_AT, 0);
        } else {
            // created_at is immutable: never overwritten.
            cv.put(COL_UPDATED_AT,
                    item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
        }
        return cv;
    }

    public void insertBankCard(BankCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.insert(TABLE_BANK_CARDS, null,
                    bankCardValues(item, System.currentTimeMillis(), true));
        } finally {
            db.close();
        }
    }

    public BankCardItem getBankCardById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                + " WHERE " + COL_CARD_ID + "=?", new String[]{String.valueOf(id)})) {
            if (cursor.moveToFirst()) {
                return mapBankCard(cursor);
            }
            return null;
        } finally {
            db.close();
        }
    }

    public List<BankCardItem> getAllBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Active rows only; trashed cards live in Trash.
        // Newest-first by recency; id breaks ties on equal stamps.
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                + " WHERE " + ACTIVE_SOCIAL_ACCOUNTS_WHERE
                + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_CARD_ID + " DESC", null)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapBankCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            db.close();
        }
    }

    public void updateBankCard(BankCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.update(TABLE_BANK_CARDS,
                    bankCardValues(item, System.currentTimeMillis(), false),
                    COL_CARD_ID + "=?", new String[]{String.valueOf(item.getId())});
        } finally {
            db.close();
        }
    }

    /** Permanent delete for a set of ids in one transaction. */
    public void deleteBankCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                db.delete(TABLE_BANK_CARDS, COL_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Soft-delete: moves the card to Trash (Settings). The dashboard hides it
     * until restored or permanently deleted.
     */
    public void moveBankCardToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, System.currentTimeMillis());
            db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Restores trashed cards back to the dashboard. */
    public void restoreBankCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /** Trashed bank cards, newest-deleted first. */
    public List<BankCardItem> getTrashedBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                + " WHERE " + TRASHED_WHERE
                + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_CARD_ID + " DESC", null)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapBankCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            db.close();
        }
    }

    /**
     * Marks a card as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called on
     * dashboard tap; the list re-sorts on the next refresh.
     */
    public void touchBankCardUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_UPDATED_AT, System.currentTimeMillis());
            db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    // ---- ID cards ----

    private static ContentValues idCardValues(IdCardItem item, long now, boolean fresh) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ID_TYPE, item.getIdType());
        cv.put(COL_ID_FIELDS_JSON, item.getFieldsJson());
        cv.put(COL_ID_DESIGN, item.getDesign());
        if (fresh) {
            // Honor caller-supplied values (e.g. imports) when present.
            // The JSON blob is untouched — stamps live in their own columns.
            cv.put(COL_CREATED_AT, item.getCreatedAt() > 0 ? item.getCreatedAt() : now);
            cv.put(COL_UPDATED_AT, item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
            cv.put(COL_DELETED_AT, 0);
        } else {
            // created_at is immutable: never overwritten.
            cv.put(COL_UPDATED_AT,
                    item.getUpdatedAt() > 0 ? item.getUpdatedAt() : now);
        }
        return cv;
    }

    public void insertIdCard(IdCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.insert(TABLE_ID_CARDS, null,
                    idCardValues(item, System.currentTimeMillis(), true));
        } finally {
            db.close();
        }
    }

    public IdCardItem getIdCardById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                + " WHERE " + COL_ID_CARD_ID + "=?", new String[]{String.valueOf(id)})) {
            if (cursor.moveToFirst()) {
                return mapIdCard(cursor);
            }
            return null;
        } finally {
            db.close();
        }
    }

    public List<IdCardItem> getAllIdCards() {
        List<IdCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Active rows only; trashed IDs live in Trash.
        // Newest-first by recency, matching bank cards and social accounts;
        // id breaks ties on equal stamps.
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                + " WHERE " + ACTIVE_SOCIAL_ACCOUNTS_WHERE
                + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_ID_CARD_ID + " DESC", null)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapIdCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            db.close();
        }
    }

    public void updateIdCard(IdCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.update(TABLE_ID_CARDS,
                    idCardValues(item, System.currentTimeMillis(), false),
                    COL_ID_CARD_ID + "=?", new String[]{String.valueOf(item.getId())});
        } finally {
            db.close();
        }
    }

    /**
     * Marks an ID as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called on
     * dashboard tap, mirroring the bank-card and social-account touch helpers;
     * list re-sorts on the next refresh.
     */
    public void touchIdCardUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_UPDATED_AT, System.currentTimeMillis());
            db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Permanent delete for a set of ids in one transaction. */
    public void deleteIdCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                db.delete(TABLE_ID_CARDS, COL_ID_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Soft-delete: moves the ID to Trash (Settings). The dashboard hides it
     * until restored or permanently deleted.
     */
    public void moveIdCardToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, System.currentTimeMillis());
            db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Restores trashed IDs back to the dashboard. */
    public void restoreIdCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /** Trashed government IDs, newest-deleted first. */
    public List<IdCardItem> getTrashedIdCards() {
        List<IdCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                + " WHERE " + TRASHED_WHERE
                + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID_CARD_ID + " DESC", null)) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapIdCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            db.close();
        }
    }

    public List<Integer> getLinkedAccountIds(long accountId) {
        List<Integer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT " + COL_LINKED_ACCOUNT_ID + " FROM " + TABLE_ACCOUNT_LINKS
                + " WHERE " + COL_ACCOUNT_ID + "=?", new String[]{String.valueOf(accountId)})) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getInt(0));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            db.close();
        }
    }
}
