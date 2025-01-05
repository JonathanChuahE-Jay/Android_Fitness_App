package com.example.quickfeet.ui.workout_manager;

import com.example.quickfeet.adapters.ImagePagerAdapter;
import com.example.quickfeet.models.Workout;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WorkoutManagerFragment extends Fragment {
    private ConnectionClass connectionClass;
    private GridLayout dayGridLayout;
    private View difficultySelectionLayout;
    private View workoutGridLayout;
    private List<Workout> workouts;
    private List<Workout> filteredWorkouts;
    private AlertDialog detailsDialog;
    private AlertDialog inputDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        workouts = loadWorkoutsFromJson();
        filteredWorkouts = new ArrayList<>();
        connectionClass = new ConnectionClass();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout_manager, container, false);

        difficultySelectionLayout = view.findViewById(R.id.difficultySelectionLayout);
        workoutGridLayout = view.findViewById(R.id.workoutGridLayout);
        dayGridLayout = view.findViewById(R.id.dayGridLayout);

        setupDifficultySelection(view);

        return view;
    }

    private List<Workout> loadWorkoutsFromJson() {
        List<Workout> workoutList = new ArrayList<>();
        try {
            InputStream inputStream = requireContext().getAssets().open("workouts.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            JSONArray jsonArray = new JSONArray(jsonStringBuilder.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Workout workout = new Workout(
                        jsonObject.getString("name"),
                        jsonObject.getString("level"),
                        jsonObject.getJSONArray("instructions").join("\n").replace("\"", ""),
                        jsonObject.getJSONArray("images")
                );
                workoutList.add(workout);
            }
            Log.d("LoadWorkouts", "Loaded " + workoutList.size() + " workouts.");
        } catch (Exception e) {
            Log.e("LoadWorkouts", "Error loading workouts from JSON", e);
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Failed to load workouts.", Toast.LENGTH_SHORT).show()
            );
        }
        return workoutList;
    }

    private void setupDifficultySelection(View view) {
        view.findViewById(R.id.btnBeginner).setOnClickListener(v -> filterAndDisplayWorkouts("beginner"));
        view.findViewById(R.id.btnIntermediate).setOnClickListener(v -> filterAndDisplayWorkouts("intermediate"));
        view.findViewById(R.id.btnProfessional).setOnClickListener(v -> filterAndDisplayWorkouts("expert"));
    }

    private void filterAndDisplayWorkouts(String difficulty) {
        filteredWorkouts = workouts.stream()
                .filter(workout -> workout.getLevel().equalsIgnoreCase(difficulty))
                .collect(Collectors.toList());

        if (filteredWorkouts.isEmpty()) {
            Toast.makeText(requireContext(), "No workouts available for " + difficulty + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        requireActivity().runOnUiThread(() -> {
            difficultySelectionLayout.setVisibility(View.GONE);
            workoutGridLayout.setVisibility(View.VISIBLE);
            populateWorkoutGrid();
        });
    }

    private void populateWorkoutGrid() {
        dayGridLayout.removeAllViews();
        dayGridLayout.setColumnCount(3);

        for (Workout workout : filteredWorkouts) {
            LinearLayout workoutLayout = new LinearLayout(requireContext());
            workoutLayout.setOrientation(LinearLayout.VERTICAL);
            workoutLayout.setGravity(Gravity.CENTER);
            workoutLayout.setPadding(15, 15, 15, 15);

            ImageView workoutImage = new ImageView(requireContext());
            workoutImage.setLayoutParams(new ViewGroup.LayoutParams(290, 300));
            workoutImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

            try (InputStream is = requireContext().getAssets().open(workout.getImages().get(0))) {
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                workoutImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                workoutImage.setImageResource(R.drawable.workout_icon);
                Log.e("ImageLoad", "Error loading image for " + workout.getName(), e);
            }

            TextView workoutTitle = new TextView(requireContext());
            workoutTitle.setText(workout.getName());
            workoutTitle.setGravity(Gravity.CENTER);
            workoutTitle.setPadding(5, 5, 5, 5);

            workoutLayout.setOnClickListener(v -> showWorkoutDetails(workout));

            workoutLayout.addView(workoutImage);
            workoutLayout.addView(workoutTitle);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(10, 10, 10, 10);
            workoutLayout.setLayoutParams(params);

            dayGridLayout.addView(workoutLayout);
        }
    }

    private void showWorkoutDetails(Workout workout) {
        LinearLayout detailsLayout = new LinearLayout(requireContext());
        detailsLayout.setOrientation(LinearLayout.VERTICAL);
        detailsLayout.setPadding(20, 20, 20, 20);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(detailsLayout);

        ViewPager viewPager = new ViewPager(requireContext());
        ImagePagerAdapter adapter = new ImagePagerAdapter(requireContext(), workout.getImages());
        viewPager.setAdapter(adapter);
        LinearLayout.LayoutParams pagerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
        );
        pagerParams.setMargins(0, 0, 0, 20);
        viewPager.setLayoutParams(pagerParams);
        detailsLayout.addView(viewPager);

        TextView nameTextView = new TextView(requireContext());
        nameTextView.setText("Workout Name: " + workout.getName());
        nameTextView.setTextSize(18);
        nameTextView.setPadding(10, 20, 10, 10);
        nameTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        detailsLayout.addView(nameTextView);

        TextView levelTextView = new TextView(requireContext());
        levelTextView.setText("Level: " + workout.getLevel());
        levelTextView.setTextSize(16);
        levelTextView.setPadding(10, 10, 10, 10);
        levelTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        detailsLayout.addView(levelTextView);

        TextView instructionsTextView = new TextView(requireContext());
        instructionsTextView.setText("Instructions:\n" + workout.getInstructions());
        instructionsTextView.setTextSize(14);
        instructionsTextView.setPadding(10, 10, 10, 10);
        detailsLayout.addView(instructionsTextView);

        detailsDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Workout Details")
                .setView(scrollView)
                .setPositiveButton("Save", (dialog, which) -> {
                    detailsDialog = null;
                    showInputModal(workout.getName());
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    detailsDialog = null;
                    dialog.dismiss();
                })
                .create();
        detailsDialog.show();
    }

    private void showInputModal(String workoutName) {
        LinearLayout inputLayout = new LinearLayout(requireContext());
        inputLayout.setOrientation(LinearLayout.VERTICAL);
        inputLayout.setPadding(20, 20, 20, 20);

        TextView workoutLabel = new TextView(requireContext());
        workoutLabel.setText("Workout: " + workoutName);
        workoutLabel.setPadding(10, 10, 10, 10);
        workoutLabel.setTextSize(16);
        inputLayout.addView(workoutLabel);

        TextView setsLabel = new TextView(requireContext());
        setsLabel.setText("Sets:");
        setsLabel.setPadding(10, 10, 10, 10);
        inputLayout.addView(setsLabel);

        EditText setsInput = new EditText(requireContext());
        setsInput.setHint("Enter number of sets");
        setsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputLayout.addView(setsInput);

        TextView repsLabel = new TextView(requireContext());
        repsLabel.setText("Reps:");
        repsLabel.setPadding(10, 10, 10, 10);
        inputLayout.addView(repsLabel);

        EditText repsInput = new EditText(requireContext());
        repsInput.setHint("Enter number of reps");
        repsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        inputLayout.addView(repsInput);

        TextView weightLabel = new TextView(requireContext());
        weightLabel.setText("Weight:");
        weightLabel.setPadding(10, 10, 10, 10);
        inputLayout.addView(weightLabel);

        EditText weightInput = new EditText(requireContext());
        weightInput.setHint("Enter weight");
        weightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputLayout.addView(weightInput);

        inputDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Input Details")
                .setView(inputLayout)
                .setPositiveButton("OK", (dialog, which) -> {
                    inputDialog = null;
                    String sets = setsInput.getText().toString();
                    String reps = repsInput.getText().toString();
                    String weight = weightInput.getText().toString();
                    if (sets.isEmpty() || reps.isEmpty() || weight.isEmpty()) {
                        Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
                    } else {
                        addWorkoutLog(workoutName, sets, reps, weight);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    inputDialog = null;
                    dialog.dismiss();
                })
                .create();
        inputDialog.show();
    }

    private void addWorkoutLog(String workoutName, String sets, String reps, String weight) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        Calendar calendar = Calendar.getInstance();
        java.util.Date today = calendar.getTime();
        Date currentDate = new Date(today.getTime());

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
                            if (detailsDialog != null) {
                                detailsDialog.dismiss();
                                detailsDialog = null;
                            }
                            if (inputDialog != null) {
                                inputDialog.dismiss();
                                inputDialog = null;
                            }
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
    }
}


