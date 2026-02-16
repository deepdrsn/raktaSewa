package com.example.raktasewa;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RequestAdapter requestAdapter;
    private List<BloodRequest> requestList;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnAll, btnPending, btnApproved, btnCompleted, btnEmergency;
    private FloatingActionButton fabCreateRequest;
    private Toolbar toolbar;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String userId;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        initializeUI();
        setupToolbar();
        setupRecyclerView();
        setupFilterButtons();

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        // Check if user is logged in
        if (fAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = fAuth.getCurrentUser().getUid();

        loadRequests();

        swipeRefreshLayout.setOnRefreshListener(this::loadRequests);
        fabCreateRequest.setOnClickListener(v -> {
            // Start CreateRequestActivity
            startActivity(new Intent(this, CreateRequestActivity.class));
        });
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnAll = findViewById(R.id.btnAll);
        btnPending = findViewById(R.id.btnPending);
        btnApproved = findViewById(R.id.btnApproved);
        btnCompleted = findViewById(R.id.btnCompleted);
        btnEmergency = findViewById(R.id.btnEmergency);
        fabCreateRequest = findViewById(R.id.fabCreateRequest);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Blood Requests");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupRecyclerView() {
        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, requestList, userId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(requestAdapter);
        recyclerView.setHasFixedSize(true);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> {
            currentFilter = "all";
            loadRequests();
            updateButtonStates(btnAll);
        });

        btnPending.setOnClickListener(v -> {
            currentFilter = "pending";
            loadRequests();
            updateButtonStates(btnPending);
        });

        btnApproved.setOnClickListener(v -> {
            currentFilter = "approved";
            loadRequests();
            updateButtonStates(btnApproved);
        });

        btnCompleted.setOnClickListener(v -> {
            currentFilter = "completed";
            loadRequests();
            updateButtonStates(btnCompleted);
        });

        btnEmergency.setOnClickListener(v -> {
            currentFilter = "emergency";
            loadRequests();
            updateButtonStates(btnEmergency);
        });

        // Set initial state
        updateButtonStates(btnAll);
    }

    private void updateButtonStates(Button selectedButton) {
        // Reset all buttons to default color
        btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
        btnPending.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
        btnApproved.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
        btnCompleted.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary)));
        btnEmergency.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.emergency)));

        // Highlight selected button
        selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_dark)));
    }

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        requestList.clear();

        Query query = fStore.collection("blood_requests")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Apply filter
        switch (currentFilter) {
            case "pending":
                query = fStore.collection("blood_requests")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("status", "pending")
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                break;
            case "approved":
                query = fStore.collection("blood_requests")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("status", "approved")
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                break;
            case "completed":
                query = fStore.collection("blood_requests")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("status", "completed")
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                break;
            case "emergency":
                query = fStore.collection("blood_requests")
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("isEmergency", true)
                        .orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }

        query.get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            BloodRequest request = document.toObject(BloodRequest.class);
                            request.setRequestId(document.getId());
                            requestList.add(request);
                        }

                        requestAdapter.notifyDataSetChanged();

                        if (requestList.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            if (currentFilter.equals("all")) {
                                tvEmptyState.setText("No blood requests yet.\nTap the + button to create one.");
                            } else {
                                tvEmptyState.setText("No requests found for this filter.");
                            }
                        }
                    } else {
                        Toast.makeText(ViewRequestsActivity.this,
                                "Error loading requests: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}