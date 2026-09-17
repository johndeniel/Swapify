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
        // Explicit row pairing: when true, this field shares its form row
        // with the next field regardless of their types. Used for custom rows
        // the generic rules can't infer (e.g. two full-width text fields).
        // Generic pairs (date/date, short-field/picker) need no flag.
        public final boolean pairWithNext;

        public IdField(String key, String label, String hint, boolean required,
                       boolean sensitive, String[] options, int inputType, int maxLength) {
            this(key, label, hint, required, sensitive, options, inputType, maxLength, false);
        }

        public IdField(String key, String label, String hint, boolean required,
                       boolean sensitive, String[] options, int inputType, int maxLength,
                       boolean pairWithNext) {
            this.key = key;
            this.label = label;
            this.hint = hint;
            this.required = required;
            this.sensitive = sensitive;
            this.options = options;
            this.inputType = inputType;
            this.maxLength = maxLength;
            this.pairWithNext = pairWithNext;
        }

        /**
         * Copy of this field that shares its form row with the next field.
         * The flag is consumed in spec order by the form builder; a flagged
         * last field is simply ignored.
         */
        public IdField pairedWithNext() {
            return new IdField(key, label, hint, required, sensitive, options,
                    inputType, maxLength, true);
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

        /**
         * Date entry with a date-optimized keyboard (numeric with separators).
         * No picker is imposed; the 8-digit shape (YYYYMMDD) is enforced in
         * form validation. Callers cap length at 8 so dashes can't be typed.
         */
        public static IdField date(String key, String label, String hint,
                                   boolean required, int maxLength) {
            return new IdField(key, label, hint, required, false, null,
                    InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_DATE,
                    maxLength);
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
    }

    private static final List<IdType> TYPES;

    static {
        List<IdType> list = new ArrayList<>();

        // National ID layout: PSN, then paired rows (name + sex, birth +
        // issue, blood type + marital status, birthplace + address). The
        // date/date and picker/picker rows pair by generic rule; name + sex
        // and birthplace + address pair by explicit flag since two full-width
        // text-led rows can't be inferred. Every field required. The PSN is
        // exactly 16 digits capped at 16 chars and regrouped 4-4-4-4 on the
        // face; both dates are exactly 8 digits (YYYYMMDD) capped at 8 chars.
        list.add(new IdType(TYPE_NATIONAL_ID, "psn", Arrays.asList(
                IdField.number("psn", "PSN (PhilSys Number)", "1234567890123456", true, true, 16),
                IdField.text("full_name", "Full Name", "Juan Dela Cruz", true, false, 80).pairedWithNext(),
                IdField.dropdown("sex", "Sex", true, SEX_OPTIONS),
                IdField.date("birth_date", "Date of Birth", "YYYYMMDD", true, 8),
                IdField.date("issue_date", "Date of Issue", "YYYYMMDD", true, 8),
                IdField.dropdown("blood_type", "Blood Type", true, BLOOD_OPTIONS),
                IdField.dropdown("marital_status", "Marital Status", true, CIVIL_STATUS_OPTIONS),
                IdField.text("place_of_birth", "Place of Birth", "Manila, PH", true, false, 80).pairedWithNext(),
                IdField.text("present_address", "Present Address", "Street, City", true, false, 120)
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

        // PhilHealth field contract: every field required; the short
        // identifiers share the top row (PhilHealth No. + Membership), then
        // full name with address directly below it, then the birth + sex row.
        // The number is exactly 12 digits capped at 12 chars and regrouped
        // 111-111-111-111 on the face; birth is exactly 8 digits (YYYYMMDD).
        list.add(new IdType(TYPE_PHILHEALTH, "philhealth_no", Arrays.asList(
                IdField.number("philhealth_no", "PhilHealth No.", "123456789012", true, true, 12),
                IdField.dropdown("membership", "Membership", true, PHILHEALTH_MEMBER_OPTIONS),
                IdField.text("fullName", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.text("address", "Address", "Street, City", true, false, 120),
                IdField.date("dateOfBirth", "Date of Birth", "YYYYMMDD", true, 8),
                IdField.dropdown("sex", "Sex", true, SEX_OPTIONS)
        )));

        // TIN field contract: every field required, 12-digit numeric TIN capped
        // at 12 chars, dates as exactly 8 digits (YYYYMMDD) capped at 8 chars.
        // The face regroups the bare digits as 111-111-111-111 and dashes
        // plain-digit dates for display.
        list.add(new IdType(TYPE_TIN, "tinNumber", Arrays.asList(
                IdField.number("tinNumber", "TIN", "123456789012", true, true, 12),
                IdField.text("fullname", "Full Name", "Juan Dela Cruz", true, false, 80),
                IdField.text("address", "Address", "Street, City", true, false, 120),
                IdField.date("dateOfBirth", "Date of Birth", "YYYYMMDD", true, 8),
                IdField.date("dateOfIssue", "Date of Issue", "YYYYMMDD", true, 8)
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
        if ("tinNumber".equalsIgnoreCase(key)) {
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
                        R.color.photo_bg_sss, R.color.sss_bg_start, R.color.sss_bg_end);
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

    /** Fourth face slot: the holder's sex, beside date of birth. */
    public static class FaceExtra {
        public final String label;
        public final String value;

        FaceExtra(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    public static FaceExtra faceExtra(String typeName, Map<String, String> fields) {
        Map<String, String> safe = fields != null ? fields : new LinkedHashMap<>();
        // TIN and National ID have no sex slot: their compact faces pair birth
        // with issue, matching their side-by-side date rows. Key varies by
        // type ("dateOfIssue" vs "issue_date"); first non-blank wins.
        if (typeName != null && (typeName.trim().equalsIgnoreCase(TYPE_TIN)
                || typeName.trim().equalsIgnoreCase(TYPE_NATIONAL_ID))) {
            return new FaceExtra("DATE OF ISSUE", displayDate(
                    firstNonEmpty(safe.get("dateOfIssue"), safe.get("issue_date"))));
        }
        // PhilHealth shows membership beside birth (no sex slot on its face),
        // matching the form's PhilHealth No. + Membership top row.
        if (typeName != null && typeName.trim().equalsIgnoreCase(TYPE_PHILHEALTH)) {
            return new FaceExtra("MEMBERSHIP", orDash(safe.get("membership")));
        }
        return new FaceExtra("SEX", orDash(safe.get("sex")));
    }

    private static String orDash(String s) {
        if (s == null || s.trim().isEmpty()) {
            return "—";
        }
        return s.trim();
    }

    /** Card-face holder line: full name, or Given + Surname for passports. */
    public static String displayName(IdType type, Map<String, String> fields) {
        Map<String, String> safe = fields != null ? fields : new LinkedHashMap<>();
        // Name key varies by type: PhilHealth "fullName", TIN "fullname",
        // everything else "full_name". First non-blank wins.
        String full = firstNonEmpty(safe.get("fullName"), safe.get("fullname"), safe.get("full_name"));
        if (nonEmpty(full)) {
            return full.trim().toUpperCase();
        }
        String given = nonEmpty(safe.get("given_names")) ? safe.get("given_names").trim() : "";
        String surname = nonEmpty(safe.get("surname")) ? safe.get("surname").trim() : "";
        String combined = (given + " " + surname).trim();
        return combined.isEmpty() ? "FULL NAME" : combined.toUpperCase();
    }

    /**
     * Card-face primary number. 12-digit numbers (TIN, PhilHealth No.) regroup
     * as 111-111-111-111 and the 16-digit PSN as 1111-1111-1111-1111;
     * everything else renders as stored.
     */
    public static String displayNumber(IdType spec, Map<String, String> fields) {
        Map<String, String> safe = fields != null ? fields : new LinkedHashMap<>();
        String numberKey = spec != null ? spec.numberKey : "";
        String primary = safe.get(numberKey);
        if (!nonEmpty(primary)) {
            return "—";
        }
        if (spec != null && usesGrouped12Display(spec.name)) {
            return formatGrouped12(primary);
        }
        if (spec != null && usesGrouped16Display(spec.name)) {
            return formatGrouped16(primary);
        }
        return primary.trim();
    }

    /** 12-digit document numbers (TIN, PhilHealth No.) group 3-3-3-3 on the face. */
    private static boolean usesGrouped12Display(String typeName) {
        return TYPE_TIN.equalsIgnoreCase(typeName)
                || TYPE_PHILHEALTH.equalsIgnoreCase(typeName);
    }

    /** The 16-digit PSN groups 4-4-4-4 on the face. */
    private static boolean usesGrouped16Display(String typeName) {
        return TYPE_NATIONAL_ID.equalsIgnoreCase(typeName);
    }

    /**
     * Face grouping for 12-digit numbers: 111-111-111-111. Storage holds the
     * bare 12 digits (the field caps at 12, validation counts digits), so
     * grouping is presentational only. Anything else passes through untouched;
     * validation guards new input.
     */
    public static String formatGrouped12(String raw) {
        String digits = raw != null ? raw.replaceAll("\\D", "") : "";
        if (digits.length() == 12) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6)
                    + "-" + digits.substring(6, 9) + "-" + digits.substring(9, 12);
        }
        return raw != null && !raw.trim().isEmpty() ? raw.trim() : "—";
    }

    /**
     * Face grouping for the 16-digit PSN: 1111-1111-1111-1111. Same contract
     * as the 12-digit grouping — storage holds bare digits, grouping is
     * presentational only, anything else passes through for validation to flag.
     */
    public static String formatGrouped16(String raw) {
        String digits = raw != null ? raw.replaceAll("\\D", "") : "";
        if (digits.length() == 16) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 8)
                    + "-" + digits.substring(8, 12) + "-" + digits.substring(12, 16);
        }
        return raw != null && !raw.trim().isEmpty() ? raw.trim() : "—";
    }

    /**
     * Face date normalization: always renders YYYY-MM-DD. Plain 8-digit input
     * (YYYYMMDD, accepted for hyphen-less keyboards) is dashed here; empty
     * renders as a dash. Anything else passes through as stored.
     */
    public static String displayDate(String raw) {
        String v = raw != null ? raw.trim() : "";
        if (v.isEmpty()) {
            return "—";
        }
        String digits = v.replaceAll("\\D", "");
        if (digits.length() == 8) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 6)
                    + "-" + digits.substring(6, 8);
        }
        return v;
    }

    private static boolean nonEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * First non-blank candidate, in preference order. Used where a value lives
     * under different keys per type (e.g. dateOfBirth vs birth_date).
     */
    private static String firstNonEmpty(String... candidates) {
        if (candidates != null) {
            for (String c : candidates) {
                if (c != null && !c.trim().isEmpty()) {
                    return c;
                }
            }
        }
        return "";
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
