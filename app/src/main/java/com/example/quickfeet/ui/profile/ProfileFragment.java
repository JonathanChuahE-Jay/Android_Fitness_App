package com.example.quickfeet.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProfileFragment extends Fragment {

    private ConnectionClass connectionClass;
    private ImageView imgProfilePicture;
    private TextView tvName, tvEmail;
    private EditText workoutGoal;
    private Button btnEditProfile, updateBtn;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        imgProfilePicture = rootView.findViewById(R.id.imgProfilePicture);
        tvName = rootView.findViewById(R.id.tvName);
        tvEmail = rootView.findViewById(R.id.tvEmail);
        btnEditProfile = rootView.findViewById(R.id.btnEditProfile);
        workoutGoal = rootView.findViewById(R.id.workoutGoal);
        updateBtn = rootView.findViewById(R.id.updateBtn);

        if (imgProfilePicture == null || tvName == null || tvEmail == null || workoutGoal == null || updateBtn == null) {
            Log.e("ProfileFragment", "One or more views were not initialized properly.");
            return rootView;
        }

        connectionClass = new ConnectionClass();

        loadUserProfile();
        showWorkoutGoal();

        updateBtn.setOnClickListener(view -> updateWorkoutGoal());
        btnEditProfile.setOnClickListener(view -> {
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            startActivity(intent);
        });

        return rootView;
    }


    public void showWorkoutGoal() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Log.e("ProfileFragment", "User ID not found in SharedPreferences.");
            return;
        }

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String query = "SELECT daily_progress_goals FROM user_preferences WHERE user_id = ?";
                    PreparedStatement stmt = con.prepareStatement(query);
                    stmt.setInt(1, userId);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String currentGoal = rs.getString("daily_progress_goals");

                        requireActivity().runOnUiThread(() -> {
                            if (currentGoal != null && !currentGoal.isEmpty()) {
                                workoutGoal.setText(currentGoal);
                            } else {
                                workoutGoal.setText("0");
                            }
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            workoutGoal.setText("0");
                            Toast.makeText(getActivity(), "No goal found in the database.", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("ProfileFragment", "Error loading workout goal", e);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Error loading workout goal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    public void updateWorkoutGoal() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        String goalText = workoutGoal.getText().toString();
        int goal = goalText.isEmpty() ? 0 : Integer.parseInt(goalText);

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                String query = "UPDATE user_preferences SET daily_progress_goals = ? WHERE user_id = ?";
                PreparedStatement stmt = con.prepareStatement(query);
                stmt.setInt(1, goal);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), "Successfully updated workout goal", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e("ProfileFragment", "Error updating workout goal", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error updating workout goal: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    private void loadUserProfile() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(getActivity(), "User not logged in. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String query = "SELECT profile_picture, name, email FROM users WHERE id = ?";
                    PreparedStatement stmt = con.prepareStatement(query);
                    stmt.setInt(1, userId);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        String profilePictureBase64 = rs.getString("profile_picture");
                        String name = rs.getString("name");
                        String email = rs.getString("email");

                        requireActivity().runOnUiThread(() -> {
                            tvName.setText(name);
                            tvEmail.setText(email);

                            if (profilePictureBase64 != null && !profilePictureBase64.isEmpty()) {
                                try {
                                    byte[] decodedString = Base64.decode(profilePictureBase64, Base64.DEFAULT);
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    imgProfilePicture.setImageBitmap(decodedByte);
                                } catch (IllegalArgumentException e) {
                                    Log.e("ProfileFragment", "Invalid Base64 string for profile picture", e);
                                    imgProfilePicture.setImageResource(R.drawable.nav_profile);
                                }
                            } else {
                                imgProfilePicture.setImageResource(R.drawable.nav_profile);
                            }
                        });
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                            Log.d("ProfileFragment", "No data returned for user ID: " + userId);
                        });
                    }

                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("ProfileFragment", "Error loading profile", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error loading profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
