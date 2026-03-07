package com.example.raktasewa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.raktasewa.ui.CreateRequestActivity;
import com.example.raktasewa.ui.DonorListActivity;
import com.example.raktasewa.ui.ProfileActivity;
import com.example.raktasewa.R;
import com.example.raktasewa.ui.ViewRequestsActivity;
import com.example.raktasewa.ui.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    // UI Components
    private TextView tvWelcome;
    private Button btnCreateRequest, btnViewRequests, btnDonorList, btnProfile, btnLogout;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Firebase Services
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize Firebase services first
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();

        // 2. Initialize UI Components
        initializeUI();

        // 3. Check if user is logged in
        if (currentUser == null) {
            // If no user is logged in, redirect to login and finish this activity
            navigateToLogin();
            return; // Stop further execution in onCreate
        }

        // 4. Fetch user data and set up the UI accordingly
        loadUserProfile();

        // 5. Set up click listeners for buttons
        setupClickListeners();
    }

    private void initializeUI() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnCreateRequest = findViewById(R.id.btnCreateRequest);
        btnViewRequests = findViewById(R.id.btnViewRequests);
        btnDonorList = findViewById(R.id.btnDonorList);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);

        // Check if swipeRefreshLayout exists in your layout
        // If it doesn't exist, this will be null but won't crash if we check before using
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void loadUserProfile() {
        // Only use swipeRefreshLayout if it exists
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        DocumentReference docRef = fStore.collection("users").document(currentUser.getUid());

        docRef.get().addOnCompleteListener(task -> {
            // Hide refresh indicator if it exists
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }

            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    String name = document.getString("fullName");
                    String role = document.getString("role");

                    tvWelcome.setText("Welcome, " + (name != null ? name : "User"));
                    updateUIVisibility(role); // Update buttons based on role
                } else {
                    Log.d(TAG, "No such document");
                    tvWelcome.setText("Welcome, User!");
                    // Handle case where user exists in Auth but not in Firestore
                    updateUIVisibility(null); // Hide role-specific buttons
                }
            } else {
                Log.d(TAG, "get failed with ", task.getException());
                Toast.makeText(DashboardActivity.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                tvWelcome.setText("Welcome, User!");
            }
        });
    }

    private void updateUIVisibility(String role) {
        // Hide all role-specific buttons by default
        btnCreateRequest.setVisibility(View.GONE);
        btnViewRequests.setVisibility(View.GONE);
        btnDonorList.setVisibility(View.GONE);

        if (role == null) return;

        // Show buttons based on the user's role
        if ("seeker".equalsIgnoreCase(role)) {
            btnCreateRequest.setVisibility(View.VISIBLE);
            btnDonorList.setVisibility(View.VISIBLE);
        } else if ("donor".equalsIgnoreCase(role)) {
            btnViewRequests.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        btnCreateRequest.setOnClickListener(v -> startActivity(new Intent(this, CreateRequestActivity.class)));
        btnViewRequests.setOnClickListener(v -> startActivity(new Intent(this, ViewRequestsActivity.class)));
        btnDonorList.setOnClickListener(v -> startActivity(new Intent(this, DonorListActivity.class)));

        btnProfile.setOnClickListener(v -> {
            // Navigate to ProfileActivity (unchanged)
            startActivity(new Intent(this, ProfileActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            fAuth.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        });

        // Only set refresh listener if swipeRefreshLayout exists
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadUserProfile);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        // Clear the activity stack to prevent user from going back to dashboard after logging out
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}