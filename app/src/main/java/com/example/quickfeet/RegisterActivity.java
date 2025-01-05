package com.example.quickfeet;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mysql.jdbc.Connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtNewUsername, edtNewEmail, edtNewPassword, edtConfirmPassword;
    private Button btnRegister, btnUploadProfilePic;
    private TextView tvErrorMessage;
    private ImageView imgProfilePicture;
    private Uri selectedImageUri;
    private ConnectionClass connectionClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtNewUsername = findViewById(R.id.edtNewUsername);
        edtNewEmail = findViewById(R.id.edtNewEmail);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnUploadProfilePic = findViewById(R.id.btnUploadProfilePic);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        imgProfilePicture = findViewById(R.id.imgProfilePicture);

        connectionClass = new ConnectionClass();

        btnRegister.setOnClickListener(v -> handleRegistration());
        btnUploadProfilePic.setOnClickListener(v -> openImagePicker());
    }

    private void handleRegistration() {
        String newUsername = edtNewUsername.getText().toString();
        String newEmail = edtNewEmail.getText().toString();
        String newPassword = edtNewPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (newUsername.isEmpty() || newEmail.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            tvErrorMessage.setVisibility(View.VISIBLE);
            tvErrorMessage.setText("All fields are required.");
        } else if (!newPassword.equals(confirmPassword)) {
            tvErrorMessage.setVisibility(View.VISIBLE);
            tvErrorMessage.setText("Passwords do not match.");
        } else if (!isValidEmail(newEmail)) {
            tvErrorMessage.setVisibility(View.VISIBLE);
            tvErrorMessage.setText("Invalid email format.");
        } else {
            new Thread(() -> {
                try {
                    Connection con = connectionClass.CONN();
                    if (con == null) {
                        runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error connecting to database", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    String queryCheck = "SELECT * FROM users WHERE email = ?";
                    PreparedStatement stmtCheck = con.prepareStatement(queryCheck);
                    stmtCheck.setString(1, newEmail);
                    ResultSet rs = stmtCheck.executeQuery();

                    if (rs.next()) {
                        runOnUiThread(() -> {
                            tvErrorMessage.setVisibility(View.VISIBLE);
                            tvErrorMessage.setText("Email already registered.");
                        });
                        con.close();
                        return;
                    }

                    String queryInsert = "INSERT INTO users (name, email, password, profile_picture) VALUES (?, ?, ?, ?)";
                    PreparedStatement stmtInsert = con.prepareStatement(queryInsert, PreparedStatement.RETURN_GENERATED_KEYS);
                    stmtInsert.setString(1, newUsername);
                    stmtInsert.setString(2, newEmail);
                    stmtInsert.setString(3, newPassword);

                    if (selectedImageUri != null) {
                        String profilePic = convertImageToBase64(selectedImageUri);
                        stmtInsert.setString(4, profilePic);
                    } else {
                        stmtInsert.setNull(4, java.sql.Types.NULL);
                    }

                    int rowsAffected = stmtInsert.executeUpdate();

                    if (rowsAffected > 0) {
                        ResultSet generatedKeys = stmtInsert.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            int userId = generatedKeys.getInt(1);

                            String insertPreferencesQuery = "INSERT INTO user_preferences (user_id) VALUES (?)";
                            PreparedStatement stmtInsertPreferences = con.prepareStatement(insertPreferencesQuery);
                            stmtInsertPreferences.setInt(1, userId);
                            stmtInsertPreferences.executeUpdate();
                        }
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });

                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private String convertImageToBase64(Uri imageUri) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imgProfilePicture.setImageURI(selectedImageUri);
        }
    }
}
