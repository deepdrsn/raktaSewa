package com.example.raktasewa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.raktasewa.R;
import com.example.raktasewa.ui.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private TextView tvUserName, tvLastDonationDate;
    private Button btnEditProfile, btnLogDonation, btnViewHistory, btnManageRequests, btnLogout;
    private Button btnViewAcceptedRequests, btnRegisterAsDonor;
    private SwitchMaterial switchAvailable;
    private BottomNavigationView bottomNavigation;
    
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String lastDonationDateStr;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        initializeUI();
        loadUserProfile();
        setupClickListeners();
        setupBottomNavigation();
    }

    private void initializeUI() {
        tvUserName = findViewById(R.id.tv_user_name);
        tvLastDonationDate = findViewById(R.id.tv_last_donation_date);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogDonation = findViewById(R.id.btn_log_donation);
        btnViewHistory = findViewById(R.id.btn_view_donation_history);
        btnManageRequests = findViewById(R.id.btn_manage_my_requests);
        btnLogout = findViewById(R.id.btn_logout);
        switchAvailable = findViewById(R.id.switch_available_to_donate);
        btnViewAcceptedRequests = findViewById(R.id.btn_view_accepted_requests);
        btnRegisterAsDonor = findViewById(R.id.btn_register_as_donor);
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            }
            if (id == R.id.nav_donors) {
                startActivity(new Intent(this, DonorListActivity.class));
                return true;
            }
            if (id == R.id.nav_requests) {
                startActivity(new Intent(this, ViewRequestsActivity.class));
                return true;
            }
            if (id == R.id.nav_profile) return true;
            return false;
        });
    }

    private void loadUserProfile() {
        if (fAuth.getCurrentUser() == null) return;

        String userId = fAuth.getCurrentUser().getUid();
        DocumentReference docRef = fStore.collection("users").document(userId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvUserName.setText(documentSnapshot.getString("fullName"));
                userRole = documentSnapshot.getString("role");
                
                if ("donor".equals(userRole)) {
                    showDonorViews();
                    lastDonationDateStr = documentSnapshot.getString("lastDonatedDate");
                    if (lastDonationDateStr != null && !lastDonationDateStr.isEmpty()) {
                        tvLastDonationDate.setText("Last Donation: " + lastDonationDateStr);
                    } else {
                        tvLastDonationDate.setText("Last Donation: Never");
                    }
                    
                    Boolean isAvailable = documentSnapshot.getBoolean("available");
                    if (isAvailable != null) {
                        switchAvailable.setChecked(isAvailable);
                    }
                } else {
                    showSeekerViews();
                }
            }
        });
    }

    private void showDonorViews() {
        if (switchAvailable != null) switchAvailable.setVisibility(View.VISIBLE);
        if (btnViewAcceptedRequests != null) btnViewAcceptedRequests.setVisibility(View.VISIBLE);
        if (btnLogDonation != null) btnLogDonation.setVisibility(View.VISIBLE);
        if (btnViewHistory != null) btnViewHistory.setVisibility(View.VISIBLE);
        if (tvLastDonationDate != null) tvLastDonationDate.setVisibility(View.VISIBLE);
        if (btnRegisterAsDonor != null) btnRegisterAsDonor.setVisibility(View.GONE);
    }

    private void showSeekerViews() {
        if (switchAvailable != null) switchAvailable.setVisibility(View.GONE);
        if (btnViewAcceptedRequests != null) btnViewAcceptedRequests.setVisibility(View.GONE);
        if (btnLogDonation != null) btnLogDonation.setVisibility(View.GONE);
        if (btnViewHistory != null) btnViewHistory.setVisibility(View.GONE);
        if (tvLastDonationDate != null) tvLastDonationDate.setVisibility(View.GONE);
        if (btnRegisterAsDonor != null) btnRegisterAsDonor.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        btnViewAcceptedRequests.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, AcceptedRequestsActivity.class));
        });

        btnRegisterAsDonor.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("registerAsDonor", true);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            fAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnManageRequests.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, ManageRequestsActivity.class));
        });

        switchAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkEligibilityAndToggle(true);
            } else {
                updateAvailabilityInFirestore(false);
            }
        });

        btnLogDonation.setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
        btnViewHistory.setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }

    private void checkEligibilityAndToggle(boolean requestedState) {
        if (lastDonationDateStr == null || lastDonationDateStr.isEmpty()) {
            updateAvailabilityInFirestore(requestedState);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date lastDonationDate = sdf.parse(lastDonationDateStr);
            if (lastDonationDate != null) {
                long diffInMs = Math.abs(System.currentTimeMillis() - lastDonationDate.getTime());
                long diffInDays = TimeUnit.DAYS.convert(diffInMs, TimeUnit.MILLISECONDS);

                if (diffInDays < 90) {
                    long remainingDays = 90 - diffInDays;
                    Toast.makeText(this, "You can donate again in " + remainingDays + " days.", Toast.LENGTH_LONG).show();
                    switchAvailable.setChecked(false);
                } else {
                    updateAvailabilityInFirestore(requestedState);
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + lastDonationDateStr, e);
            updateAvailabilityInFirestore(requestedState);
        }
    }

    private void updateAvailabilityInFirestore(boolean isAvailable) {
        if (fAuth.getCurrentUser() == null) return;

        String userId = fAuth.getCurrentUser().getUid();
        fStore.collection("users").document(userId)
                .update("available", isAvailable)
                .addOnSuccessListener(aVoid -> {
                    String status = isAvailable ? "available" : "unavailable";
                    Toast.makeText(ProfileActivity.this, "You are now " + status, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    switchAvailable.setChecked(!isAvailable);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }
}
