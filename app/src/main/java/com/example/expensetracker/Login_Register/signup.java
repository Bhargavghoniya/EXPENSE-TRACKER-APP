package com.example.expensetracker.Login_Register;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.expensetracker.BaseActivity;
import com.example.expensetracker.DatabaseHelper;
import com.example.expensetracker.R;

public class signup extends BaseActivity {

    EditText email, pass, confirm;
    TextView tvLogin;
    Button btnSignup;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        email = findViewById(R.id.username);
        pass = findViewById(R.id.signup_password);
        confirm = findViewById(R.id.signup_confirm);
        tvLogin = findViewById(R.id.loginRedirectText);
        btnSignup = findViewById(R.id.signup_button);

        db = new DatabaseHelper(this);

        // GO TO LOGIN
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(signup.this, login.class));
        });

        btnSignup.setOnClickListener(v -> {

            String u = email.getText().toString().trim();
            String p = pass.getText().toString().trim();
            String c = confirm.getText().toString().trim();

            if (u.isEmpty() || p.isEmpty() || c.isEmpty()) {
                Toast.makeText(signup.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!p.equals(c)) {
                Toast.makeText(signup.this, "Passwords not matching", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = db.registerUser(u, p);

            if (success) {
                Toast.makeText(signup.this, "Registered Successfully", Toast.LENGTH_SHORT).show();

                // Save username for autofill on login
                SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("username", u);
                editor.apply();

                startActivity(new Intent(signup.this, login.class));
                finish();
            } else {
                Toast.makeText(signup.this, "User already exists", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
