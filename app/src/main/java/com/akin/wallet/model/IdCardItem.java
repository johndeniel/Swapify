package com.akin.wallet.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Government ID entry. Variable per-type fields are stored as a JSON object
 * in SQLite (fields_json column) so National ID, Driver's License, Passport,
 * SSS, GSIS, etc. all share one table and one card UI.
 *
 * <p>Audit timestamps ({@code createdAt}/{@code updatedAt}) are first-class
 * columns sibling to {@code id} — never keys inside {@code fields_json}.
 * Keeping them out of the JSON preserves the per-type field contract (TIN,
 * SSS, etc. carry only their own document data) and lets recency ordering
 * reuse the same {@code updated_at DESC, id DESC} pattern as bank cards and
 * logins.
 */
public class IdCardItem {
    private final int id;
    private final String idType;
    private final Map<String, String> fields;
    private final int design;
    // Epoch millis (UTC). 0 = unset (unsaved drafts); the DB fills real
    // values on insert.
    private final long createdAt;
    private final long updatedAt;

    public IdCardItem(String idType, Map<String, String> fields, int design) {
        this(-1, idType, fields, design, 0, 0);
    }

    /**
     * Full constructor carrying audit timestamps alongside the row identity.
     * New entries pass {@code 0, 0} and let the DB stamp {@code now};
     * updates preserve {@code createdAt} and pass {@code 0} for
     * {@code updatedAt} so the DB bumps recency.
     */
    public IdCardItem(int id, String idType, Map<String, String> fields, int design,
                      long createdAt, long updatedAt) {
        this.id = id;
        this.idType = idType != null ? idType : "";
        this.fields = fields != null ? new LinkedHashMap<>(fields) : new LinkedHashMap<>();
        this.design = design;
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

    public int getDesign() {
        return design;
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
     * Serialize fields map to JSON string for SQLite storage.
     * Timestamps are deliberately excluded: they live in their own columns.
     */
    public String getFieldsJson() {
        JSONObject obj = new JSONObject();
        for (Map.Entry<String, String> e : fields.entrySet()) {
            try {
                obj.put(e.getKey(), e.getValue() != null ? e.getValue() : "");
            } catch (JSONException ignored) {
            }
        }
        return obj.toString();
    }
    /** Parse JSON string from SQLite. Never throws — returns empty map on bad input. */
    public static Map<String, String> parseFieldsJson(String payload) {
        Map<String, String> map = new LinkedHashMap<>();
        if (payload == null || payload.trim().isEmpty()) {
            return map;
        }
        try {
            JSONObject obj = new JSONObject(payload);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, obj.optString(key, ""));
            }
        } catch (JSONException ignored) {
        }
        return map;
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
                && design == that.design
                && createdAt == that.createdAt
                && updatedAt == that.updatedAt
                && Objects.equals(idType, that.idType)
                && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idType, fields, design, createdAt, updatedAt);
    }
}
