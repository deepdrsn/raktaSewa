package com.example.raktasewa.ui;

import android.content.Intent;
import android.os.Bundle;
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

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUserName, tvLastDonationDate;
    private Button btnEditProfile, btnLogDonation, btnViewHistory, btnManageRequests, btnLogout;
    private SwitchMaterial switchAvailable;
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

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
    }

    private void loadUserProfile() {
        if (fAuth.getCurrentUser() == null) return;

        String userId = fAuth.getCurrentUser().getUid();
        DocumentReference docRef = fStore.collection("users").document(userId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvUserName.setText(documentSnapshot.getString("fullName"));
                String lastDonation = documentSnapshot.getString("lastDonationDate");
                if (lastDonation != null) {
                    tvLastDonationDate.setText("Last Donation: " + lastDonation);
                }
                Boolean isAvailable = documentSnapshot.getBoolean("available");
                if (isAvailable != null) {
                    switchAvailable.setChecked(isAvailable);
                }
            }
        });
    }

    private void setupClickListeners() {
        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
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

        // Other buttons can be implemented as needed
        btnLogDonation.setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
        btnViewHistory.setOnClickListener(v -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
    }
}