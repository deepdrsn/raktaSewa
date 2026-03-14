package com.example.raktasewa.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.raktasewa.R;
import com.example.raktasewa.ui.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    private TextView tvUserName, tvDonorBloodType, tvLastDonated, tvEligibilityMessage, tvActiveRequestsCount, tvAvailableDonorsCount;
    private SwitchMaterial switchAvailability;
    private MaterialCardView cardDonorStatus, btnEmergencyRequest, btnCreateRequest, btnFindDonors, btnViewRequests, btnProfile;
    private RecyclerView rvNearbyRequests;
    private ProgressBar pbRequests;
    private BottomNavigationView bottomNavigation;
    private ImageView ivNotifications;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser currentUser;
    
    private RequestAdapter requestAdapter;
    private List<BloodRequest> requestList;
    private String userRole = "";
    private String userBloodType = "";
    private String lastDonationDateStr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();

        if (currentUser == null) {
            navigateToLogin();
            return;
        }

        initializeUI();
        setupRecyclerView();
        setupClickListeners();
        setupBottomNavigation();
        
        loadUserProfile();
        loadQuickStats();
        loadNearbyRequests();
    }

    private void initializeUI() {
        tvUserName = findViewById(R.id.tvUserName);
        tvDonorBloodType = findViewById(R.id.tvDonorBloodType);
        tvLastDonated = findViewById(R.id.tvLastDonated);
        tvEligibilityMessage = findViewById(R.id.tvEligibilityMessage);
        tvActiveRequestsCount = findViewById(R.id.tvActiveRequestsCount);
        tvAvailableDonorsCount = findViewById(R.id.tvAvailableDonorsCount);
        
        switchAvailability = findViewById(R.id.switchAvailability);
        cardDonorStatus = findViewById(R.id.cardDonorStatus);
        btnEmergencyRequest = findViewById(R.id.btnEmergencyRequest);
        btnCreateRequest = findViewById(R.id.btnCreateRequest);
        btnFindDonors = findViewById(R.id.btnFindDonors);
        btnViewRequests = findViewById(R.id.btnViewRequests);
        btnProfile = findViewById(R.id.btnProfile);
        
        rvNearbyRequests = findViewById(R.id.rvNearbyRequests);
        pbRequests = findViewById(R.id.pbRequests);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        ivNotifications = findViewById(R.id.ivNotifications);
    }

    private void setupRecyclerView() {
        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, requestList, currentUser.getUid());
        rvNearbyRequests.setLayoutManager(new LinearLayoutManager(this));
        rvNearbyRequests.setAdapter(requestAdapter);
    }

    private void setupClickListeners() {
        btnEmergencyRequest.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateRequestActivity.class);
            intent.putExtra("isEmergency", true);
            startActivity(intent);
        });

        btnCreateRequest.setOnClickListener(v -> startActivity(new Intent(this, CreateRequestActivity.class)));
        btnFindDonors.setOnClickListener(v -> startActivity(new Intent(this, DonorListActivity.class)));
        btnViewRequests.setOnClickListener(v -> startActivity(new Intent(this, ViewRequestsActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isEligibleToDonate()) {
                switchAvailability.setChecked(false);
                Toast.makeText(this, "You are not eligible to donate yet.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateAvailability(isChecked);
        });
        
        ivNotifications.setOnClickListener(v -> Toast.makeText(this, "No new notifications", Toast.LENGTH_SHORT).show());
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_donors) {
                startActivity(new Intent(this, DonorListActivity.class));
                return true;
            }
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

    private void loadUserProfile() {
        fStore.collection("users").document(currentUser.getUid())
                .addSnapshotListener((document, error) -> {
                    if (error != null) return;
                    if (document != null && document.exists()) {
                        String name = document.getString("fullName");
                        tvUserName.setText(name != null ? name : "User");
                        
                        userRole = document.getString("role");
                        userBloodType = document.getString("bloodType");
                        lastDonationDateStr = document.getString("lastDonatedDate");
                        Boolean isAvailable = document.getBoolean("available");
                        
                        if ("donor".equals(userRole)) {
                            cardDonorStatus.setVisibility(View.VISIBLE);
                            tvDonorBloodType.setText(userBloodType != null ? userBloodType : "--");
                            tvLastDonated.setText(lastDonationDateStr != null && !lastDonationDateStr.isEmpty() ? lastDonationDateStr : "Never");
                            
                            switchAvailability.setChecked(isAvailable != null ? isAvailable : false);
                            checkEligibility();
                        } else {
                            cardDonorStatus.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void checkEligibility() {
        if (lastDonationDateStr == null || lastDonationDateStr.isEmpty()) {
            tvEligibilityMessage.setVisibility(View.GONE);
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date lastDonation = sdf.parse(lastDonationDateStr);
            if (lastDonation == null) return;

            long diffInMillis = Math.abs(System.currentTimeMillis() - lastDonation.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

            if (diffInDays < 90) {
                long remainingDays = 90 - diffInDays;
                tvEligibilityMessage.setVisibility(View.VISIBLE);
                tvEligibilityMessage.setText("You can donate again in " + remainingDays + " days.");
                if (switchAvailability.isChecked()) {
                    updateAvailability(false);
                    switchAvailability.setChecked(false);
                }
            } else {
                tvEligibilityMessage.setVisibility(View.GONE);
            }
        } catch (ParseException e) {
            tvEligibilityMessage.setVisibility(View.GONE);
        }
    }

    private boolean isEligibleToDonate() {
        if (lastDonationDateStr == null || lastDonationDateStr.isEmpty()) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date lastDonation = sdf.parse(lastDonationDateStr);
            if (lastDonation == null) return true;

            long diffInMillis = Math.abs(System.currentTimeMillis() - lastDonation.getTime());
            long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
            return diffInDays >= 90;
        } catch (ParseException e) {
            return true;
        }
    }

    private void updateAvailability(boolean available) {
        fStore.collection("users").document(currentUser.getUid())
                .update("available", available)
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update availability", Toast.LENGTH_SHORT).show());
    }

    private void loadQuickStats() {
        // Active Requests
        fStore.collection("blood_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvActiveRequestsCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                });

        // Available Donors
        fStore.collection("users")
                .whereEqualTo("role", "donor")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvAvailableDonorsCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }

    private void loadNearbyRequests() {
        pbRequests.setVisibility(View.VISIBLE);
        fStore.collection("blood_requests")
                .whereEqualTo("status", "pending")
                .orderBy("isEmergency", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener((value, error) -> {
                    pbRequests.setVisibility(View.GONE);
                    if (error != null) {
                        Log.e(TAG, "Error loading requests", error);
                        return;
                    }
                    if (value != null) {
                        requestList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            BloodRequest request = doc.toObject(BloodRequest.class);
                            if (request != null) {
                                request.setRequestId(doc.getId());
                                requestList.add(request);
                            }
                        }
                        requestAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
