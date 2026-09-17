package com.akin.wallet.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Government ID entry. Variable per-type fields are stored as a JSON object
 * in SQLite (fields_json column) so National ID, Driver's License, Passport,
 * SSS, GSIS, etc. all share one table and one card UI.
 */
public class IdCardItem {
    private final int id;
    private final String idType;
    private final Map<String, String> fields;
    private final int design;

    public IdCardItem(String idType, Map<String, String> fields, int design) {
        this(-1, idType, fields, design);
    }

    public IdCardItem(int id, String idType, Map<String, String> fields, int design) {
        this.id = id;
        this.idType = idType != null ? idType : "";
        this.fields = fields != null ? new LinkedHashMap<>(fields) : new LinkedHashMap<>();
        this.design = design;
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

    /** Serialize fields map to JSON string for SQLite storage. */
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
    public static Map<String, String> parseFieldsJson(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }
        try {
            JSONObject obj = new JSONObject(json);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, obj.optString(key, ""));
            }
        } catch (JSONException ignored) {
        }
        return map;
    }
}
