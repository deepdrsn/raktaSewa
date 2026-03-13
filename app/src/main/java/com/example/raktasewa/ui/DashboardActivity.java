package com.example.raktasewa.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.raktasewa.R;
import com.example.raktasewa.ui.login.LoginActivity;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    private TextView tvWelcome;
    private Button btnCreateRequest, btnViewRequests, btnDonorList, btnProfile, btnLogout;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser currentUser;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        initializeUI();
        
        // Show all buttons immediately so they don't disappear
        showAllButtons();
        
        // Setup click listeners immediately
        setupClickListeners();
        
        // Load profile in background to update welcome message and check role for location
        loadUserProfile();
    }

    private void initializeUI() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnCreateRequest = findViewById(R.id.btnCreateRequest);
        btnViewRequests = findViewById(R.id.btnViewRequests);
        btnDonorList = findViewById(R.id.btnDonorList);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void showAllButtons() {
        btnCreateRequest.setVisibility(View.VISIBLE);
        btnViewRequests.setVisibility(View.VISIBLE);
        btnDonorList.setVisibility(View.VISIBLE);
        btnProfile.setVisibility(View.VISIBLE);
        btnLogout.setVisibility(View.VISIBLE);
    }

    private void loadUserProfile() {
        DocumentReference docRef = fStore.collection("users").document(currentUser.getUid());

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String name = document.getString("fullName");
                    tvWelcome.setText("Welcome, " + (name != null ? name : "User"));

                    String role = document.getString("role");
                    if ("donor".equals(role)) {
                        checkAndRequestLocation();
                    }
                }
            } else {
                Log.d(TAG, "Profile fetch failed: " + task.getException());
            }
        });
    }

    private void checkAndRequestLocation() {
        SharedPreferences sharedPref = getSharedPreferences("RaktaSewaPrefs", Context.MODE_PRIVATE);
        long lastUpdate = sharedPref.getLong("lastLocationUpdate", 0);
        long currentTime = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        if (currentTime - lastUpdate > oneDayMillis) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                updateDonorLocation();
            }
        }
    }

    private void updateDonorLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CurrentLocationRequest locationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(10000)
                .build();

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(locationRequest, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        saveLocationToFirestore(location);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Location fetch failed", e));
    }

    private void saveLocationToFirestore(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String city = "Unknown";
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    city = address.getLocality();
                    if (city == null) {
                        city = address.getSubAdminArea(); // Fallback
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder failed", e);
            }

            final String finalCity = city;
            
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", lat);
            locationData.put("longitude", lon);
            locationData.put("city", finalCity);
            locationData.put("lastLocationUpdate", System.currentTimeMillis());

            fStore.collection("users").document(currentUser.getUid())
                    .update(locationData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Location updated in Firestore for donor");
                        SharedPreferences sharedPref = getSharedPreferences("RaktaSewaPrefs", Context.MODE_PRIVATE);
                        sharedPref.edit().putLong("lastLocationUpdate", System.currentTimeMillis()).apply();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update location in Firestore", e));
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateDonorLocation();
            }
        }
    }

    private void setupClickListeners() {
        btnCreateRequest.setOnClickListener(v -> startActivity(new Intent(this, CreateRequestActivity.class)));
        
        btnViewRequests.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewRequestsActivity.class));
        });

        btnDonorList.setOnClickListener(v -> {
           // Toast.makeText(this, "Donor list coming soon", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DonorListActivity.class));
        });

        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        btnLogout.setOnClickListener(v -> {
            fAuth.signOut();
            navigateToLogin();
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
