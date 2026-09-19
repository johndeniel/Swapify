package com.akin.wallet.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * App-lock vault: 4-digit PIN setup/verification plus opt-in biometric
 * unlock. The PIN itself is never stored — only a salted PBKDF2-HMAC-SHA256
 * hash in private SharedPreferences. Hashes written by older installs
 * (single salted SHA-256) are verified once and transparently upgraded to
 * PBKDF2 on the next successful unlock. Biometrics stay disabled until the
 * user turns them on in Settings (and the device actually supports them).
 *
 * <p>Brute-force protection: {@link #MAX_ATTEMPTS} wrong PINs trigger a
 * {@link #LOCKOUT_DURATION_MS} cooldown measured on the monotonic clock, so
 * changing the wall clock cannot shorten it. The in-memory session flag dies
 * with the process, so a cold start always re-locks.
 */
public final class AppLockManager {

    private static final String PREFS = "akin_app_lock";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_PIN_HASH_V2 = "pin_hash_v2";
    private static final String KEY_PIN_ITER = "pin_iter";
    /** Legacy single-SHA-256 hash. Read for migration only, never written. */
    private static final String KEY_PIN_HASH_LEGACY = "pin_hash";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_UNTIL = "lockout_until";

    /** PBKDF2 work factor. Stored alongside the hash for future agility. */
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int HASH_BITS = 256;

    /** PIN length, attempt policy and background re-lock grace. */
    public static final int PIN_LENGTH = 4;
    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MS = 30_000L;
    public static final long SESSION_GRACE_MS = 60_000L;

    private static volatile boolean sessionUnlocked = false;

    private AppLockManager() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** True once the user has completed the first-run PIN setup. */
    public static boolean isPinSet(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        if (!p.getString(KEY_PIN_HASH_V2, "").isEmpty()
                && !p.getString(KEY_PIN_SALT, "").isEmpty()) {
            return true;
        }
        // Legacy installs: hash + salt pair with no v2 entry.
        return !p.getString(KEY_PIN_HASH_LEGACY, "").isEmpty()
                && !p.getString(KEY_PIN_SALT, "").isEmpty();
    }

    /** Persists a new PIN (hash + fresh salt) and clears attempt counters. */
    public static void setPin(@NonNull Context context, @NonNull String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        prefs(context).edit()
                .putString(KEY_PIN_SALT, saltB64)
                .putString(KEY_PIN_HASH_V2, pbkdf2(pin, salt, PBKDF2_ITERATIONS))
                .putInt(KEY_PIN_ITER, PBKDF2_ITERATIONS)
                .remove(KEY_PIN_HASH_LEGACY)
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKOUT_UNTIL)
                .apply();
    }

    /** Hash comparison — the raw PIN never touches disk. */
    public static boolean verifyPin(@NonNull Context context, @NonNull String pin) {
        SharedPreferences p = prefs(context);
        String saltB64 = p.getString(KEY_PIN_SALT, "");
        String v2 = p.getString(KEY_PIN_HASH_V2, "");
        if (!saltB64.isEmpty() && !v2.isEmpty()) {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            int iter = p.getInt(KEY_PIN_ITER, PBKDF2_ITERATIONS);
            return slowEquals(pbkdf2(pin, salt, iter), v2);
        }
        // Legacy single-SHA-256 entry: verify, then upgrade in place.
        String expected = p.getString(KEY_PIN_HASH_LEGACY, "");
        if (saltB64.isEmpty() || expected.isEmpty()) {
            return false;
        }
        boolean ok = slowEquals(legacyHash(pin, saltB64), expected);
        if (ok) {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            p.edit()
                    .putString(KEY_PIN_HASH_V2, pbkdf2(pin, salt, PBKDF2_ITERATIONS))
                    .putInt(KEY_PIN_ITER, PBKDF2_ITERATIONS)
                    .remove(KEY_PIN_HASH_LEGACY)
                    .apply();
        }
        return ok;
    }

    private static boolean slowEquals(@NonNull String a, @NonNull String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private static String pbkdf2(@NonNull String pin, @NonNull byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    pin.toCharArray(), salt, iterations, HASH_BITS);
            try {
                SecretKeyFactory factory =
                        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                byte[] digest = factory.generateSecret(spec).getEncoded();
                return Base64.encodeToString(digest, Base64.NO_WRAP);
            } finally {
                spec.clearPassword();
            }
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        }
    }

    /** Pre-PBKDF2 scheme. Verify-only for migration; never written. */
    private static String legacyHash(@NonNull String pin, @NonNull String saltB64) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltB64.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // ------------------------------------------------------------------
    // Biometrics (opt-in from Settings, capability-gated)
    // ------------------------------------------------------------------

    /** Device can actually do strong biometric auth (any API level). */
    public static boolean isBiometricAvailable(@NonNull Context context) {
        return BiometricManager.from(context.getApplicationContext())
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /** User toggle from Settings. Never true unless explicitly enabled. */
    public static boolean isBiometricEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public static void setBiometricEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    /** Usable on the lock screen only when opted in AND supported. */
    public static boolean canUseBiometric(@NonNull Context context) {
        return isBiometricEnabled(context) && isBiometricAvailable(context);
    }

    // ------------------------------------------------------------------
    // Brute-force protection (monotonic clock: wall-clock immune)
    // ------------------------------------------------------------------

    /**
     * Values predating the monotonic clock are wall-clock millis (order of
     * 1e12); any stamp further in the future than a fresh cooldown could
     * ever be is converted once to remaining monotonic time.
     */
    private static long lockoutUntil(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        long stored = p.getLong(KEY_LOCKOUT_UNTIL, 0);
        if (stored <= 0) {
            return 0;
        }
        long now = SystemClock.elapsedRealtime();
        if (stored > now + LOCKOUT_DURATION_MS * 2) {
            long remaining = stored - System.currentTimeMillis();
            long mono = remaining > 0 ? now + remaining : 0;
            p.edit().putLong(KEY_LOCKOUT_UNTIL, mono).apply();
            return mono;
        }
        return stored;
    }

    public static boolean isLockedOut(@NonNull Context context) {
        return SystemClock.elapsedRealtime() < lockoutUntil(context);
    }

    /** Seconds left on the cooldown, 0 when not locked out. */
    public static long lockoutRemainingSeconds(@NonNull Context context) {
        long left = lockoutUntil(context) - SystemClock.elapsedRealtime();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    /**
     * Records a wrong PIN. Returns attempts left before lockout, or -1 when
     * this failure just triggered the cooldown. Committed synchronously so
     * rapid attempts cannot under-count the counter.
     */
    public static int recordFailure(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        int attempts = p.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        if (attempts >= MAX_ATTEMPTS) {
            p.edit()
                    .remove(KEY_FAILED_ATTEMPTS)
                    .putLong(KEY_LOCKOUT_UNTIL,
                            SystemClock.elapsedRealtime() + LOCKOUT_DURATION_MS)
                    .commit();
            return -1;
        }
        p.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).commit();
        return MAX_ATTEMPTS - attempts;
    }

    public static void resetFailures(@NonNull Context context) {
        prefs(context).edit()
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKOUT_UNTIL)
                .apply();
    }

    // ------------------------------------------------------------------
    // Session (in-memory: cold start always re-locks)
    // ------------------------------------------------------------------

    public static boolean isSessionUnlocked() {
        return sessionUnlocked;
    }

    public static void setSessionUnlocked(boolean unlocked) {
        sessionUnlocked = unlocked;
    }
}
