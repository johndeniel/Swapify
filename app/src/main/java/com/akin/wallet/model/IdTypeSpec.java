package com.akin.wallet.model;

import android.text.InputType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for every supported government ID type.
 *
 * <p>Adding Passport / SSS / GSIS later = append one {@link IdType} here.
 * No DB migration, no new XML, no adapter change needed because storage is
 * JSON ({@code fields_json}) and both the list card and the form are rendered
 * dynamically from these specs.
 */
public final class IdTypeSpec {

    public static final String TYPE_NATIONAL_ID = "National ID";
    public static final String TYPE_DRIVERS_LICENSE = "Driver's License";

    private static final String[] SEX_OPTIONS =
            {"Male", "Female"};
    private static final String[] BLOOD_OPTIONS =
            {"Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private static final String[] CIVIL_STATUS_OPTIONS =
            {"Single", "Married", "Widowed", "Divorced", "Separated"};

    private IdTypeSpec() {
    }

    /** One input field definition. */
    public static class IdField {
        public final String key;
        public final String label;
        public final String hint;
        public final boolean required;
        public final boolean sensitive;
        public final String[] options; // non-null => dropdown instead of free text
        public final int inputType;
        public final int maxLength; // 0 = no limit

        public IdField(String key, String label, String hint, boolean required,
                       boolean sensitive, String[] options, int inputType, int maxLength) {
            this.key = key;
            this.label = label;
            this.hint = hint;
            this.required = required;
            this.sensitive = sensitive;
            this.options = options;
            this.inputType = inputType;
            this.maxLength = maxLength;
        }

        public boolean isDropdown() {
            return options != null && options.length > 0;
        }

        public static IdField text(String key, String label, String hint,
                                   boolean required, boolean sensitive, int maxLength) {
            return new IdField(key, label, hint, required, sensitive, null,
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, maxLength);
        }

        public static IdField number(String key, String label, String hint,
                                     boolean required, boolean sensitive, int maxLength) {
            return new IdField(key, label, hint, required, sensitive, null,
                    InputType.TYPE_CLASS_NUMBER, maxLength);
        }

        public static IdField dropdown(String key, String label,
                                       boolean required, String[] options) {
            return new IdField(key, label, "Select", required, false,
                    options, InputType.TYPE_CLASS_TEXT, 0);
        }
    }

    /** One ID type definition. */
    public static class IdType {
        public final String name;
        public final String numberKey; // primary number shown masked on preview
        public final String nameKey;   // holder name key (usually "full_name")
        public final List<IdField> fields;

        IdType(String name, String numberKey, String nameKey, List<IdField> fields) {
            this.name = name;
            this.numberKey = numberKey;
            this.nameKey = nameKey;
            this.fields = Collections.unmodifiableList(fields);
        }

        public IdField findField(String key) {
            for (IdField f : fields) {
                if (f.key.equals(key)) {
                    return f;
                }
            }
            return null;
        }
    }

    private static final List<IdType> TYPES;

    static {
        List<IdType> list = new ArrayList<>();

        list.add(new IdType(TYPE_NATIONAL_ID, "psn", "full_name", Arrays.asList(
                IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.number("psn", "PSN (PhilSys Number)", "1234 5678 9012 3456", true, true, 19),
                IdField.text("birth_date", "Date of Birth", "YYYY-MM-DD", false, false, 10),
                IdField.dropdown("sex", "Sex", false, SEX_OPTIONS),
                IdField.dropdown("blood_type", "Blood Type", false, BLOOD_OPTIONS),
                IdField.text("place_of_birth", "Place of Birth", "Manila, PH", false, false, 80),
                IdField.text("present_address", "Present Address", "Street, City", false, false, 120),
                IdField.dropdown("marital_status", "Marital Status", false, CIVIL_STATUS_OPTIONS)
        )));

        list.add(new IdType(TYPE_DRIVERS_LICENSE, "license_no", "full_name", Arrays.asList(
                IdField.text("license_no", "License No.", "N01-23-456789", true, true, 20),
                IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.text("address", "Address", "Street, City", false, false, 120),
                IdField.text("nationality", "Nationality", "Filipino", false, false, 40),
                IdField.dropdown("sex", "Sex", false, SEX_OPTIONS),
                IdField.text("birth_date", "Date of Birth", "YYYY-MM-DD", false, false, 10),
                IdField.text("expiry_date", "Expiry Date", "YYYY-MM-DD", false, false, 10),
                IdField.dropdown("blood_type", "Blood Type", false, BLOOD_OPTIONS),
                IdField.text("restrictions", "Restrictions", "e.g. 1, 2", false, false, 20),
                IdField.text("conditions", "Conditions", "e.g. A", false, false, 20)
        )));

        TYPES = Collections.unmodifiableList(list);
    }

    public static List<IdType> getAllTypes() {
        return TYPES;
    }

    public static String[] getTypeNames() {
        String[] names = new String[TYPES.size()];
        for (int i = 0; i < TYPES.size(); i++) {
            names[i] = TYPES.get(i).name;
        }
        return names;
    }

    public static IdType forName(String name) {
        if (name != null) {
            for (IdType t : TYPES) {
                if (t.name.equalsIgnoreCase(name.trim())) {
                    return t;
                }
            }
        }
        return TYPES.get(0);
    }

    public static int indexOf(String name) {
        if (name != null) {
            for (int i = 0; i < TYPES.size(); i++) {
                if (TYPES.get(i).name.equalsIgnoreCase(name.trim())) {
                    return i;
                }
            }
        }
        return 0;
    }

    public static boolean isKnownType(String name) {
        if (name == null) {
            return false;
        }
        for (IdType t : TYPES) {
            if (t.name.equalsIgnoreCase(name.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fallback spec for data whose type is no longer known (forward-compat).
     * Every stored key becomes a free-text field so edit never loses data.
     */
    public static IdType genericType(String name, Map<String, String> stored) {
        List<IdField> fields = new ArrayList<>();
        if (stored != null && !stored.isEmpty()) {
            for (String key : stored.keySet()) {
                fields.add(IdField.text(key, toLabel(key), "", false, false, 0));
            }
        } else {
            fields.add(IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80));
            fields.add(IdField.text("id_number", "ID Number", "", true, true, 40));
        }
        String safeName = name != null && !name.trim().isEmpty() ? name.trim() : "Government ID";
        String numberKey = stored != null && stored.containsKey("id_number") ? "id_number"
                : (!fields.isEmpty() ? fields.get(fields.size() > 1 ? 1 : 0).key : "id_number");
        return new IdType(safeName, numberKey, "full_name", fields);
    }

    /** Sub-line under the card title, per type. Never null. */
    public static String previewSubtitle(String typeName) {
        if (typeName != null) {
            if (typeName.trim().equalsIgnoreCase(TYPE_NATIONAL_ID)) {
                return "PHILIPPINE IDENTIFICATION";
            }
            if (typeName.trim().equalsIgnoreCase(TYPE_DRIVERS_LICENSE)) {
                return "LAND TRANSPORT OFFICE";
            }
            if (!typeName.trim().isEmpty()) {
                return "GOVERNMENT ID";
            }
        }
        return "GOVERNMENT ID";
    }

    /** Face label for the primary number, per type (PSN, LICENSE NO., …). */
    public static String numberLabel(String typeName) {
        IdType spec = forName(typeName);
        String key = spec.numberKey;
        if ("psn".equalsIgnoreCase(key)) {
            return "PSN";
        }
        if ("license_no".equalsIgnoreCase(key)) {
            return "LICENSE NO.";
        }
        if ("id_number".equalsIgnoreCase(key)) {
            return "ID NUMBER";
        }
        return toLabel(key).toUpperCase();
    }

    /** Footer line for the collapsed preview, per type. Never null. */
    public static String buildPreviewMeta(IdType type, Map<String, String> fields) {
        if (type.name.equals(TYPE_DRIVERS_LICENSE)) {
            return joinNonEmpty(
                    nonEmpty(fields.get("birth_date")) ? "DOB " + fields.get("birth_date") : "",
                    nonEmpty(fields.get("expiry_date")) ? "EXP " + fields.get("expiry_date") : "");
        }
        // National ID + default
        return joinNonEmpty(
                nonEmpty(fields.get("birth_date")) ? "DOB " + fields.get("birth_date") : "",
                nonEmpty(fields.get("sex")) ? fields.get("sex") : "");
    }

    /** Mask sensitive numbers: show last 4 like bank cards, bullets otherwise. */
    public static String maskNumber(String raw) {
        if (raw == null) {
            return "••••";
        }
        String digits = raw.replaceAll("[^A-Za-z0-9]", "");
        if (digits.isEmpty()) {
            return "••••";
        }
        String last4 = digits.length() > 4 ? digits.substring(digits.length() - 4) : digits;
        return "•••• •••• " + last4;
    }

    public static String maskAll(String value) {
        if (value == null || value.isEmpty()) {
            return "Not set";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            sb.append('•');
        }
        return sb.toString();
    }

    /** Ordered display map: spec fields first, then any unknown stored keys (forward-compat). */
    public static Map<String, IdField> displayFields(IdType type, Map<String, String> stored) {
        Map<String, IdField> ordered = new LinkedHashMap<>();
        for (IdField f : type.fields) {
            ordered.put(f.key, f);
        }
        if (stored != null) {
            for (String key : stored.keySet()) {
                if (!ordered.containsKey(key)) {
                    ordered.put(key, IdField.text(key, toLabel(key), "", false, false, 0));
                }
            }
        }
        return ordered;
    }

    private static boolean nonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String joinNonEmpty(String a, String b) {
        boolean ea = a == null || a.isEmpty();
        boolean eb = b == null || b.isEmpty();
        if (ea && eb) {
            return "";
        }
        if (ea) {
            return b;
        }
        if (eb) {
            return a;
        }
        return a + "   •   " + b;
    }

    private static String toLabel(String key) {
        String[] parts = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }
}
