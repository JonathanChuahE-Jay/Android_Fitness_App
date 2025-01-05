package com.example.quickfeet.ui.share;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class ShareFragment extends Fragment {
    private boolean showWeeks, showMonths;
    private boolean showModal = true;
    private Button btnWeeks, btnMonths, btnShare;
    private ProgressBar progressMonday, progressTuesday, progressWednesday, progressThursday, progressFriday, progressSaturday, progressSunday;
    private TextView tvProgressPeriod, tvRunningProgressTitle;
    private TextView progressMondayPercentage, progressTuesdayPercentage, progressWednesdayPercentage,
            progressThursdayPercentage, progressFridayPercentage, progressSaturdayPercentage, progressSundayPercentage;

    private ConnectionClass connectionClass;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_share, container, false);
        tvProgressPeriod = view.findViewById(R.id.tvProgressPeriod);

        int weekOfMonth = getCurrentWeekOfMonth();
        tvProgressPeriod.setText("Week of the Month: " + weekOfMonth);

        connectionClass = new ConnectionClass();
        btnWeeks = view.findViewById(R.id.btnWeeks);
        btnMonths = view.findViewById(R.id.btnMonths);
        btnShare = view.findViewById(R.id.btnShare);
        progressMonday = view.findViewById(R.id.progressMonday);
        progressTuesday = view.findViewById(R.id.progressTuesday);
        progressWednesday = view.findViewById(R.id.progressWednesday);
        progressThursday = view.findViewById(R.id.progressThursday);
        progressFriday = view.findViewById(R.id.progressFriday);
        progressSaturday = view.findViewById(R.id.progressSaturday);
        progressSunday = view.findViewById(R.id.progressSunday);

        progressMondayPercentage = view.findViewById(R.id.progressMondayPercentage);
        progressTuesdayPercentage = view.findViewById(R.id.progressTuesdayPercentage);
        progressWednesdayPercentage = view.findViewById(R.id.progressWednesdayPercentage);
        progressThursdayPercentage = view.findViewById(R.id.progressThursdayPercentage);
        progressFridayPercentage = view.findViewById(R.id.progressFridayPercentage);
        progressSaturdayPercentage = view.findViewById(R.id.progressSaturdayPercentage);
        progressSundayPercentage = view.findViewById(R.id.progressSundayPercentage);

        tvRunningProgressTitle = view.findViewById(R.id.tvRunningProgressTitle);

        btnWeeks.setOnClickListener(v -> {
            showWeeks = true;
            showMonths = false;
            Toast.makeText(getContext(), "Weeks selected", Toast.LENGTH_SHORT).show();
            checkUserPreferences();

            view.findViewById(R.id.monthsScrollView).setVisibility(View.GONE);
            view.findViewById(R.id.monthsLayout).setVisibility(View.GONE);
        });

        btnMonths.setOnClickListener(v -> {
            showMonths = true;
            showWeeks = false;
            Toast.makeText(getContext(), "Months selected", Toast.LENGTH_SHORT).show();
            checkUserPreferences();

            view.findViewById(R.id.monthsScrollView).setVisibility(View.VISIBLE);
            view.findViewById(R.id.monthsLayout).setVisibility(View.VISIBLE);
        });
        btnShare.setOnClickListener(v -> {
            String progressData = "Your weekly progress: \n" +
                    "Monday: " + progressMonday.getProgress() + "%\n" +
                    "Tuesday: " + progressTuesday.getProgress() + "%\n" +
                    "Wednesday: " + progressWednesday.getProgress() + "%\n" +
                    "Thursday: " + progressThursday.getProgress() + "%\n" +
                    "Friday: " + progressFriday.getProgress() + "%\n" +
                    "Saturday: " + progressSaturday.getProgress() + "%\n" +
                    "Sunday: " + progressSunday.getProgress() + "%\n";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, progressData);

            if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(Intent.createChooser(shareIntent, "Share progress"));
            } else {
                Toast.makeText(getContext(), "No apps available to share", Toast.LENGTH_SHORT).show();
            }
        });

        checkUserPreferences();
        return view;

    }
    private int getCurrentWeekOfMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.WEEK_OF_MONTH);
    }
    public void checkUserPreferences() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String preferenceQuery = "SELECT daily_progress_goals FROM user_preferences WHERE user_id = ?";
                    try (PreparedStatement stmt = con.prepareStatement(preferenceQuery)) {
                        stmt.setInt(1, userId);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int dailyProgressGoals = rs.getInt("daily_progress_goals");
                                if(dailyProgressGoals > 0){
                                    showModal = false;
                                    requireActivity().runOnUiThread(() -> {
                                        TextView tvRunningProgressTitle = requireView().findViewById(R.id.tvRunningProgressTitle);
                                        tvRunningProgressTitle.setText("Your Running Progress \n(" + dailyProgressGoals + " workout per day)");
                                    });
                                    if(showWeeks){
                                        getWeeklyData(dailyProgressGoals, userId);
                                    }else if(showMonths){
                                        getMonthlyData(dailyProgressGoals, userId);
                                    }else{
                                        getWeeklyData(dailyProgressGoals, userId);
                                    }
                                }else{
                                    if(showModal == true){
                                        showModalForUserInput();
                                    }
                                }
                            } else {
                                requireActivity().runOnUiThread(this::showModalForUserInput);
                            }
                        }
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    public void getWeeklyData (int dailyProgressGoals, int userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        java.sql.Date startDate = new java.sql.Date(calendar.getTimeInMillis());
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        java.sql.Date endDate = new java.sql.Date(calendar.getTimeInMillis());

        String workoutQuery = "SELECT DAYOFWEEK(created_at) AS day, COUNT(*) AS workout_count " +
                "FROM workout_logs WHERE user_id = ? AND created_at BETWEEN ? AND ? " +
                "GROUP BY DAYOFWEEK(created_at)";

        new Thread(() -> {
            try(Connection con = connectionClass.CONN()){
                if (con != null) {
                    try (PreparedStatement stmt2 = con.prepareStatement(workoutQuery)) {
                        stmt2.setInt(1, userId);
                        stmt2.setDate(2, startDate);
                        stmt2.setDate(3, endDate);
                        try (ResultSet rs2 = stmt2.executeQuery()) {
                            int[] weeklyProgress = new int[7];
                            while (rs2.next()) {
                                int dayOfWeek = rs2.getInt("day") - 1;
                                int workoutCount = rs2.getInt("workout_count");
                                weeklyProgress[dayOfWeek] = (int) Math.min((workoutCount / (float) dailyProgressGoals) * 100, 100);
                            }

                            requireActivity().runOnUiThread(() -> updateWeeklyProgress(weeklyProgress));
                        }
                    }
                }else{
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error fetching weekly data: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    public void getMonthlyData(int dailyProgressGoals, int userId) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        java.sql.Date startDate = new java.sql.Date(calendar.getTimeInMillis());

        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        java.sql.Date endDate = new java.sql.Date(calendar.getTimeInMillis());

        String workoutQuery = "SELECT DAYOFMONTH(created_at) AS day, COUNT(*) AS workout_count " +
                "FROM workout_logs WHERE user_id = ? AND created_at BETWEEN ? AND ? " +
                "GROUP BY DAYOFMONTH(created_at)";

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    try (PreparedStatement stmt2 = con.prepareStatement(workoutQuery)) {
                        stmt2.setInt(1, userId);
                        stmt2.setDate(2, startDate);
                        stmt2.setDate(3, endDate);
                        try (ResultSet rs2 = stmt2.executeQuery()) {
                            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                            int[] monthlyProgress = new int[daysInMonth];
                            while (rs2.next()) {
                                int dayOfMonth = rs2.getInt("day") - 1;
                                int workoutCount = rs2.getInt("workout_count");
                                monthlyProgress[dayOfMonth] = (int) Math.min((workoutCount / (float) dailyProgressGoals) * 100, 100);
                            }

                            requireActivity().runOnUiThread(() -> updateMonthlyProgress(monthlyProgress));
                        }
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error fetching monthly data: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    private void updateMonthlyProgress(int[] monthlyProgress) {
        LinearLayout monthsLayout = requireView().findViewById(R.id.monthsLayout);
        monthsLayout.removeAllViews();

        for (int i = 0; i < monthlyProgress.length; i++) {
            LinearLayout dayLayout = new LinearLayout(getContext());
            dayLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView dayText = new TextView(getContext());
            dayText.setText("Day " + (i + 1));
            dayText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            ProgressBar dayProgress = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            dayProgress.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
            dayProgress.setMax(100);
            dayProgress.setProgress(monthlyProgress[i]);

            TextView percentageText = new TextView(getContext());
            percentageText.setText(monthlyProgress[i] + "%");
            percentageText.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            dayLayout.addView(dayText);
            dayLayout.addView(dayProgress);
            dayLayout.addView(percentageText);

            monthsLayout.addView(dayLayout);
        }
    }


    private void showModalForUserInput() {
        requireActivity().runOnUiThread(() -> {
            if (getContext() == null || !isAdded()) {
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Set Daily Progress Goals");

            final EditText input = new EditText(getContext());
            input.setHint("Please enter daily goals:");

            builder.setView(input);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String userInput = input.getText().toString().trim();
                if (!userInput.isEmpty()) {
                    saveDailyProgressGoals(userInput);
                } else {
                    Toast.makeText(getContext(), "Please enter a valid goal", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

            builder.show();
        });
    }
    private void saveDailyProgressGoals(String goals) {
        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String updateQuery = "UPDATE user_preferences SET daily_progress_goals = ? WHERE user_id = ?";
                    try (PreparedStatement stmt = con.prepareStatement(updateQuery)) {
                        stmt.setString(1, goals);
                        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
                        int userId = prefs.getInt("userId", -1);
                        stmt.setInt(2, userId);
                        stmt.executeUpdate();

                        showModal = false;
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Goals updated successfully", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error saving goals: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    private void updateWeeklyProgress(int[] weeklyProgress) {
        progressMonday.setProgress(weeklyProgress[Calendar.MONDAY - 1]);
        progressTuesday.setProgress(weeklyProgress[Calendar.TUESDAY - 1]);
        progressWednesday.setProgress(weeklyProgress[Calendar.WEDNESDAY - 1]);
        progressThursday.setProgress(weeklyProgress[Calendar.THURSDAY - 1]);
        progressFriday.setProgress(weeklyProgress[Calendar.FRIDAY - 1]);
        progressSaturday.setProgress(weeklyProgress[Calendar.SATURDAY - 1]);
        progressSunday.setProgress(weeklyProgress[Calendar.SUNDAY - 1]);

        ((TextView) requireView().findViewById(R.id.progressMondayPercentage))
                .setText(weeklyProgress[Calendar.MONDAY - 1] + "%");
        ((TextView) requireView().findViewById(R.id.progressTuesdayPercentage))
                .setText(weeklyProgress[Calendar.TUESDAY - 1] + "%");
        ((TextView) requireView().findViewById(R.id.progressWednesdayPercentage))
                .setText(weeklyProgress[Calendar.WEDNESDAY - 1] + "%");
        ((TextView) requireView().findViewById(R.id.progressThursdayPercentage))
                .setText(weeklyProgress[Calendar.THURSDAY - 1] + "%");
        ((TextView) requireView().findViewById(R.id.progressFridayPercentage))
                .setText(weeklyProgress[Calendar.FRIDAY - 1] + "%");
        ((TextView) requireView().findViewById(R.id.progressSaturdayPercentage))
                .setText(weeklyProgress[Calendar.SATURDAY - 1] + "%");
        ((TextView) requireView().findViewById(R.id.progressSundayPercentage))
                .setText(weeklyProgress[Calendar.SUNDAY - 1] + "%");
    }
}
