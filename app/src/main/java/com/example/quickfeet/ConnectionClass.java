package com.example.quickfeet;

import android.util.Log;

import com.mysql.jdbc.Connection;

import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Objects;

public class ConnectionClass {
    protected static String db = "quickfeet";
    protected static String ip = "192.168.0.16";
    protected static String port = "3306";
    protected static String username = "root";
    protected static String password = "123123";

    public Connection CONN() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            String connectionString = "jdbc:mysql://" + ip + ":" + port + "/" + db;
            conn = (Connection) DriverManager.getConnection(connectionString, username, password);

            if (conn != null) {
                createTables(conn);
            }
        } catch (Exception e) {
            Log.e("ERROR", Objects.requireNonNull(e.getMessage()));
        }
        return conn;
    }

    private void createTables(Connection conn) {
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "name VARCHAR(100) NOT NULL, "
                + "email VARCHAR(100) NOT NULL UNIQUE, "
                + "password VARCHAR(100) NOT NULL, "
                + "profile_picture TEXT);";

        String createBmiDataTable = "CREATE TABLE IF NOT EXISTS bmi_data ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT NOT NULL, "
                + "bmi_result DOUBLE NOT NULL, "
                + "height DOUBLE NOT NULL, "
                + "weight DOUBLE NOT NULL, "
                + "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (user_id) REFERENCES users(id));";

        String createRunningRecordsTable = "CREATE TABLE IF NOT EXISTS running_records ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT, "
                + "distance DOUBLE, "
                + "speed DOUBLE, "
                + "time TEXT, "
                + "calories DOUBLE, "
                + "FOREIGN KEY (user_id) REFERENCES users(id));";

        String createUserPreferencesTable = "CREATE TABLE IF NOT EXISTS user_preferences ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT, "
                + "daily_progress_goals INT, "
                + "FOREIGN KEY (user_id) REFERENCES users(id));";

        String createWorkoutLogsTable = "CREATE TABLE IF NOT EXISTS workout_logs ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "user_id INT, "
                + "workout_name VARCHAR(100) NOT NULL, "
                + "sets INT, "
                + "reps INT, "
                + "weight DOUBLE, "
                + "created_at DATE, "
                + "FOREIGN KEY (user_id) REFERENCES users(id));";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createBmiDataTable);
            stmt.execute(createRunningRecordsTable);
            stmt.execute(createUserPreferencesTable);
            stmt.execute(createWorkoutLogsTable);
            Log.d("INFO", "Tables created successfully.");
        } catch (Exception e) {
            Log.e("ERROR", "Error creating tables: " + e.getMessage());
        }
    }
}
