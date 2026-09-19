package com.akin.wallet.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Government ID entry. Variable per-type fields are stored as a JSON object
 * in SQLite (fields_json column) so National ID, Passport,
 * SSS, GSIS, etc. all share one table and one card UI.
 *
 * <p>Audit timestamps ({@code createdAt}/{@code updatedAt}) are first-class
 * columns sibling to {@code id} — never keys inside the JSON document.
 * Keeping them out of the JSON preserves the per-type field contract and lets
 * recency ordering reuse the same {@code updated_at DESC, id DESC} pattern as
 * bank cards and social accounts.
 *
 * <p>No {@code toString()} by design: document fields are sensitive and must
 * never reach logcat through an implicit string conversion.
 */
public class IdCardItem {

    /** Row id for drafts that have never been persisted. */
    public static final int UNSET_ID = -1;

    private final int id;
    private final String idType;
    private final Map<String, String> fields;
    private final long createdAt;
    private final long updatedAt;

    /** Unsaved draft; the database assigns the id and timestamps on insert. */
    public IdCardItem(String idType, Map<String, String> fields) {
        this(UNSET_ID, idType, fields, 0, 0);
    }

    /**
     * Full constructor carrying audit timestamps alongside the row identity.
     * New entries pass {@code 0, 0} and let the DB stamp {@code now};
     * updates preserve {@code createdAt} and pass {@code 0} for
     * {@code updatedAt} so the DB bumps recency.
     */
    public IdCardItem(int id, String idType, Map<String, String> fields,
                      long createdAt, long updatedAt) {
        this.id = id;
        this.idType = idType != null ? idType : "";
        this.fields = fields != null
                ? new LinkedHashMap<>(fields)
                : new LinkedHashMap<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getId() {
        return id;
    }

    public String getIdType() {
        return idType;
    }

    /** Defensive copy — callers cannot mutate internal state. */
    public Map<String, String> getFields() {
        return new LinkedHashMap<>(fields);
    }

    /** Row creation time, epoch millis. 0 when unset. */
    public long getCreatedAt() {
        return createdAt;
    }

    /** Last recency bump, epoch millis. Drives newest-first ordering. */
    public long getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Serialize document fields to JSON for SQLite storage.
     * Timestamps are deliberately excluded: they live in their own columns.
     */
    public String getFieldsJson() {
        JSONObject jsonDocument = new JSONObject();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            putField(jsonDocument, field.getKey(), field.getValue());
        }
        return jsonDocument.toString();
    }

    /**
     * JSONObject.put only throws for null keys or non-finite numbers; our keys
     * are non-null strings and values are normalized to "", so this is
     * provably non-throwing and intentionally silent.
     */
    private static void putField(JSONObject jsonDocument, String key, String value) {
        try {
            jsonDocument.put(key, value != null ? value : "");
        } catch (JSONException impossible) {
            throw new AssertionError("String keys never fail JSONObject.put", impossible);
        }
    }

    /** Parse JSON string from SQLite. Never throws — returns empty map on bad input. */
    public static Map<String, String> parseFieldsJson(String jsonPayload) {
        Map<String, String> parsedFields = new LinkedHashMap<>();
        if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
            return parsedFields;
        }
        try {
            JSONObject jsonDocument = new JSONObject(jsonPayload);
            Iterator<String> keys = jsonDocument.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                parsedFields.put(key, jsonDocument.optString(key, ""));
            }
        } catch (JSONException malformed) {
            // Forward-compat: a corrupt row degrades to an empty form rather
            // than crashing the list. The error is deliberate silence, not
            // swallowed diagnostics — there is nothing actionable to log.
            parsedFields.clear();
        }
        return parsedFields;
    }

    /** Value equality across every column (backs DiffUtil content checks). */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IdCardItem)) {
            return false;
        }
        IdCardItem that = (IdCardItem) o;
        return id == that.id
                && createdAt == that.createdAt
                && updatedAt == that.updatedAt
                && Objects.equals(idType, that.idType)
                && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idType, fields, createdAt, updatedAt);
    }
}
