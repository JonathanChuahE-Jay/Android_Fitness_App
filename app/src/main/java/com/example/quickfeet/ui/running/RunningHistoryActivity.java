package com.example.quickfeet.ui.running;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.MainActivity;
import com.example.quickfeet.R;
import com.example.quickfeet.adapters.RunningHistoryAdapter;
import com.example.quickfeet.models.RunningHistoryItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RunningHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private RunningHistoryAdapter historyAdapter;
    private ConnectionClass connectionClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Running History");
        }
        connectionClass = new ConnectionClass();
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(RunningHistoryActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

        fetchHistoryFromDatabase();
    }
    private void fetchHistoryFromDatabase() {
        new Thread(() -> {
            List<RunningHistoryItem> historyItems = new ArrayList<>();
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String query = "SELECT id, distance, speed, time, calories FROM running_records";
                    try (Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                        while (rs.next()) {
                            int id = rs.getInt("id");
                            double distance = rs.getDouble("distance");
                            double speed = rs.getDouble("speed");
                            String time = rs.getString("time");
                            double calories = rs.getDouble("calories");
                            historyItems.add(new RunningHistoryItem(id, distance, speed, time, calories));
                        }
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("RunningHistoryActivity", "Error fetching history: ", e);
            }

            runOnUiThread(() -> {
                historyAdapter = new RunningHistoryAdapter(historyItems, item -> {
                    deleteRecord(item);
                });
                historyRecyclerView.setAdapter(historyAdapter);
            });
        }).start();
    }

    private void deleteRecord(RunningHistoryItem item) {
        new Thread(() -> {
            try (Connection con = connectionClass.CONN()) {
                if (con != null) {
                    String query = "DELETE FROM running_records WHERE id = ?";
                    try (PreparedStatement stmt = con.prepareStatement(query)) {
                        stmt.setInt(1, item.getId());
                        int rowsAffected = stmt.executeUpdate();
                        runOnUiThread(() -> {
                            if (rowsAffected > 0) {
                                Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                                fetchHistoryFromDatabase();
                            } else {
                                Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("RunningHistoryActivity", "Error deleting history: ", e);
            }
        }).start();
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
