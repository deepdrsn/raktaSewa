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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    private TextView tvWelcome;
    private Button btnCreateRequest, btnViewRequests, btnDonorList, btnProfile, btnLogout;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser currentUser;

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
        
        // Show all buttons immediately so they don't disappear
        showAllButtons();
        
        // Setup click listeners immediately
        setupClickListeners();
        
        // Load profile in background to update welcome message
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
                }
            } else {
                Log.d(TAG, "Profile fetch failed: " + task.getException());
            }
        });
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