package com.example.raktasewa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.raktasewa.R;
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
    private String currentUserId;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        if (fAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = fAuth.getCurrentUser().getUid();

        initializeUI();
        setupToolbar();
        setupRecyclerView();
        setupFilterButtons();

        loadRequests();

        swipeRefreshLayout.setOnRefreshListener(this::loadRequests);
        fabCreateRequest.setOnClickListener(v -> {
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
        }
    }

    private void setupRecyclerView() {
        requestList = new ArrayList<>();
        requestAdapter = new RequestAdapter(this, requestList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(requestAdapter);
    }

    private void setupFilterButtons() {
        btnAll.setOnClickListener(v -> { currentFilter = "all"; loadRequests(); updateButtonStates(btnAll); });
        btnPending.setOnClickListener(v -> { currentFilter = "pending"; loadRequests(); updateButtonStates(btnPending); });
        btnApproved.setOnClickListener(v -> { currentFilter = "approved"; loadRequests(); updateButtonStates(btnApproved); });
        btnCompleted.setOnClickListener(v -> { currentFilter = "completed"; loadRequests(); updateButtonStates(btnCompleted); });
        btnEmergency.setOnClickListener(v -> { currentFilter = "emergency"; loadRequests(); updateButtonStates(btnEmergency); });
        updateButtonStates(btnAll);
    }

    private void updateButtonStates(Button selectedButton) {
        int primaryColor = getResources().getColor(R.color.primary);
        int darkColor = getResources().getColor(R.color.primary_dark);
        
        btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        btnPending.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        btnApproved.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        btnCompleted.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        btnEmergency.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.emergency)));

        selectedButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(darkColor));
    }

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        requestList.clear();

        // CHANGED: Don't filter by userId here, show ALL requests for donors
        Query query = fStore.collection("blood_requests")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (!currentFilter.equals("all")) {
            if (currentFilter.equals("emergency")) {
                query = query.whereEqualTo("isEmergency", true);
            } else {
                query = query.whereEqualTo("status", currentFilter);
            }
        }

        query.get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    BloodRequest request = document.toObject(BloodRequest.class);
                    request.setRequestId(document.getId());
                    requestList.add(request);
                }
                requestAdapter.notifyDataSetChanged();
                if (requestList.isEmpty()) tvEmptyState.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}