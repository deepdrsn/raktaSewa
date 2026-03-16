package com.example.raktasewa.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.raktasewa.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DonorListActivity extends AppCompatActivity {

    private Spinner spinnerBloodType;
    private Button btnSearchDonors;
    private ListView listViewDonors;
    private ProgressBar progressBar;
    private TextView tvSearchStatus;
    private BottomNavigationView bottomNavigation;

    private FirebaseFirestore fStore;
    private DonorAdapter donorAdapter;
    private List<Donor> donorList;
    private List<Donor> allMatchingDonors;
    
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentUserLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_list);

        initializeUI();
        setupSpinner();
        setupBottomNavigation();

        fStore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        donorList = new ArrayList<>();
        allMatchingDonors = new ArrayList<>();
        donorAdapter = new DonorAdapter(this, donorList);
        listViewDonors.setAdapter(donorAdapter);

        btnSearchDonors.setOnClickListener(v -> checkLocationPermissionAndSearch());
    }

    private void initializeUI() {
        spinnerBloodType = findViewById(R.id.spinnerBloodType);
        btnSearchDonors = findViewById(R.id.btnSearchDonors);
        listViewDonors = findViewById(R.id.listViewDonors);
        progressBar = findViewById(R.id.progressBar);
        tvSearchStatus = findViewById(R.id.tvSearchStatus);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(bloodAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_donors);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            }
            if (id == R.id.nav_donors) return true;
            if (id == R.id.nav_requests) {
                startActivity(new Intent(this, ViewRequestsActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void checkLocationPermissionAndSearch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        } else {
            getCurrentLocationAndSearch();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocationAndSearch();
        } else {
            Toast.makeText(this, "Location permission is required for distance-based search", Toast.LENGTH_SHORT).show();
            searchDonors(null);
        }
    }

    private void getCurrentLocationAndSearch() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            currentUserLocation = location;
            searchDonors(location);
        });
    }

    private void searchDonors(Location myLocation) {
        String bloodType = spinnerBloodType.getSelectedItem().toString();

        if (bloodType.equals("Select Blood Type")) {
            Toast.makeText(this, "Please select blood type", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        donorList.clear();
        allMatchingDonors.clear();

        fStore.collection("users")
                .whereEqualTo("role", "donor")
                .whereEqualTo("bloodType", bloodType)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Boolean isAvailable = document.getBoolean("available");
                            Double lat = document.getDouble("latitude");
                            Double lon = document.getDouble("longitude");
                            String city = document.getString("city");

                            Donor donor = new Donor(
                                    document.getString("fullName"),
                                    document.getString("bloodType"),
                                    document.getString("phone"),
                                    document.getString("address"),
                                    document.getString("lastDonatedDate"),
                                    isAvailable != null ? isAvailable : false,
                                    lat != null ? lat : 0.0,
                                    lon != null ? lon : 0.0,
                                    city != null ? city : ""
                            );
                            
                            if (myLocation != null && lat != null && lon != null) {
                                float[] results = new float[1];
                                Location.distanceBetween(myLocation.getLatitude(), myLocation.getLongitude(), lat, lon, results);
                                donor.setDistance(results[0] / 1000.0);
                            }
                            
                            allMatchingDonors.add(donor);
                        }
                        
                        applyRadiusFilter(myLocation != null);
                        
                    } else {
                        Toast.makeText(DonorListActivity.this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyRadiusFilter(boolean hasLocation) {
        donorList.clear();

        // Sort: Available donors first, then sort by distance (if location exists)
        Collections.sort(allMatchingDonors, (d1, d2) -> {
            // Priority 1: Availability (true first)
            if (d1.isAvailableToDonate() != d2.isAvailableToDonate()) {
                return d1.isAvailableToDonate() ? -1 : 1;
            }
            // Priority 2: Distance (closer first)
            if (hasLocation) {
                return Double.compare(d1.getDistance(), d2.getDistance());
            }
            return 0;
        });
        
        if (!hasLocation) {
            donorList.addAll(allMatchingDonors);
            updateUIWithResults("Showing all matching donors (Location unavailable)");
            donorAdapter.notifyDataSetChanged();
            return;
        }

        int[] radii = {5, 10, 15, 20, 25};
        int selectedRadius = -1;

        for (int radius : radii) {
            List<Donor> filtered = new ArrayList<>();
            boolean hasAvailable = false;
            for (Donor d : allMatchingDonors) {
                if (d.getDistance() <= radius) {
                    filtered.add(d);
                    if (d.isAvailableToDonate()) {
                        hasAvailable = true;
                    }
                }
            }
            // Break ONLY if we found at least one AVAILABLE donor in this radius
            if (hasAvailable) {
                donorList.addAll(filtered);
                selectedRadius = radius;
                break;
            }
        }

        if (donorList.isEmpty()) {
            if (allMatchingDonors.isEmpty()) {
                updateUIWithResults("No donors found with this blood type.");
            } else {
                // If no AVAILABLE donors found within 25km, show all matching ones as a fallback
                donorList.addAll(allMatchingDonors);
                updateUIWithResults("No available donors within 25 km. Showing all matches.");
            }
        } else {
            updateUIWithResults("Found available donors within " + selectedRadius + " km.");
        }
        
        donorAdapter.notifyDataSetChanged();
    }

    private void updateUIWithResults(String message) {
        if (tvSearchStatus != null) {
            tvSearchStatus.setVisibility(View.VISIBLE);
            tvSearchStatus.setText(message);
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }
}
