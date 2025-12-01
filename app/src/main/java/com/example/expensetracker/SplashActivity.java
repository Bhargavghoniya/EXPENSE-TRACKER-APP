package com.example.expensetracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.expensetracker.Login_Register.login;
import com.example.expensetracker.Login_Register.signup;

public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        SharedPreferences sp1 = getSharedPreferences("settings", MODE_PRIVATE);
//        boolean dark = sp1.getBoolean("dark_mode", false);
//
//        AppCompatDelegate.setDefaultNightMode(
//                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
//        );
//
//        // Set status bar text color
//        if (dark) {
//            // dark mode → white icons
//            getWindow().getDecorView().setSystemUiVisibility(0);
//        } else {
//            // light mode → dark icons
//            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new android.os.Handler().postDelayed(() -> {

            SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);

            String username = sp.getString("username", "");
            String password = sp.getString("password", "");

            if (username.isEmpty()) {
                // FIRST TIME → No user registered
                startActivity(new Intent(SplashActivity.this, login.class));
            }
            else if (!password.isEmpty()) {
                // User exists and already logged in before → Go to AppLock
                startActivity(new Intent(SplashActivity.this, AppLockActivity.class));
            }
            else {
                // Username exists but password not saved → Need login
                startActivity(new Intent(SplashActivity.this, login.class));
            }

            finish();

        }, 2000); // 2 seconds splash

    }
}
