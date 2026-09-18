package com.akin.wallet.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Vault database key: a random 256-bit key, never derived from the 4-digit
 * app PIN (10k guesses would fall instantly to offline brute force). The key
 * is sealed with an AES-GCM key that lives in the Android Keystore and only
 * the sealed blob touches disk (private SharedPreferences). The unwrapped
 * key lives in process memory only and dies with the process.
 *
 * <p>Threat model, stated plainly: this encrypts the database file at rest,
 * so raw file reads (lost-device flash dumps, backup snooping, adb pulls)
 * yield ciphertext. It does not stop code running as the app on a rooted
 * device, which can ask the Keystore to unwrap just like we do.
 */
public final class DbKeyManager {

    private static final String PREFS = "akin_vault_key";
    private static final String KEY_WRAPPED = "wrapped_db_key";
    private static final String KEYSTORE_ALIAS = "akin_vault_db_key";
    private static final int RAW_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    /** Unwrapped hex passphrase, process memory only. Never persisted. */
    private static volatile char[] cachedPassphrase;

    private DbKeyManager() {
    }

    /**
     * Hex passphrase for SQLCipher. Creates and seals a fresh random key on
     * first run (or first run after the plaintext era); unwraps via Keystore
     * afterwards. Never returns null; throws instead of risking data loss —
     * callers must not catch-and-recreate, which would orphan the database.
     */
    @NonNull
    public static synchronized char[] getPassphrase(@NonNull Context context) {
        if (cachedPassphrase != null) {
            return cachedPassphrase;
        }
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String wrapped = prefs.getString(KEY_WRAPPED, "");
        byte[] raw;
        if (wrapped.isEmpty()) {
            raw = new byte[RAW_KEY_BYTES];
            new SecureRandom().nextBytes(raw);
            prefs.edit().putString(KEY_WRAPPED, seal(raw)).apply();
        } else {
            raw = unseal(wrapped);
        }
        cachedPassphrase = toHex(raw).toCharArray();
        // Best effort: drop the raw bytes as soon as the hex copy exists.
        java.util.Arrays.fill(raw, (byte) 0);
        return cachedPassphrase;
    }

    private static String seal(byte[] raw) {
        try {
            SecretKey key = keystoreKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(raw);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(iv, 0, iv.length);
            out.write(cipherText, 0, cipherText.length);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Vault key seal failed", e);
        }
    }

    private static byte[] unseal(String wrapped) {
        try {
            byte[] blob = Base64.decode(wrapped, Base64.NO_WRAP);
            if (blob.length <= GCM_IV_BYTES) {
                throw new IllegalStateException("Vault key blob truncated");
            }
            byte[] iv = java.util.Arrays.copyOfRange(blob, 0, GCM_IV_BYTES);
            byte[] cipherText = java.util.Arrays.copyOfRange(blob, GCM_IV_BYTES, blob.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(cipherText);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // Wrong key, tampered blob, or a Keystore that forgot the alias:
            // surface loudly. Recreating here would orphan the real database.
            throw new IllegalStateException("Vault key unreadable", e);
        }
    }

    private static SecretKey keystoreKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(KEYSTORE_ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            return kg.generateKey();
        }
        return (SecretKey) ks.getKey(KEYSTORE_ALIAS, null);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
