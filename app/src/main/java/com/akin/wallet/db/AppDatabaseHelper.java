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

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "akin_wallet.db";
    // v10 consolidated every schema column (timestamps, mobile, trash stamp).
    // v11 changes the envelope, not the schema: the file is SQLCipher
    // encrypted (AES-256) with a Keystore-wrapped random key. The table/column
    // set is identical to v10, so the upgrade step below is unchanged apart
    // from the version gate; plaintext installs convert on first open.
    // v12 adds query indexes only (no columns) for the dashboard/trash
    // ordering and the associations join.
    // No per-version legacy paths are kept.
    private static final int DATABASE_VERSION = 12;

    /** Guards first-open plaintext conversion against concurrent helpers. */
    private static final Object ENCRYPTION_LOCK = new Object();

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

    private final Context appContext;

    public AppDatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
        SQLiteDatabase.loadLibs(appContext);
    }

    // All opens flow through the vault key: no caller touches raw schema or
    // plaintext. SQLCipher's helper only offers password-taking getters, so
    // these same-named no-arg wrappers are new methods (not overrides) that
    // keep every existing call site working.
    public SQLiteDatabase getWritableDatabase() {
        synchronized (ENCRYPTION_LOCK) {
            ensureEncrypted();
            return super.getWritableDatabase(DbKeyManager.getPassphrase(appContext));
        }
    }

    public SQLiteDatabase getReadableDatabase() {
        synchronized (ENCRYPTION_LOCK) {
            ensureEncrypted();
            return super.getReadableDatabase(DbKeyManager.getPassphrase(appContext));
        }
    }

    /**
     * One-time plaintext conversion for installs predating encryption.
     * Detected by file header ("SQLite format 3" = plaintext; SQLCipher
     * files start with random bytes). Exports into a fresh encrypted copy
     * via sqlcipher_export, then atomically swaps it in. Fresh installs
     * (no file yet) and already-encrypted files skip untouched.
     */
    private void ensureEncrypted() {
        File dbFile = appContext.getDatabasePath(DATABASE_NAME);
        if (dbFile == null || !dbFile.exists() || dbFile.length() < 16) {
            return;
        }
        try (FileInputStream in = new FileInputStream(dbFile)) {
            byte[] header = new byte[16];
            if (in.read(header) != 16 || !isPlaintextHeader(header)) {
                return;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Vault header unreadable", e);
        }
        char[] key = DbKeyManager.getPassphrase(appContext);
        File encFile = new File(dbFile.getAbsolutePath() + ".enc");
        if (encFile.exists() && !encFile.delete()) {
            throw new IllegalStateException("Vault conversion blocked");
        }
        SQLiteDatabase plain = null;
        try {
            // Empty passphrase opens the legacy plaintext file.
            plain = SQLiteDatabase.openOrCreateDatabase(
                    dbFile.getAbsolutePath(), "", null);
            plain.rawExecSQL("ATTACH DATABASE '"
                    + encFile.getAbsolutePath().replace("'", "''")
                    + "' AS enc KEY \"" + new String(key) + "\";");
            plain.rawExecSQL("SELECT sqlcipher_export('enc');");
            plain.rawExecSQL("DETACH DATABASE enc;");
        } finally {
            if (plain != null) {
                try {
                    plain.close();
                } catch (Exception ignored) {
                }
            }
        }
        if (!dbFile.delete() || !encFile.renameTo(dbFile)) {
            encFile.delete();
            throw new IllegalStateException("Vault conversion failed");
        }
    }

    private static boolean isPlaintextHeader(byte[] header) {
        byte[] magic = "SQLite format 3\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
        createIndexes(db);
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

    /** v12: indexes for the dashboard/trash ordering and association joins. */
    private static void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logins_active ON " + TABLE_LOGINS
                + " (" + COL_DELETED_AT + ", " + COL_UPDATED_AT + " DESC, " + COL_ID + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bank_cards_active ON " + TABLE_BANK_CARDS
                + " (" + COL_DELETED_AT + ", " + COL_UPDATED_AT + " DESC, " + COL_CARD_ID + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_id_cards_active ON " + TABLE_ID_CARDS
                + " (" + COL_DELETED_AT + ", " + COL_UPDATED_AT + " DESC, " + COL_ID_CARD_ID + " DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_assoc_login ON " + TABLE_ASSOCIATIONS
                + " (" + COL_LOGIN_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_assoc_associated ON " + TABLE_ASSOCIATIONS
                + " (" + COL_ASSOCIATED_ID + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Single consolidated migration, non-destructive (never drops data):
        // bring every older install to the exact v10/v11 column set. Missing
        // tables are created; any missing column is added in place. v11 adds
        // no columns — it is the encryption envelope (handled on open above).
        // v12 adds indexes only. No per-version branches, no backfills,
        // no legacy fallbacks anywhere.
        createAllTables(db);
        if (oldVersion < 11) {
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
        if (oldVersion < 12) {
            createIndexes(db);
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

    // ---- Shared row mapping (single definition per table) ----

    private static CredentialItem mapLogin(Cursor cursor) {
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

    private static final String ACTIVE_LOGINS_WHERE =
            "(" + COL_DELETED_AT + " IS NULL OR " + COL_DELETED_AT + "=0)";
    private static final String TRASHED_WHERE =
            COL_DELETED_AT + " IS NOT NULL AND " + COL_DELETED_AT + " != 0";

    // ---- Logins ----

    private static ContentValues loginValues(CredentialItem item, long now, boolean fresh) {
        ContentValues cv = new ContentValues();
        cv.put(COL_PLATFORM, item.getPlatform());
        cv.put(COL_USERNAME, item.getUsername());
        cv.put(COL_PASSWORD, item.getPassword());
        cv.put(COL_PIN, item.getPin());
        cv.put(COL_ICON_RES, item.getIconRes());
        cv.put(COL_MOBILE, item.getMobile() != null ? item.getMobile() : "");
        if (fresh) {
            // Fresh rows are both created and updated now; honour
            // caller-supplied values (edit reinsert) when present.
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

    private static long insertLogin(SQLiteDatabase db, CredentialItem item) {
        return db.insert(TABLE_LOGINS, null,
                loginValues(item, System.currentTimeMillis(), true));
    }

    public long insertLogin(CredentialItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return insertLogin(db, item);
        } finally {
            db.close();
        }
    }

    /**
     * In-place update: preserves row id, created_at and every reverse link
     * pointing at this account. Prefer over delete+reinsert for edits.
     */
    public int updateLogin(CredentialItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return updateLogin(db, item);
        } finally {
            db.close();
        }
    }

    private static int updateLogin(SQLiteDatabase db, CredentialItem item) {
        return db.update(TABLE_LOGINS,
                loginValues(item, System.currentTimeMillis(), false),
                COL_ID + "=?", new String[]{String.valueOf(item.getId())});
    }

    /**
     * Atomic save: insert-or-update the row plus its forward links in one
     * transaction on one connection. Reverse links from other accounts are
     * untouched (only this row's outgoing edges are replaced).
     *
     * @return the row id (existing id for edits, new id for adds).
     */
    public long saveLoginWithAssociations(CredentialItem item, List<Integer> associatedIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            long id;
            if (item.getId() > 0) {
                updateLogin(db, item);
                id = item.getId();
            } else {
                id = insertLogin(db, item);
                if (id < 0) {
                    return -1;
                }
            }
            String[] args = {String.valueOf(id)};
            db.delete(TABLE_ASSOCIATIONS, COL_LOGIN_ID + "=?", args);
            if (associatedIds != null) {
                for (Integer assocId : associatedIds) {
                    if (assocId == null || assocId == id) {
                        continue;
                    }
                    ContentValues cv = new ContentValues();
                    cv.put(COL_LOGIN_ID, id);
                    cv.put(COL_ASSOCIATED_ID, assocId);
                    db.insert(TABLE_ASSOCIATIONS, null, cv);
                }
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public CredentialItem getLoginById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_LOGINS
                    + " WHERE " + COL_ID + "=?", new String[]{String.valueOf(id)});
            if (cursor.moveToFirst()) {
                return mapLogin(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    public List<CredentialItem> getAllLogins() {
        List<CredentialItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Active rows only: trashed rows live in Trash (Settings).
            // Newest-first by recency; id breaks ties on equal stamps.
            cursor = db.rawQuery("SELECT * FROM " + TABLE_LOGINS
                    + " WHERE " + ACTIVE_LOGINS_WHERE
                    + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_ID + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapLogin(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    /**
     * Permanent delete: removes the row plus every association touching it.
     * Used by Trash "delete forever". User-facing deletes must call
     * {@link #moveLoginToTrash(int)} instead so the item lands in Trash.
     */
    public int deleteLogin(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_ASSOCIATIONS, COL_LOGIN_ID + "=? OR " + COL_ASSOCIATED_ID + "=?",
                    new String[]{String.valueOf(id), String.valueOf(id)});
            int rows = db.delete(TABLE_LOGINS, COL_ID + "=?",
                    new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return rows;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /** Permanent delete for a set of ids in one transaction. */
    public int deleteLogins(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int total = 0;
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                String[] both = {String.valueOf(id), String.valueOf(id)};
                db.delete(TABLE_ASSOCIATIONS,
                        COL_LOGIN_ID + "=? OR " + COL_ASSOCIATED_ID + "=?", both);
                total += db.delete(TABLE_LOGINS, COL_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            return total;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Soft-delete: stamps deleted_at so the account disappears from the
     * dashboard and appears in Settings > Trash. Associations are kept so a
     * restore brings links back; permanent delete cleans them up.
     */
    public int moveLoginToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, System.currentTimeMillis());
            return db.update(TABLE_LOGINS, cv, COL_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Restores trashed accounts back to the dashboard (deleted_at = 0). */
    public int restoreLogins(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            int total = 0;
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                total += db.update(TABLE_LOGINS, cv, COL_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            return total;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /** Restores a trashed account back to the dashboard (deleted_at = 0). */
    public int restoreLogin(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            return db.update(TABLE_LOGINS, cv, COL_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Trashed social accounts, newest-deleted first. */
    public List<CredentialItem> getTrashedLogins() {
        List<CredentialItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_LOGINS
                    + " WHERE " + TRASHED_WHERE
                    + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapLogin(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    /**
     * Marks an account as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called when the
     * account is opened for editing; the list re-sorts on the next refresh.
     */
    public int touchLoginUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_UPDATED_AT, System.currentTimeMillis());
            return db.update(TABLE_LOGINS, cv, COL_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    public void insertAssociation(long loginId, long associatedId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_LOGIN_ID, loginId);
            cv.put(COL_ASSOCIATED_ID, associatedId);
            db.insert(TABLE_ASSOCIATIONS, null, cv);
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
            // Fresh rows are both created and updated now; honour
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

    public long insertBankCard(BankCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.insert(TABLE_BANK_CARDS, null,
                    bankCardValues(item, System.currentTimeMillis(), true));
        } finally {
            db.close();
        }
    }

    public BankCardItem getBankCardById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                    + " WHERE " + COL_CARD_ID + "=?", new String[]{String.valueOf(id)});
            if (cursor.moveToFirst()) {
                return mapBankCard(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    public List<BankCardItem> getAllBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Active rows only; trashed cards live in Trash.
            // Newest-first by recency; id breaks ties on equal stamps.
            cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                    + " WHERE " + ACTIVE_LOGINS_WHERE
                    + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_CARD_ID + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapBankCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    public int updateBankCard(BankCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.update(TABLE_BANK_CARDS,
                    bankCardValues(item, System.currentTimeMillis(), false),
                    COL_CARD_ID + "=?", new String[]{String.valueOf(item.getId())});
        } finally {
            db.close();
        }
    }

    public int deleteBankCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.delete(TABLE_BANK_CARDS, COL_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Permanent delete for a set of ids in one transaction. */
    public int deleteBankCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int total = 0;
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                total += db.delete(TABLE_BANK_CARDS, COL_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            return total;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Soft-delete: moves the card to Trash (Settings). The dashboard hides it
     * until restored or permanently deleted.
     */
    public int moveBankCardToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, System.currentTimeMillis());
            return db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Restores trashed cards back to the dashboard. */
    public int restoreBankCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            int total = 0;
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                total += db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            return total;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /** Restores a trashed card back to the dashboard. */
    public int restoreBankCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            return db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Trashed bank cards, newest-deleted first. */
    public List<BankCardItem> getTrashedBankCards() {
        List<BankCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_BANK_CARDS
                    + " WHERE " + TRASHED_WHERE
                    + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_CARD_ID + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapBankCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    /**
     * Marks a card as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called on
     * dashboard tap; the list re-sorts on the next refresh.
     */
    public int touchBankCardUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_UPDATED_AT, System.currentTimeMillis());
            return db.update(TABLE_BANK_CARDS, cv, COL_CARD_ID + "=?",
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
            // Honour caller-supplied values (e.g. imports) when present.
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

    public long insertIdCard(IdCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.insert(TABLE_ID_CARDS, null,
                    idCardValues(item, System.currentTimeMillis(), true));
        } finally {
            db.close();
        }
    }

    public IdCardItem getIdCardById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                    + " WHERE " + COL_ID_CARD_ID + "=?", new String[]{String.valueOf(id)});
            if (cursor.moveToFirst()) {
                return mapIdCard(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    public List<IdCardItem> getAllIdCards() {
        List<IdCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Active rows only; trashed IDs live in Trash.
            // Newest-first by recency, matching bank cards and logins; id breaks
            // ties on equal stamps.
            cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                    + " WHERE " + ACTIVE_LOGINS_WHERE
                    + " ORDER BY " + COL_UPDATED_AT + " DESC, " + COL_ID_CARD_ID + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapIdCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    public int updateIdCard(IdCardItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.update(TABLE_ID_CARDS,
                    idCardValues(item, System.currentTimeMillis(), false),
                    COL_ID_CARD_ID + "=?", new String[]{String.valueOf(item.getId())});
        } finally {
            db.close();
        }
    }

    /**
     * Marks an ID as recently used without touching its data: bumps only
     * updated_at to now so newest-first ordering picks it up. Called on
     * dashboard tap, mirroring the bank-card and login touch helpers; the
     * list re-sorts on the next refresh.
     */
    public int touchIdCardUpdatedAt(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_UPDATED_AT, System.currentTimeMillis());
            return db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    public int deleteIdCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            return db.delete(TABLE_ID_CARDS, COL_ID_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Permanent delete for a set of ids in one transaction. */
    public int deleteIdCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int total = 0;
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                total += db.delete(TABLE_ID_CARDS, COL_ID_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            return total;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Soft-delete: moves the ID to Trash (Settings). The dashboard hides it
     * until restored or permanently deleted.
     */
    public int moveIdCardToTrash(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, System.currentTimeMillis());
            return db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Restores trashed IDs back to the dashboard. */
    public int restoreIdCards(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            int total = 0;
            for (Integer id : ids) {
                if (id == null) {
                    continue;
                }
                total += db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                        new String[]{String.valueOf(id)});
            }
            db.setTransactionSuccessful();
            return total;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /** Restores a trashed ID back to the dashboard. */
    public int restoreIdCard(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COL_DELETED_AT, 0);
            return db.update(TABLE_ID_CARDS, cv, COL_ID_CARD_ID + "=?",
                    new String[]{String.valueOf(id)});
        } finally {
            db.close();
        }
    }

    /** Trashed government IDs, newest-deleted first. */
    public List<IdCardItem> getTrashedIdCards() {
        List<IdCardItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_ID_CARDS
                    + " WHERE " + TRASHED_WHERE
                    + " ORDER BY " + COL_DELETED_AT + " DESC, " + COL_ID_CARD_ID + " DESC", null);
            if (cursor.moveToFirst()) {
                do {
                    list.add(mapIdCard(cursor));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }

    /** Total items currently in Trash (all three vault tables). */
    public int getTrashCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            return countWhere(db, TABLE_LOGINS) + countWhere(db, TABLE_BANK_CARDS)
                    + countWhere(db, TABLE_ID_CARDS);
        } finally {
            db.close();
        }
    }

    private static int countWhere(SQLiteDatabase db, String table) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + table
                    + " WHERE " + TRASHED_WHERE, null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public List<Integer> getAssociations(long loginId) {
        List<Integer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT " + COL_ASSOCIATED_ID + " FROM " + TABLE_ASSOCIATIONS
                    + " WHERE " + COL_LOGIN_ID + "=?", new String[]{String.valueOf(loginId)});
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getInt(0));
                } while (cursor.moveToNext());
            }
            return list;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
    }
}
