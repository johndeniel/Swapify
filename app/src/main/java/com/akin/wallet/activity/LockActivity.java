package com.akin.wallet.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.akin.wallet.R;
import com.akin.wallet.security.AppLockManager;

/**
 * App lock — the launcher screen. First run walks 4-digit PIN setup
 * (create + confirm); afterwards it is PIN entry or, only when the user
 * opted in from Settings, fingerprint unlock. Result taps reuse the same
 * unlock-success routing per launch mode:
 *
 * <ul>
 *   <li>{@link #MODE_START} (launcher): success opens MainActivity.</li>
 *   <li>{@link #MODE_VERIFY}: success just returns RESULT_OK.</li>
 *   <li>{@link #MODE_CHANGE}: verifies the current PIN, then sets a new one.</li>
 * </ul>
 *
 * <p>Secrets policy: password / PIN / CVV are never searchable here — this
 * screen only ever handles the 4-digit app PIN, verified as a salted hash.
 */
public class LockActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "extra_mode";
    public static final String MODE_START = "start";
    public static final String MODE_VERIFY = "verify";
    public static final String MODE_CHANGE = "change";

    private enum Screen { CREATE, CONFIRM, VERIFY, PIN }

    private String mode = MODE_START;
    private Screen screen = Screen.PIN;

    private final StringBuilder entry = new StringBuilder();
    private String firstPin;
    private boolean submitting;

    private TextView error;
    private TextView tagline;
    private View dotsRow;
    private final View[] dots = new View[AppLockManager.PIN_LENGTH];
    private LinearLayout keypad;
    private View bioKey;

    private BiometricPrompt biometricPrompt;
    private boolean promptActive;
    private boolean paused;

    private final Handler lockoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable lockoutTicker = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            if (AppLockManager.isLockedOut(LockActivity.this)) {
                showLockout();
                lockoutHandler.postDelayed(this, 1000);
            } else {
                clearError();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        String extra = getIntent().getStringExtra(EXTRA_MODE);
        if (extra != null) {
            mode = extra;
        }

        error = findViewById(R.id.lock_error);
        tagline = findViewById(R.id.lock_tagline);
        dotsRow = findViewById(R.id.dots_row);
        keypad = findViewById(R.id.keypad);
        bioKey = findViewById(R.id.key_bio);
        int[] dotIds = {R.id.dot_0, R.id.dot_1, R.id.dot_2, R.id.dot_3};
        for (int i = 0; i < dotIds.length; i++) {
            dots[i] = findViewById(dotIds[i]);
        }

        wireKeypad();
        // Keypad fingerprint key — same design, just asks the system
        // prompt. The keypad never switches screens.
        bioKey.setOnClickListener(v -> startBiometric());

        biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this), biometricCallback());

        if (!AppLockManager.isPinSet(this)) {
            showCreateScreen();
        } else if (MODE_CHANGE.equals(mode)) {
            showVerifyScreen();
        } else {
            showPinScreen(null);
            // Biometric available: ask via system prompt over the same keypad.
            if (AppLockManager.canUseBiometric(this)) {
                keypad.postDelayed(this::startBiometric, 400);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        // System-cancelled prompts (backgrounding) re-ask automatically,
        // design stays on the keypad.
        if (screen == Screen.PIN
                && AppLockManager.canUseBiometric(this)
                && !AppLockManager.isLockedOut(this)
                && !MODE_CHANGE.equals(mode)
                && AppLockManager.isPinSet(this)) {
            startBiometric();
        }
    }

    @Override
    protected void onPause() {
        paused = true;
        cancelBiometric();
        lockoutHandler.removeCallbacks(lockoutTicker);
        super.onPause();
    }

    // ------------------------------------------------------------------
    // Screens
    // ------------------------------------------------------------------

    private void showCreateScreen() {
        screen = Screen.CREATE;
        firstPin = null;
        entry.setLength(0);
        submitting = false;
        if (MODE_CHANGE.equals(mode)) {
            setStepText(getString(R.string.lock_step_new_pin));
        } else {
            setStepText(getString(R.string.lock_sub_create));
        }
        showKeypadMode();
        clearError();
        setBioKeyVisible(false);
        renderDots();
    }

    private void showConfirmScreen() {
        screen = Screen.CONFIRM;
        entry.setLength(0);
        submitting = false;
        if (MODE_CHANGE.equals(mode)) {
            setStepText(getString(R.string.lock_step_confirm_pin));
        } else {
            setStepText(getString(R.string.lock_sub_confirm));
        }
        showKeypadMode();
        setBioKeyVisible(false);
        renderDots();
    }

    /** Change-PIN step 1: prove the current PIN before setting a new one. */
    private void showVerifyScreen() {
        screen = Screen.VERIFY;
        entry.setLength(0);
        submitting = false;
        setStepText(getString(R.string.lock_step_old_pin));
        showKeypadMode();
        clearError();
        setBioKeyVisible(false);
        renderDots();
    }

    private void showPinScreen(String message) {
        screen = Screen.PIN;
        entry.setLength(0);
        submitting = false;
        cancelBiometric();
        resetTagline();
        showKeypadMode();
        if (message != null) {
            showError(message);
        } else {
            clearError();
        }
        setBioKeyVisible(AppLockManager.canUseBiometric(this) && !MODE_CHANGE.equals(mode));
        renderDots();
        if (AppLockManager.isLockedOut(this)) {
            showLockout();
        }
    }

    private void showKeypadMode() {
        keypad.setVisibility(View.VISIBLE);
        dotsRow.setVisibility(View.VISIBLE);
    }

    /** Keypad biometric key left of 0 — INVISIBLE (not GONE) to keep 0 centered. */
    private void setBioKeyVisible(boolean visible) {
        if (bioKey != null) {
            bioKey.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /** Tagline slot doubles as the step helper during setup / update. */
    private void setStepText(String text) {
        tagline.setText(text);
        tagline.setLetterSpacing(0.02f);
    }

    private void resetTagline() {
        tagline.setText(R.string.header_tagline);
        tagline.setLetterSpacing(0.22f);
    }

    // ------------------------------------------------------------------
    // Keypad input
    // ------------------------------------------------------------------

    private void wireKeypad() {
        int[] keyIds = {R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
                R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0};
        for (int i = 0; i < keyIds.length; i++) {
            // i = 0..8 -> '1'..'9', i = 9 -> '0'.
            final char d = i == 9 ? '0' : (char) ('1' + i);
            findViewById(keyIds[i]).setOnClickListener(v -> onDigit(d));
        }
        findViewById(R.id.key_back).setOnClickListener(v -> onBackspace());
    }

    private void onDigit(char digit) {
        if (submitting) {
            return;
        }
        if (AppLockManager.isLockedOut(this)) {
            showLockout();
            return;
        }
        if (entry.length() >= AppLockManager.PIN_LENGTH) {
            return;
        }
        clearError();
        entry.append(digit);
        renderDots();
        if (entry.length() == AppLockManager.PIN_LENGTH) {
            submitting = true;
            dotsRow.postDelayed(this::processEntry, 120);
        }
    }

    private void onBackspace() {
        if (submitting || entry.length() == 0) {
            return;
        }
        entry.deleteCharAt(entry.length() - 1);
        renderDots();
    }

    private void processEntry() {
        String pin = entry.toString();
        entry.setLength(0);
        switch (screen) {
            case CREATE:
                firstPin = pin;
                showConfirmScreen();
                break;
            case CONFIRM:
                if (pin.equals(firstPin)) {
                    AppLockManager.setPin(this, pin);
                    unlockSuccess();
                } else {
                    firstPin = null;
                    showCreateScreen();
                    showError(getString(R.string.lock_error_mismatch));
                    shakeDots();
                }
                break;
            case VERIFY:
            case PIN:
            default:
                if (AppLockManager.isLockedOut(this)) {
                    showLockout();
                } else if (AppLockManager.verifyPin(this, pin)) {
                    AppLockManager.resetFailures(this);
                    if (screen == Screen.VERIFY) {
                        showCreateScreen();
                    } else {
                        unlockSuccess();
                    }
                } else {
                    int left = AppLockManager.recordFailure(this);
                    if (left < 0) {
                        showLockout();
                    } else {
                        showError(getString(R.string.lock_error_wrong, left));
                        shakeDots();
                    }
                }
                break;
        }
        submitting = false;
        renderDots();
    }

    private void unlockSuccess() {
        AppLockManager.setSessionUnlocked(true);
        cancelBiometric();
        if (MODE_CHANGE.equals(mode)) {
            Toast.makeText(this, R.string.lock_pin_updated, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else if (MODE_VERIFY.equals(mode)) {
            setResult(RESULT_OK);
            finish();
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    // ------------------------------------------------------------------
    // Biometrics (system prompt; only reachable when opted in + supported)
    // ------------------------------------------------------------------

    private void startBiometric() {
        if (promptActive || isFinishing() || !AppLockManager.canUseBiometric(this)) {
            return;
        }
        if (AppLockManager.isLockedOut(this)) {
            return;
        }
        // Only auto-ask on plain PIN unlock — never during setup / change.
        if (screen != Screen.PIN || MODE_CHANGE.equals(mode)) {
            return;
        }
        promptActive = true;
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setSubtitle(getString(R.string.lock_sub_enter))
                .setNegativeButtonText(getString(R.string.lock_use_pin))
                .build();
        biometricPrompt.authenticate(info);
    }

    private void cancelBiometric() {
        promptActive = false;
        if (biometricPrompt != null) {
            try {
                biometricPrompt.cancelAuthentication();
            } catch (Exception ignored) {
            }
        }
    }

    private BiometricPrompt.AuthenticationCallback biometricCallback() {
        return new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                promptActive = false;
                if (!isFinishing() && !paused) {
                    AppLockManager.resetFailures(LockActivity.this);
                    unlockSuccess();
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                promptActive = false;
                if (isFinishing() || paused) {
                    return;
                }
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                        || errorCode == BiometricPrompt.ERROR_CANCELED
                        || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    // User dismissed — stay on the same keypad design.
                    return;
                } else {
                    showError(getString(R.string.lock_error_fingerprint));
                }
            }

            @Override
            public void onAuthenticationFailed() {
                if (isFinishing() || paused) {
                    return;
                }
                showError(getString(R.string.lock_error_fingerprint));
            }
        };
    }

    // ------------------------------------------------------------------
    // Small visuals
    // ------------------------------------------------------------------

    private void renderDots() {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundResource(i < entry.length()
                    ? R.drawable.bg_pin_dot_filled : R.drawable.bg_pin_dot_empty);
        }
    }

    private void showError(String message) {
        error.setText(message);
    }

    private void clearError() {
        error.setText("");
    }

    private void showLockout() {
        long seconds = AppLockManager.lockoutRemainingSeconds(this);
        showError(getString(R.string.lock_error_locked, Math.max(seconds, 1)));
        lockoutHandler.removeCallbacks(lockoutTicker);
        lockoutHandler.postDelayed(lockoutTicker, 1000);
    }

    private void shakeDots() {
        TranslateAnimation shake = new TranslateAnimation(0, 12, 0, 0);
        shake.setDuration(45);
        shake.setRepeatCount(5);
        shake.setRepeatMode(TranslateAnimation.REVERSE);
        dotsRow.startAnimation(shake);
    }
}
