package com.example.quickfeet.ui.bmi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.R;
import com.example.quickfeet.adapters.BmiHistoryAdapter;
import com.example.quickfeet.models.BmiHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BmiFragment extends Fragment {

    private EditText edtWeight, edtHeight;
    private Button btnCalculate, btnSaveBmi;
    private TextView tvBmiResult;
    private RecyclerView recyclerViewHistory;

    private ConnectionClass connectionClass;
    private double calculatedBmi = -1;
    private double weight = -1, height = -1;

    public BmiFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bmi, container, false);

        edtWeight = rootView.findViewById(R.id.edtWeight);
        edtHeight = rootView.findViewById(R.id.edtHeight);
        btnCalculate = rootView.findViewById(R.id.btnCalculate);
        btnSaveBmi = rootView.findViewById(R.id.btnSaveBmi);
        tvBmiResult = rootView.findViewById(R.id.tvBmiResult);
        recyclerViewHistory = rootView.findViewById(R.id.recyclerViewHistory);

        connectionClass = new ConnectionClass();

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        btnCalculate.setOnClickListener(v -> calculateBmi());
        btnSaveBmi.setOnClickListener(v -> saveBmi());
        btnSaveBmi.setEnabled(false);

        loadBmiHistory();

        return rootView;
    }

    private void calculateBmi() {
        String weightStr = edtWeight.getText().toString().trim();
        String heightStr = edtHeight.getText().toString().trim();

        if (weightStr.isEmpty() || heightStr.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter both weight and height", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            weight = Double.parseDouble(weightStr);
            height = Double.parseDouble(heightStr) / 100;

            if (height > 0) {
                calculatedBmi = weight / (height * height);
                String bmiResult = String.format("Your BMI: %.2f", calculatedBmi);
                tvBmiResult.setText(bmiResult);
                btnSaveBmi.setEnabled(true);
            } else {
                Toast.makeText(getActivity(), "Height must be greater than zero", Toast.LENGTH_SHORT).show();
            }

        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Invalid input. Please enter numeric values.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBmi() {
        if (calculatedBmi == -1) {
            Toast.makeText(getActivity(), "Please calculate BMI first", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(getActivity(), "User not logged in. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String queryInsert = "INSERT INTO bmi_data (user_id, bmi_result, height, weight) VALUES (?, ?, ?, ?)";
                    PreparedStatement stmtInsert = con.prepareStatement(queryInsert);
                    stmtInsert.setInt(1, userId);
                    stmtInsert.setDouble(2, calculatedBmi);
                    stmtInsert.setDouble(3, height * 100);
                    stmtInsert.setDouble(4, weight);

                    int rowsAffected = stmtInsert.executeUpdate();
                    if (rowsAffected > 0) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "BMI saved to database", Toast.LENGTH_SHORT).show());
                        loadBmiHistory();
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Failed to save BMI", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error saving BMI: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void loadBmiHistory() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        if (userId != -1) {
            new Thread(() -> {
                List<BmiHistory> bmiHistoryList = new ArrayList<>();
                try (Connection con = connectionClass.CONN()) {
                    if (con != null) {
                        String query = "SELECT id, bmi_result, weight, height, date FROM bmi_data WHERE user_id = ?";
                        PreparedStatement stmt = con.prepareStatement(query);
                        stmt.setInt(1, userId);

                        var resultSet = stmt.executeQuery();
                        while (resultSet.next()) {
                            int id = resultSet.getInt("id");
                            double bmi = resultSet.getDouble("bmi_result");
                            double weight = resultSet.getDouble("weight");
                            double height = resultSet.getDouble("height");
                            String date = resultSet.getString("date");

                            bmiHistoryList.add(new BmiHistory(id, bmi, weight, height * 100, date));
                        }

                        requireActivity().runOnUiThread(() -> {
                            BmiHistoryAdapter adapter = new BmiHistoryAdapter(bmiHistoryList, this::deleteBmiRecord);
                            recyclerViewHistory.setAdapter(adapter);
                        });
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Error loading BMI history: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        }
    }
    private void deleteBmiRecord(int bmiId) {
        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String queryDelete = "DELETE FROM bmi_data WHERE id = ?";
                    PreparedStatement stmtDelete = con.prepareStatement(queryDelete);
                    stmtDelete.setInt(1, bmiId);

                    int rowsAffected = stmtDelete.executeUpdate();
                    if (rowsAffected > 0) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getActivity(), "BMI record deleted", Toast.LENGTH_SHORT).show();
                            loadBmiHistory();
                        });
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Failed to delete BMI record", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error deleting BMI record: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
