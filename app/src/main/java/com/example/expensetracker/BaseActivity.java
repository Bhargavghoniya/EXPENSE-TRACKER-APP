package com.example.expensetracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        applyAppTheme();   // Theme applied BEFORE UI loads

        super.onCreate(savedInstanceState);

        applyStatusBar();   // Status bar AFTER window is created
    }

    private void applyAppTheme() {
        SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = sp.getBoolean("dark_mode", false);

        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void applyStatusBar() {
        SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = sp.getBoolean("dark_mode", false);

        if (dark) {
            // Dark theme → white icons
            getWindow().getDecorView().setSystemUiVisibility(0);
        } else {
            // Light theme → dark icons
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }
    }
}
