package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.fragments.AddIncomeFragment;
import com.example.expensetracker.fragments.BalanceFragment;
import com.example.expensetracker.fragments.ExpenseFragment;
import com.example.expensetracker.fragments.HomeFragment;
import com.example.expensetracker.fragments.SettingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Date;


public class MainActivity extends AppCompatActivity {

    public Date sharedSelectedDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        applyStatusBarTheme();

        SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
        boolean dark = sp.getBoolean("dark_mode", false);

        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );


        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (sharedSelectedDate == null) {
            sharedSelectedDate = new Date();   // first time open → today
        }

// Default Home Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new HomeFragment())
                .commit();


        bottomNav.setOnItemSelectedListener(item -> {

            Fragment selected = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selected = new HomeFragment();

            } else if (id == R.id.nav_income) {
                selected = new AddIncomeFragment();

            } else if (id == R.id.nav_balance)
            {

                // Get HomeFragment current date from MainActivity memory
                Date selectedDate = sharedSelectedDate;   // you already store this earlier

                BalanceFragment frag = new BalanceFragment();
                Bundle b = new Bundle();
                b.putSerializable("selectedDate", selectedDate);
                frag.setArguments(b);

                selected = frag;
            }
            else if (id == R.id.nav_expense) {
                selected = new ExpenseFragment();

            } else if (id == R.id.nav_setting) {
                selected = new SettingFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, selected)
                    .commit();

            return true;
        });

    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {

        Fragment currentFragment =
                getSupportFragmentManager().findFragmentById(R.id.main_container);

        // If NOT in Home → go to Home
        if (!(currentFragment instanceof HomeFragment)) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new HomeFragment())
                    .commit();
            return;
        }

        // If already in Home → show exit dialog
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (d, w) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

private void applyStatusBarTheme()
{
    SharedPreferences sp = getSharedPreferences("settings", MODE_PRIVATE);
    boolean dark = sp.getBoolean("dark_mode", false);

    if (dark) {
        // Dark mode → light status bar icons
        getWindow().getDecorView().setSystemUiVisibility(0);
    } else {
        // Light mode → dark status bar icons
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );
    }
}

}