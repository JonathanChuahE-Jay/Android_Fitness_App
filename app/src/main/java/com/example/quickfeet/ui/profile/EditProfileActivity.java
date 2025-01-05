package com.example.quickfeet.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.quickfeet.LoginActivity;
import com.example.quickfeet.MainActivity;
import com.example.quickfeet.R;
import com.example.quickfeet.ui.running.RunningHistoryActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mysql.jdbc.Connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.quickfeet.ConnectionClass;

public class EditProfileActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText editTextName;
    private EditText editTextEmail;
    private EditText editTextOldPassword;
    private EditText editTextNewPassword;
    private EditText editTextConfirmPassword;
    private ImageView profileImage;
    private Button btnSave, btnChangeImage;
    private Uri selectedImageUri;
    private ConnectionClass connectionClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Edit Profile");
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(EditProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        profileImage = findViewById(R.id.editProfileImage);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextOldPassword = findViewById(R.id.editTextOldPassword);
        editTextNewPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
        btnChangeImage = findViewById(R.id.btnChangeImage);

        connectionClass = new ConnectionClass();

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userName = prefs.getString("userName", "Default Name");
        String userEmail = prefs.getString("userEmail", "example@example.com");
        String profilePicBase64 = prefs.getString("userProfilePic", null);

        editTextName.setText(userName);
        editTextEmail.setText(userEmail);

        if (profilePicBase64 != null) {
            byte[] decodedString = Base64.decode(profilePicBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profileImage.setImageBitmap(decodedByte);
        }

        profileImage.setOnClickListener(v -> openImagePicker());
        btnChangeImage.setOnClickListener(v -> openImagePicker());

        editTextOldPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    editTextNewPassword.setVisibility(View.VISIBLE);
                    editTextConfirmPassword.setVisibility(View.VISIBLE);
                } else {
                    editTextNewPassword.setVisibility(View.GONE);
                    editTextConfirmPassword.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> handleUpdate());
    }

    private void handleUpdate() {
        String newUsername = editTextName.getText().toString();
        String newEmail = editTextEmail.getText().toString();
        String oldPassword = editTextOldPassword.getText().toString();
        String newPassword = editTextNewPassword.getText().toString();
        String confirmPassword = editTextConfirmPassword.getText().toString();

        if (newUsername.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Name and Email are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(newEmail)) {
            Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Connection con = connectionClass.CONN();
                if (con == null) {
                    runOnUiThread(() -> Toast.makeText(this, "Error connecting to database", Toast.LENGTH_SHORT).show());
                    return;
                }

                String currentEmail = prefs.getString("userEmail", "example@example.com");

                String currentPassword = null;
                String fetchPasswordQuery = "SELECT password FROM users WHERE email = ?";
                PreparedStatement fetchPasswordStmt = con.prepareStatement(fetchPasswordQuery);
                fetchPasswordStmt.setString(1, currentEmail);
                ResultSet rs = fetchPasswordStmt.executeQuery();
                if (rs.next()) {
                    currentPassword = rs.getString("password");
                }

                if (!oldPassword.isEmpty()) {
                    if (!oldPassword.equals(currentPassword)) {
                        runOnUiThread(() -> Toast.makeText(this, "Old password is incorrect.", Toast.LENGTH_SHORT).show());
                        con.close();
                        return;
                    }
                    if (!newPassword.equals(confirmPassword)) {
                        runOnUiThread(() -> Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show());
                        con.close();
                        return;
                    }
                    currentPassword = newPassword;
                }

                String updateQuery = "UPDATE users SET name = ?, email = ?, password = ?, profile_picture = ? WHERE email = ?";
                PreparedStatement updateStmt = con.prepareStatement(updateQuery);
                updateStmt.setString(1, newUsername);
                updateStmt.setString(2, newEmail);
                updateStmt.setString(3, currentPassword);

                String profilePicBase64 = selectedImageUri != null
                        ? resizeAndConvertImageToBase64(selectedImageUri)
                        : prefs.getString("userProfilePic", null);
                updateStmt.setString(4, profilePicBase64);
                updateStmt.setString(5, currentEmail);

                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("userName", newUsername);
                        editor.putString("userEmail", newEmail);
                        if (selectedImageUri != null) {
                            editor.putString("userProfilePic", profilePicBase64);
                        }
                        editor.apply();

                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show());
                }

                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    private String resizeAndConvertImageToBase64(Uri imageUri) {
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
            profileImage.setImageURI(selectedImageUri);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
