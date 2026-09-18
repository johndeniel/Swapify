package com.akin.wallet.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * App-lock vault: 4-digit PIN setup/verification plus opt-in biometric
 * unlock. The PIN itself is never stored — only a salted SHA-256 hash in
 * private SharedPreferences. Biometrics stay disabled until the user turns
 * them on in Settings (and the device actually supports them).
 *
 * <p>Brute-force protection: {@link #MAX_ATTEMPTS} wrong PINs trigger a
 * {@link #LOCKOUT_DURATION_MS} cooldown. The in-memory session flag dies
 * with the process, so a cold start always re-locks.
 */
public final class AppLockManager {

    private static final String PREFS = "akin_app_lock";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_UNTIL = "lockout_until";

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
        return !p.getString(KEY_PIN_HASH, "").isEmpty()
                && !p.getString(KEY_PIN_SALT, "").isEmpty();
    }

    /** Persists a new PIN (hash + fresh salt) and clears attempt counters. */
    public static void setPin(@NonNull Context context, @NonNull String pin) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        prefs(context).edit()
                .putString(KEY_PIN_SALT, saltB64)
                .putString(KEY_PIN_HASH, hash(pin, saltB64))
                .remove(KEY_FAILED_ATTEMPTS)
                .remove(KEY_LOCKOUT_UNTIL)
                .apply();
    }

    /** Hash comparison — the raw PIN never touches disk. */
    public static boolean verifyPin(@NonNull Context context, @NonNull String pin) {
        SharedPreferences p = prefs(context);
        String salt = p.getString(KEY_PIN_SALT, "");
        String expected = p.getString(KEY_PIN_HASH, "");
        if (salt.isEmpty() || expected.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(pin, salt).getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(@NonNull String pin, @NonNull String saltB64) {
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
    // Brute-force protection
    // ------------------------------------------------------------------

    public static boolean isLockedOut(@NonNull Context context) {
        return System.currentTimeMillis() < prefs(context).getLong(KEY_LOCKOUT_UNTIL, 0);
    }

    /** Seconds left on the cooldown, 0 when not locked out. */
    public static long lockoutRemainingSeconds(@NonNull Context context) {
        long left = prefs(context).getLong(KEY_LOCKOUT_UNTIL, 0) - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    /**
     * Records a wrong PIN. Returns attempts left before lockout, or -1 when
     * this failure just triggered the cooldown.
     */
    public static int recordFailure(@NonNull Context context) {
        SharedPreferences p = prefs(context);
        int attempts = p.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        if (attempts >= MAX_ATTEMPTS) {
            p.edit()
                    .remove(KEY_FAILED_ATTEMPTS)
                    .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                    .apply();
            return -1;
        }
        p.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply();
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
