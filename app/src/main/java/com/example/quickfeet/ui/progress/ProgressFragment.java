package com.example.quickfeet.ui.progress;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.R;
import com.example.quickfeet.models.WorkoutLog;
import com.example.quickfeet.adapters.WorkoutLogAdapter;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ProgressFragment extends Fragment {

    private List<WorkoutLog> workoutLogs;
    private RecyclerView recyclerView;
    private WorkoutLogAdapter adapter;

    private EditText etWorkoutName, etSets, etReps, etWeight;
    private Button btnAddWorkout;

    private ConnectionClass connectionClass;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_progress, container, false);

        connectionClass = new ConnectionClass();

        etWorkoutName = view.findViewById(R.id.etWorkoutName);
        etSets = view.findViewById(R.id.etSets);
        etReps = view.findViewById(R.id.etReps);
        etWeight = view.findViewById(R.id.etWeight);
        btnAddWorkout = view.findViewById(R.id.btnAddWorkout);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        workoutLogs = new ArrayList<>();
        adapter = new WorkoutLogAdapter(workoutLogs, this::deleteWorkoutLog);
        recyclerView.setAdapter(adapter);

        btnAddWorkout.setOnClickListener(v -> addWorkoutLog());

        fetchWorkoutLogs();

        return view;
    }

    private void addWorkoutLog() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        Calendar calendar = Calendar.getInstance();
        java.util.Date today = calendar.getTime();
        Date currentDate = new Date(today.getTime());

        String workoutName = etWorkoutName.getText().toString().trim();
        String sets = etSets.getText().toString().trim();
        String reps = etReps.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();

        if (workoutName.isEmpty() || sets.isEmpty() || reps.isEmpty() || weight.isEmpty()) {
            Toast.makeText(getContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String queryInsert = "INSERT INTO workout_logs (user_id, workout_name, sets, reps, weight, created_at) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement stmtInsert = con.prepareStatement(queryInsert);
                    stmtInsert.setInt(1, userId);
                    stmtInsert.setString(2, workoutName);
                    stmtInsert.setInt(3, Integer.parseInt(sets));
                    stmtInsert.setInt(4, Integer.parseInt(reps));
                    stmtInsert.setDouble(5, Double.parseDouble(weight));
                    stmtInsert.setDate(6, currentDate);

                    int rowsAffected = stmtInsert.executeUpdate();
                    if (rowsAffected > 0) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), "Workout log saved to database!", Toast.LENGTH_SHORT).show();
                            fetchWorkoutLogs();
                        });
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Failed to save workout log", Toast.LENGTH_SHORT).show());
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

        etWorkoutName.setText("");
        etSets.setText("");
        etReps.setText("");
        etWeight.setText("");
    }

    private void fetchWorkoutLogs() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String queryFetch = "SELECT * FROM workout_logs WHERE user_id = ?";
                    PreparedStatement stmtFetch = con.prepareStatement(queryFetch);
                    stmtFetch.setInt(1, userId);

                    ResultSet rs = stmtFetch.executeQuery();
                    workoutLogs.clear();

                    while (rs.next()) {
                        WorkoutLog log = new WorkoutLog(
                                rs.getInt("id"),
                                rs.getString("workout_name"),
                                rs.getInt("sets"),
                                rs.getInt("reps"),
                                rs.getDouble("weight")
                        );
                        workoutLogs.add(log);
                    }

                    requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error fetching logs: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void deleteWorkoutLog(WorkoutLog log) {
        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String queryDelete = "DELETE FROM workout_logs WHERE id = ?";
                    PreparedStatement stmtDelete = con.prepareStatement(queryDelete);
                    stmtDelete.setInt(1, log.getLogId());

                    int rowsAffected = stmtDelete.executeUpdate();
                    if (rowsAffected > 0) {
                        requireActivity().runOnUiThread(() -> {
                            workoutLogs.remove(log);
                            adapter.notifyDataSetChanged();
                            Toast.makeText(getActivity(), "Workout log deleted!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error deleting log: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
