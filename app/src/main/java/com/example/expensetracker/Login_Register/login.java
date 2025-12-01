package com.example.expensetracker.Login_Register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.expensetracker.BaseActivity;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.R;

public class login extends BaseActivity {

    EditText loginUsername, loginPassword;
    TextView tvSignup;
    Button btnLogin;

    DatabaseHelper db;
    SharedPreferences sp;   // <-- important

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        boolean dark = getSharedPreferences("settings", MODE_PRIVATE)
                .getBoolean("dark_mode", false);

        EditText user = findViewById(R.id.login_username);
        EditText pass = findViewById(R.id.login_password);

        if (dark) {
            user.setTextColor(Color.BLACK);
            pass.setTextColor(Color.BLACK);
        } else {
            user.setTextColor(Color.BLACK);
            pass.setTextColor(Color.BLACK);
        }

        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);
        tvSignup = findViewById(R.id.signupRedirectText);
        btnLogin = findViewById(R.id.login_button);

        db = new DatabaseHelper(this);

        // SESSION
        sp = getSharedPreferences("user", MODE_PRIVATE);

        // Auto-fill username
        loginUsername.setText(sp.getString("username", ""));

        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(login.this, signup.class));
        });

        // LOGIN BUTTON
        btnLogin.setOnClickListener(v -> {

            String u = loginUsername.getText().toString().trim();
            String p = loginPassword.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(login.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean ok = db.checkUser(u, p);

            if (ok) {
                Toast.makeText(login.this, "Login Successful", Toast.LENGTH_SHORT).show();

                // SAVE LOGIN SESSION
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("username", u);
                editor.putString("password", p);
                editor.apply();

                startActivity(new Intent(login.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
