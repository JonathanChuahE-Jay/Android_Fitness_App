package com.example.quickfeet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnRegister;
    private ConnectionClass connectionClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        connectionClass = new ConnectionClass();

        // Handle Login Button Click
        btnLogin.setOnClickListener(v -> handleLogin());

        // Navigate to Registration Activity
        btnRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void handleLogin() {
        String email = edtEmail.getText().toString();
        String password = edtPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Email and password are required", Toast.LENGTH_SHORT).show();
        } else {
            new Thread(() -> {
                try {
                    Connection con = connectionClass.CONN();
                    if (con == null) {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error connecting to database", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    String query = "SELECT * FROM users WHERE email = ? AND password = ?";
                    java.sql.PreparedStatement stmt = con.prepareStatement(query);
                    stmt.setString(1, email);
                    stmt.setString(2, password);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String userName = rs.getString("name");
                        String userEmail = rs.getString("email");
                        String profilePicBase64 = rs.getString("profile_picture");

                        // Store login status and user data in SharedPreferences
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("isLoggedIn", true);
                        editor.putInt("userId", userId);
                        editor.putString("userName", userName);
                        editor.putString("userEmail", userEmail);
                        editor.putString("userProfilePic", profilePicBase64);
                        editor.apply();

                        // Navigate to MainActivity after successful login
                        runOnUiThread(() -> {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show());
                    }

                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}
