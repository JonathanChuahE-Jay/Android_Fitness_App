package com.example.quickfeet.ui.running;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quickfeet.ConnectionClass;
import com.example.quickfeet.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RunningFragment extends Fragment implements OnMapReadyCallback {

    private TextView distanceTextView, speedTextView, timeTextView, caloriesTextView;
    private Button startButton, stopButton, resetButton, saveButton, resumeButton, historyButton ;

    private long startTime = 0L;
    private long pauseTime = 0L;
    private Handler handler = new Handler();
    private boolean isRunning = false;
    private boolean isPaused = false;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;

    private double totalDistanceMeters = 0.0;
    private double speed = 0.0;
    private double calories = 0.0;
    private static final float MIN_DISTANCE_THRESHOLD = 0.5f;
    private static final float MIN_ACCURACY_THRESHOLD = 10.0f;

    private GoogleMap googleMap;
    private Polyline pathPolyline;
    private ConnectionClass connectionClass;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                long elapsedTime = SystemClock.elapsedRealtime() - startTime;
                updateUI(elapsedTime);
                handler.postDelayed(this, 1000);
            }
        }
    };

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
            if (locationResult == null || locationResult.getLastLocation() == null) return;

            Location currentLocation = locationResult.getLastLocation();

            if (isValidLocation(currentLocation)) {
                if (lastLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            lastLocation.getLatitude(), lastLocation.getLongitude(),
                            currentLocation.getLatitude(), currentLocation.getLongitude(),
                            results);

                    if (results[0] >= MIN_DISTANCE_THRESHOLD) {
                        totalDistanceMeters += results[0];

                        long elapsedTimeMillis = SystemClock.elapsedRealtime() - startTime;
                        double elapsedTimeSeconds = elapsedTimeMillis / 1000.0;
                        speed = (totalDistanceMeters / 1000.0) / (elapsedTimeMillis / 3600000.0);

                        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", getActivity().MODE_PRIVATE);
                        double weightInKg = prefs.getFloat("userWeight", 70);
                        double elapsedTimeMinutes = elapsedTimeMillis / 60000.0;
                        double caloriesPerMinute = (weightInKg * 0.035) + (Math.pow(speed / 3.6, 2) * 0.029 * weightInKg);
                        calories = caloriesPerMinute * elapsedTimeMinutes;

                        LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        updatePathOnMap(currentLatLng);
                    } else {
                        speed = 0.0;
                    }
                } else {
                    speed = 0.0;
                }
                updateUI(SystemClock.elapsedRealtime() - startTime);
                lastLocation = currentLocation;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_running, container, false);

        distanceTextView = view.findViewById(R.id.distanceTextView);
        speedTextView = view.findViewById(R.id.speedTextView);
        timeTextView = view.findViewById(R.id.timeTextView);
        caloriesTextView = view.findViewById(R.id.caloriesTextView);
        startButton = view.findViewById(R.id.startButton);
        stopButton = view.findViewById(R.id.stopButton);
        resetButton = view.findViewById(R.id.resetButton);
        saveButton = view.findViewById(R.id.saveButton);
        resumeButton = view.findViewById(R.id.resumeButton);
        Button historyButton = view.findViewById(R.id.historyButton);

        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), RunningHistoryActivity.class);
            startActivity(intent);
        });


        connectionClass = new ConnectionClass();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        startButton.setOnClickListener(v -> {
            if (!isRunning) {
                isRunning = true;
                startTime = SystemClock.elapsedRealtime();
                handler.post(timerRunnable);
                startLocationUpdates();

                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                resetButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.GONE);
                resumeButton.setVisibility(View.GONE);
            }
            resetButton.setVisibility(View.VISIBLE);
        });

        stopButton.setOnClickListener(v -> {
            if (isRunning) {
                isRunning = false;
                isPaused = true;
                pauseTime = SystemClock.elapsedRealtime();
                handler.removeCallbacks(timerRunnable);
                stopLocationUpdates();

                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.GONE);
                resumeButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);
                resetButton.setVisibility(View.VISIBLE);

                long elapsedTimeMillis = SystemClock.elapsedRealtime() - startTime;
                updateUI(elapsedTimeMillis);
            }
        });

        resumeButton.setOnClickListener(v -> {
            if (isPaused) {
                isRunning = true;
                isPaused = false;
                long resumeTime = SystemClock.elapsedRealtime();
                startTime += (resumeTime - pauseTime);
                handler.post(timerRunnable);
                startLocationUpdates();

                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                resumeButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
                resetButton.setVisibility(View.VISIBLE);
            }
        });
        resetButton.setOnClickListener(v -> resetTracker());

        saveButton.setOnClickListener(v -> saveRecord());

        return view;
    }

    private boolean isValidLocation(Location location) {
        if (location == null || location.getAccuracy() > MIN_ACCURACY_THRESHOLD) return false;
        if (lastLocation != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    lastLocation.getLatitude(), lastLocation.getLongitude(),
                    location.getLatitude(), location.getLongitude(),
                    results);

            double elapsedTimeSeconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0;
            double calculatedSpeed = (results[0] / elapsedTimeSeconds);
            if (calculatedSpeed > 10.0) return false;
        }
        return true;
    }

    private void updatePathOnMap(LatLng latLng) {
        if (googleMap != null) {
            if (pathPolyline == null) {
                pathPolyline = googleMap.addPolyline(new PolylineOptions().add(latLng).width(5).color(R.color.polyline_color));
            } else {
                List<LatLng> points = pathPolyline.getPoints();
                points.add(latLng);
                pathPolyline.setPoints(points);
            }
        }
    }

    private void saveRecord() {
        if (totalDistanceMeters < 0) {
            Toast.makeText(requireContext(), "Distance too short to save!", Toast.LENGTH_SHORT).show();
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
                    String query = "INSERT INTO running_records (distance, speed, time, calories, user_id) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = con.prepareStatement(query)) {
                        stmt.setDouble(1, totalDistanceMeters);
                        stmt.setDouble(2, speed);
                        stmt.setString(3, formatElapsedTime(SystemClock.elapsedRealtime() - startTime));
                        stmt.setDouble(4, calories);
                        stmt.setInt(5, userId);

                        int rowsAffected = stmt.executeUpdate();
                        requireActivity().runOnUiThread(() -> {
                            if (rowsAffected > 0) {
                                Toast.makeText(getActivity(), "Record saved to database", Toast.LENGTH_SHORT).show();
                                resetTracker();
                            } else {
                                Toast.makeText(getActivity(), "Failed to save record", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Database connection failed", Toast.LENGTH_SHORT).show());
                }
            } catch (SQLException e) {
                Log.e("RunningFragment", "Database error: ", e);
            }
        }).start();
    }

    private void resetTracker() {
        isRunning = false;
        handler.removeCallbacks(timerRunnable);
        stopLocationUpdates();

        totalDistanceMeters = 0.0;
        speed = 0.0;
        calories = 0.0;
        startTime = 0L;

        updateUI(0L);
        lastLocation = null;

        if (pathPolyline != null) {
            pathPolyline.setPoints(new ArrayList<>());
        }
        resumeButton.setVisibility(View.GONE);
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.GONE);
        resetButton.setVisibility(View.GONE);
    }

    private void updateUI(long elapsedTimeMillis) {
        distanceTextView.setText(String.format(Locale.getDefault(), "%.2f m", totalDistanceMeters));
        speedTextView.setText(String.format(Locale.getDefault(), "%.2f km/h", speed));
        timeTextView.setText(formatElapsedTime(elapsedTimeMillis));
        caloriesTextView.setText(String.format(Locale.getDefault(), "%.2f cal", calories));
    }

    private String formatElapsedTime(long elapsedTimeMillis) {
        long seconds = (elapsedTimeMillis / 1000) % 60;
        long minutes = (elapsedTimeMillis / (1000 * 60)) % 60;
        long hours = (elapsedTimeMillis / (1000 * 60 * 60)) % 24;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = LocationRequest.create()
                    .setInterval(1000)
                    .setFastestInterval(500)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setSmallestDisplacement(MIN_DISTANCE_THRESHOLD);

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        LatLng defaultLocation = new LatLng(-34, 151);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
        enableMyLocation();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                googleMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e("RunningFragment", "Error enabling My Location layer", e);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
