package com.example.expensetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class AppLockActivity extends BaseActivity {

    TextView txtError, txtLogout;
    ImageView txtFingerprint;
    ImageView pin1, pin2, pin3, pin4;

    StringBuilder typedPIN = new StringBuilder();
    String savedPIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        txtError = findViewById(R.id.txtError);
        pin1 = findViewById(R.id.pin1);
        pin2 = findViewById(R.id.pin2);
        pin3 = findViewById(R.id.pin3);
        pin4 = findViewById(R.id.pin4);

        txtLogout = findViewById(R.id.txtLogout);          // 🔵 Added logout button
        txtFingerprint = findViewById(R.id.txtFingerprint); // 🔵 Text to trigger fingerprint

        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        savedPIN = sp.getString("password", "");

        SharedPreferences spSettings = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isFingerprintEnabled = spSettings.getBoolean("fingerprint_enabled", false);

// if fingerprint disabled → do not show fingerprint icon
        if (!isFingerprintEnabled) {
            findViewById(R.id.txtFingerprint).setVisibility(View.GONE);
        } else {
            startFingerprintAuth(); // only if enabled
        }


        setupKeypad();
        setupFingerprint();
        setupLogout();
    }
    private void startFingerprintAuth() {

        BiometricPrompt biometricPrompt;
        BiometricPrompt.PromptInfo promptInfo;

        biometricPrompt = new BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {

                        startActivity(new Intent(AppLockActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(AppLockActivity.this,
                                "Fingerprint not recognized", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock App")
                .setDescription("Use your fingerprint to unlock")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }



    // 🔐 BIOMETRIC / FINGERPRINT LOGIN
    private void setupFingerprint() {

        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                AppLockActivity.this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(
                            BiometricPrompt.AuthenticationResult result) {

                        startActivity(new Intent(AppLockActivity.this, MainActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        txtError.setText("Fingerprint not recognized");
                    }
                }
        );

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock App")
                .setDescription("Use your fingerprint")
                .setNegativeButtonText("Cancel")
                .build();

        // Click on fingerprint text
        txtFingerprint.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));
    }

    // 🔴 LOGOUT
    private void setupLogout() {
        txtLogout.setOnClickListener(v -> {

            SharedPreferences.Editor editor =
                    getSharedPreferences("user", MODE_PRIVATE).edit();

            editor.clear();
            editor.apply();

            startActivity(new Intent(AppLockActivity.this,
                    com.example.expensetracker.Login_Register.login.class));

            finish();
        });
    }

    // 🔢 KEYPAD SETUP
    private void setupKeypad() {

        int[] nums = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        View.OnClickListener numberClick = v -> {
            if (typedPIN.length() >= 4) return;

            String value = v.getTag().toString();
            typedPIN.append(value);

            updatePins();
            checkPIN();
        };

        for (int id : nums) findViewById(id).setOnClickListener(numberClick);

        // DELETE KEY
        findViewById(R.id.btnDel).setOnClickListener(v -> {
            if (typedPIN.length() > 0) {
                typedPIN.deleteCharAt(typedPIN.length() - 1);
                updatePins();
            }
        });
    }

    // 🔵 Update PIN Circles
    private void updatePins() {
        int len = typedPIN.length();

        pin1.setImageResource(len >= 1 ? R.drawable.pin_filled : R.drawable.pin_empty);
        pin2.setImageResource(len >= 2 ? R.drawable.pin_filled : R.drawable.pin_empty);
        pin3.setImageResource(len >= 3 ? R.drawable.pin_filled : R.drawable.pin_empty);
        pin4.setImageResource(len == 4 ? R.drawable.pin_filled : R.drawable.pin_empty);
    }

    // ✔ Check PIN
    private void checkPIN() {
        if (typedPIN.length() == 4) {

            if (typedPIN.toString().equals(savedPIN)) {

                txtError.setText("");
                startActivity(new Intent(AppLockActivity.this, MainActivity.class));
                finish();

            } else {

                txtError.setText("Incorrect PIN");

                // Shake animation
                pin1.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                pin2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                pin3.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
                pin4.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));

                vibratePhone();

                typedPIN.setLength(0);
                updatePins();
            }
        }
    }

    // 📳 Vibrate
    private void vibratePhone() {
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) v.vibrate(150);
    }
}
