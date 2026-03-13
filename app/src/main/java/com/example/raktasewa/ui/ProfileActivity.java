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
        switchAvailable.setVisibility(View.VISIBLE);
        btnViewAcceptedRequests.setVisibility(View.VISIBLE);
        btnLogDonation.setVisibility(View.VISIBLE);
        btnViewHistory.setVisibility(View.VISIBLE);
        tvLastDonationDate.setVisibility(View.VISIBLE);
        btnRegisterAsDonor.setVisibility(View.GONE);
    }

    private void showSeekerViews() {
        switchAvailable.setVisibility(View.GONE);
        btnViewAcceptedRequests.setVisibility(View.GONE);
        btnLogDonation.setVisibility(View.GONE);
        btnViewHistory.setVisibility(View.GONE);
        tvLastDonationDate.setVisibility(View.GONE);
        btnRegisterAsDonor.setVisibility(View.VISIBLE);
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        btnViewAcceptedRequests.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, AcceptedRequestsActivity.class));
        });

        btnRegisterAsDonor.setOnClickListener(v -> {
            // Navigate to EditProfile but with intention to register as donor
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
