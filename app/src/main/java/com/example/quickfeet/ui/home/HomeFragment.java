package com.example.quickfeet.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.R;
import com.example.quickfeet.ui.workout_manager.WorkoutManagerFragment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class HomeFragment extends Fragment {
    private TextView textUserName;
    private TextView textProgress;
    private ProgressBar progressBar;

    private ConnectionClass connectionClass;
    private int progress = 0;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        connectionClass = new ConnectionClass();
        textUserName = rootView.findViewById(R.id.text_user_name);
        textProgress = rootView.findViewById(R.id.text_progress);
        progressBar = rootView.findViewById(R.id.progressBar);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        String userName = prefs.getString("userName", "User");
        int userId = prefs.getInt("userId", -1);

        progress = prefs.getInt("todaysProgress", 0);
        textUserName.setText(userName);
        textProgress.setText("Today's progress: " + progress + "%");
        progressBar.setProgress(progress);


        getProgress(userId);

        return rootView;
    }
    public void getProgress(int userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        java.sql.Date startDate = new java.sql.Date(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        java.sql.Date endDate = new java.sql.Date(calendar.getTimeInMillis());

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String preferenceQuery = "SELECT DAYOFWEEK(created_at) AS day, COUNT(*) AS workout_count " +
                            "FROM workout_logs WHERE user_id = ? AND created_at BETWEEN ? AND ? " +
                            "GROUP BY DAYOFWEEK(created_at)";
                    try (PreparedStatement stmt = con.prepareStatement(preferenceQuery)) {
                        stmt.setInt(1, userId);
                        stmt.setDate(2, startDate);
                        stmt.setDate(3, endDate);

                        try (ResultSet rs = stmt.executeQuery()) {
                            int todaysActualProgress = 0;
                            while (rs.next()) {
                                int workoutCount = rs.getInt("workout_count");
                                int day = rs.getInt("day");
                                if (day == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                                    todaysActualProgress = workoutCount;
                                }
                            }

                            String goalQuery = "SELECT daily_progress_goals FROM user_preferences WHERE user_id = ?";
                            try (PreparedStatement goalStmt = con.prepareStatement(goalQuery)) {
                                goalStmt.setInt(1, userId);
                                try (ResultSet goalRs = goalStmt.executeQuery()) {
                                    if (goalRs.next()) {
                                        int dailyProgressGoals = goalRs.getInt("daily_progress_goals");

                                        if (dailyProgressGoals > 0) {
                                            int newProgress = (int) ((todaysActualProgress / (float) dailyProgressGoals) * 100);

                                            requireActivity().runOnUiThread(() -> {
                                                textProgress.setText("Today's progress: " + newProgress + "%");
                                                progressBar.setProgress(newProgress);
                                            });
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
