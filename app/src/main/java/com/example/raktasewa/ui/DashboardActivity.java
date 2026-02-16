package com.example.raktasewa;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.raktasewa.ui.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardActivity extends AppCompatActivity {

    Button btnCreateRequest, btnViewRequests, btnDonorList, btnProfile, btnLogout;
    TextView tvWelcome;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeUI();
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        String userName = getIntent().getStringExtra("userName");
        if (userName != null) {
            tvWelcome.setText("Welcome, " + userName);
        } else {
            tvWelcome.setText("Welcome to RaktaSewa");
        }

        setupClickListeners();
    }

    private void initializeUI() {
        tvWelcome = findViewById(R.id.tvWelcome);
        btnCreateRequest = findViewById(R.id.btnCreateRequest);
        btnViewRequests = findViewById(R.id.btnViewRequests);
        btnDonorList = findViewById(R.id.btnDonorList);
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnCreateRequest.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, CreateRequestActivity.class));
        });

        btnViewRequests.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, ViewRequestsActivity.class));
        });

        btnDonorList.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, DonorListActivity.class));
        });

        btnProfile.setOnClickListener(v -> {
            Toast.makeText(DashboardActivity.this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            fAuth.signOut();
            Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getApplicationContext(), LandingActivity.class));
            finish();
        });
    }
}