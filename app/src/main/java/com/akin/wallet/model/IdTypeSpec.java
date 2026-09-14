package com.akin.wallet.model;

import android.text.InputType;

import com.akin.wallet.R;import java.util.ArrayList;
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
    public static final String TYPE_PASSPORT = "Passport";
    public static final String TYPE_SSS = "SSS";
    public static final String TYPE_PHILHEALTH = "PhilHealth ID";
    public static final String TYPE_TIN = "TIN ID";

    private static final String[] SEX_OPTIONS =
            {"Male", "Female"};
    private static final String[] BLOOD_OPTIONS =
            {"Unknown", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    private static final String[] CIVIL_STATUS_OPTIONS =
            {"Single", "Married", "Widowed", "Divorced", "Separated"};
    private static final String[] PHILHEALTH_MEMBER_OPTIONS =
            {"Formal Economy", "Informal Economy", "Indigent", "Sponsored",
                    "Senior Citizen", "Lifetime Member"};

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
        public final String numberKey; // primary number shown on the face
        public final List<IdField> fields;

        IdType(String name, String numberKey, List<IdField> fields) {
            this.name = name;
            this.numberKey = numberKey;
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

        list.add(new IdType(TYPE_NATIONAL_ID, "psn", Arrays.asList(
                IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.number("psn", "PSN (PhilSys Number)", "1234 5678 9012 3456", true, true, 19),
                IdField.text("birth_date", "Date of Birth", "YYYY-MM-DD", false, false, 10),
                IdField.dropdown("sex", "Sex", false, SEX_OPTIONS),
                IdField.dropdown("blood_type", "Blood Type", false, BLOOD_OPTIONS),
                IdField.text("place_of_birth", "Place of Birth", "Manila, PH", false, false, 80),
                IdField.text("present_address", "Present Address", "Street, City", false, false, 120),
                IdField.dropdown("marital_status", "Marital Status", false, CIVIL_STATUS_OPTIONS)
        )));

        list.add(new IdType(TYPE_DRIVERS_LICENSE, "license_no", Arrays.asList(
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

        list.add(new IdType(TYPE_PASSPORT, "passport_no", Arrays.asList(
                IdField.text("passport_no", "Passport No.", "P1234567A", true, true, 20),
                IdField.text("surname", "Surname", "Dela Cruz", true, false, 60),
                IdField.text("given_names", "Given Names", "Juan", true, false, 80),
                IdField.text("middle_name", "Middle Name", "Santos", false, false, 60),
                IdField.text("nationality", "Nationality", "Filipino", false, false, 40),
                IdField.dropdown("sex", "Sex", false, SEX_OPTIONS),
                IdField.text("birth_date", "Date of Birth", "YYYY-MM-DD", false, false, 10),
                IdField.text("place_of_birth", "Place of Birth", "Manila, PH", false, false, 80),
                IdField.text("issue_date", "Date of Issue", "YYYY-MM-DD", false, false, 10),
                IdField.text("expiry_date", "Date of Expiry", "YYYY-MM-DD", false, false, 10),
                IdField.text("issuing_authority", "Issuing Authority", "DFA Manila", false, false, 60)
        )));

        list.add(new IdType(TYPE_SSS, "ss_number", Arrays.asList(
                IdField.text("ss_number", "SS Number", "34-1234567-8", true, true, 14),
                IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.text("birth_date", "Date of Birth", "YYYY-MM-DD", false, false, 10),
                IdField.dropdown("sex", "Sex", false, SEX_OPTIONS),
                IdField.text("address", "Address", "Street, City", false, false, 120),
                IdField.text("mobile_no", "Mobile No.", "09XX XXX XXXX", false, false, 20)
        )));

        list.add(new IdType(TYPE_PHILHEALTH, "philhealth_no", Arrays.asList(
                IdField.text("philhealth_no", "PhilHealth No.", "01-2345678-9", true, true, 16),
                IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.text("birth_date", "Date of Birth", "YYYY-MM-DD", false, false, 10),
                IdField.dropdown("sex", "Sex", false, SEX_OPTIONS),
                IdField.text("address", "Address", "Street, City", false, false, 120),
                IdField.dropdown("membership_type", "Membership Type", false, PHILHEALTH_MEMBER_OPTIONS)
        )));

        list.add(new IdType(TYPE_TIN, "tin", Arrays.asList(
                IdField.text("tin", "TIN", "123-456-789-000", true, true, 15),
                IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.text("birth_date", "Date of Birth", "YYYY-MM-DD", false, false, 10),
                IdField.dropdown("sex", "Sex", false, SEX_OPTIONS),
                IdField.text("address", "Address", "Street, City", false, false, 120),
                IdField.text("employer_name", "Employer Name", "Company Inc.", false, false, 80)
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
        return new IdType(safeName, numberKey, fields);
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
            if (typeName.trim().equalsIgnoreCase(TYPE_PASSPORT)) {
                return "DEPARTMENT OF FOREIGN AFFAIRS";
            }
            if (typeName.trim().equalsIgnoreCase(TYPE_SSS)) {
                return "SOCIAL SECURITY SYSTEM";
            }
            if (typeName.trim().equalsIgnoreCase(TYPE_PHILHEALTH)) {
                return "PHILHEALTH";
            }
            if (typeName.trim().equalsIgnoreCase(TYPE_TIN)) {
                return "BUREAU OF INTERNAL REVENUE";
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
        if ("passport_no".equalsIgnoreCase(key)) {
            return "PASSPORT NO.";
        }
        if ("ss_number".equalsIgnoreCase(key)) {
            return "SS NUMBER";
        }
        if ("philhealth_no".equalsIgnoreCase(key)) {
            return "PHILHEALTH NO.";
        }
        if ("tin".equalsIgnoreCase(key)) {
            return "TIN";
        }
        if ("id_number".equalsIgnoreCase(key)) {
            return "ID NUMBER";
        }
        return toLabel(key).toUpperCase();
    }

    /**
     * Face theming for the single shared preview layout. The design (which
     * background + ink) varies per ID type, but the XML stays the same, so
     * adding a type never needs a new layout. Unknown types reuse National.
     */
    public static class FaceScheme {
        public final int backgroundRes;
        public final int titleColorRes;
        public final int subtitleColorRes;
        public final int ruleColorRes;
        public final int holderColorRes;
        public final int numberLabelColorRes;
        public final int numberColorRes;
        public final int metaColorRes;
        public final int photoBgRes;
        public final int photoBorderRes;
        public final int photoIconRes;

        FaceScheme(int backgroundRes, int titleColorRes, int subtitleColorRes,
                   int ruleColorRes, int holderColorRes, int numberLabelColorRes,
                   int numberColorRes, int metaColorRes,
                   int photoBgRes, int photoBorderRes, int photoIconRes) {
            this.backgroundRes = backgroundRes;
            this.titleColorRes = titleColorRes;
            this.subtitleColorRes = subtitleColorRes;
            this.ruleColorRes = ruleColorRes;
            this.holderColorRes = holderColorRes;
            this.numberLabelColorRes = numberLabelColorRes;
            this.numberColorRes = numberColorRes;
            this.metaColorRes = metaColorRes;
            this.photoBgRes = photoBgRes;
            this.photoBorderRes = photoBorderRes;
            this.photoIconRes = photoIconRes;
        }
    }

    public static FaceScheme faceScheme(String typeName) {
        if (typeName != null) {
            String t = typeName.trim();
            if (t.equalsIgnoreCase(TYPE_DRIVERS_LICENSE)) {
                return new FaceScheme(
                        R.drawable.bg_drivers_license,
                        R.color.dl_ink, R.color.dl_accent, R.color.dl_bar,
                        R.color.dl_ink, R.color.dl_muted,
                        R.color.dl_ink, R.color.dl_muted,
                        R.color.photo_bg_dl, R.color.dl_bar, R.color.dl_ink);
            }
            if (t.equalsIgnoreCase(TYPE_PASSPORT)) {
                return new FaceScheme(
                        R.drawable.bg_passport,
                        R.color.passport_ink, R.color.passport_muted, R.color.passport_rule,
                        R.color.passport_ink, R.color.passport_muted,
                        R.color.passport_ink, R.color.passport_muted,
                        R.color.photo_bg_passport, R.color.passport_rule,
                        R.color.photo_icon_passport);
            }
            if (t.equalsIgnoreCase(TYPE_SSS)) {
                return new FaceScheme(
                        R.drawable.bg_sss,
                        R.color.text_primary, R.color.sss_muted, R.color.sss_muted,
                        R.color.text_primary, R.color.sss_muted,
                        R.color.text_primary, R.color.sss_muted,
                        R.color.photo_bg_sss, R.color.sss_muted, R.color.sss_bg_end);
            }
            if (t.equalsIgnoreCase(TYPE_PHILHEALTH)) {
                return new FaceScheme(
                        R.drawable.bg_philhealth,
                        R.color.philhealth_ink, R.color.philhealth_muted, R.color.philhealth_rule,
                        R.color.philhealth_ink, R.color.philhealth_muted,
                        R.color.philhealth_ink, R.color.philhealth_muted,
                        R.color.photo_bg_philhealth, R.color.philhealth_rule,
                        R.color.philhealth_ink);
            }
            if (t.equalsIgnoreCase(TYPE_TIN)) {
                return new FaceScheme(
                        R.drawable.bg_tin,
                        R.color.tin_ink, R.color.tin_muted, R.color.tin_rule,
                        R.color.tin_ink, R.color.tin_muted,
                        R.color.tin_ink, R.color.tin_muted,
                        R.color.photo_bg_tin, R.color.tin_rule, R.color.tin_ink);
            }
        }
        return new FaceScheme(
                R.drawable.bg_national_id,
                R.color.national_ink, R.color.national_muted, R.color.national_rule,
                R.color.national_ink, R.color.national_muted,
                R.color.national_ink, R.color.national_muted,
                R.color.photo_bg_national, R.color.national_rule, R.color.national_ink);
    }

    /** Footer line for the collapsed preview, per type. Never null. */
    public static String buildPreviewMeta(IdType type, Map<String, String> fields) {
        Map<String, String> safe = fields != null ? fields : new LinkedHashMap<>();
        String birth = nonEmpty(safe.get("birth_date")) ? safe.get("birth_date").trim() : "";
        String expiry = nonEmpty(safe.get("expiry_date")) ? safe.get("expiry_date").trim() : "";
        if (!expiry.isEmpty()) {
            return joinNonEmpty(
                    birth.isEmpty() ? "" : "DOB " + birth,
                    "EXP " + expiry);
        }
        // National ID, SSS, PhilHealth + default
        String sex = nonEmpty(safe.get("sex")) ? safe.get("sex").trim() : "";
        return joinNonEmpty(
                birth.isEmpty() ? "" : "DOB " + birth,
                sex);
    }

    /** Card-face holder line: full name, or Given + Surname for passports. */
    public static String displayName(IdType type, Map<String, String> fields) {
        Map<String, String> safe = fields != null ? fields : new LinkedHashMap<>();
        String full = safe.get("full_name");
        if (nonEmpty(full)) {
            return full.trim().toUpperCase();
        }
        String given = nonEmpty(safe.get("given_names")) ? safe.get("given_names").trim() : "";
        String surname = nonEmpty(safe.get("surname")) ? safe.get("surname").trim() : "";
        String combined = (given + " " + surname).trim();
        return combined.isEmpty() ? "FULL NAME" : combined.toUpperCase();
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
