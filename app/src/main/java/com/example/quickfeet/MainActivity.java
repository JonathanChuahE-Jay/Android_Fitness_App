
// fix button going wrong pages
package com.example.quickfeet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.quickfeet.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import com.mysql.jdbc.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ConnectionClass connectionClass;
    Connection con;
    String  str;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        if (!isLoggedIn) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        String userName = prefs.getString("userName", "Default Name");
        String userEmail = prefs.getString("userEmail", "example@example.com");
        String profilePicBase64 = prefs.getString("userProfilePic", null);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        connectionClass = new ConnectionClass();
        connect();
        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        View headerView = navigationView.getHeaderView(0);
        TextView headerTitle = headerView.findViewById(R.id.headerTitle);
        TextView subTitle = headerView.findViewById(R.id.subTitle);
        ImageView profileImage = headerView.findViewById(R.id.profileImage);

        headerTitle.setText(userName);
        subTitle.setText(userEmail);
        if (profilePicBase64 != null) {
            byte[] decodedString = Base64.decode(profilePicBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profileImage.setImageBitmap(decodedByte);
        }
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_bmi_calculator,
                R.id.nav_home,
                R.id.nav_progress_tracker,
                R.id.nav_running_tracker,
                R.id.nav_workout_manager,
                R.id.nav_profile,
                R.id.nav_share_it)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_logout) {
                logout();
            } else {
                try {
                    navController.navigate(id);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, "Invalid navigation ID", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("NavigationError", "Invalid navigation action", e);
                    Toast.makeText(this, "Error: Navigation failed.", Toast.LENGTH_SHORT).show();
                }
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            try{
                if (id == R.id.nav_home ) {
                    navController.navigate(id);
                    return true;
                }
            }catch (Exception e) {
                Log.e("NavigationError", "Invalid navigation action", e);
                Toast.makeText(this, "Error: Navigation failed.", Toast.LENGTH_SHORT).show();
            }
            return false;
        });


    }

    public void connect() {
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.execute(() -> {
            try {
                con = connectionClass.CONN();
                if (con == null) {
                    str = "Error: Unable to connect to the database";
                } else {
                    str = "Connected to the database successfully!";
                }
                runOnUiThread(() -> Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }
}
