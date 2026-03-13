package com.example.raktasewa.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {

    private static final String TAG = "ViewRequestsActivity";
    private static final int PAGE_LIMIT = 50; // Increased to allow better sorting client-side if needed

    private RecyclerView recyclerView;
    private RequestAdapter requestAdapter;
    private List<BloodRequest> requestList;
    private ProgressBar progressBar;
    private TextView tvEmptyState, tvFilterStatus;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button btnViewAllCenter, btnLoadMore;
    private FloatingActionButton fabCreateRequest;
    private Toolbar toolbar;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private String currentUserId;
    private String userBloodType = null;
    
    private DocumentSnapshot lastVisible = null;
    private boolean isViewAllMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        if (fAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = fAuth.getCurrentUser().getUid();

        initializeUI();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();

        // Start by getting user's blood type, then load requests
        fetchUserBloodType();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshData();
        });
    }

    private void initializeUI() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvFilterStatus = findViewById(R.id.tvFilterStatus);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        btnViewAllCenter = findViewById(R.id.btnViewAll); // Reusing ID from layout
        btnLoadMore = findViewById(R.id.btnLoadMore);
        fabCreateRequest = findViewById(R.id.fabCreateRequest);
        
        btnViewAllCenter.setVisibility(View.GONE);
        btnLoadMore.setVisibility(View.GONE);
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

    private void setupClickListeners() {
        btnViewAllCenter.setOnClickListener(v -> {
            isViewAllMode = true;
            tvFilterStatus.setText("Showing all requests");
            btnViewAllCenter.setVisibility(View.GONE);
            refreshData();
        });

        btnLoadMore.setOnClickListener(v -> loadRequests(true));

        fabCreateRequest.setOnClickListener(v -> startActivity(new Intent(this, CreateRequestActivity.class)));
    }

    private void fetchUserBloodType() {
        progressBar.setVisibility(View.VISIBLE);
        fStore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        userBloodType = doc.getString("bloodType");
                        if (userBloodType != null && !userBloodType.isEmpty()) {
                            tvFilterStatus.setText("Matching your blood group: " + userBloodType);
                            loadRequests(false);
                        } else {
                            enableViewAll("No blood group in your profile.");
                        }
                    } else {
                        enableViewAll("Profile not found.");
                    }
                })
                .addOnFailureListener(e -> enableViewAll("Error fetching profile."));
    }

    private void enableViewAll(String reason) {
        isViewAllMode = true;
        tvFilterStatus.setText("Showing all requests");
        loadRequests(false);
    }

    private void refreshData() {
        lastVisible = null;
        loadRequests(false);
    }

    private void loadRequests(boolean isLoadMore) {
        if (!isLoadMore) {
            progressBar.setVisibility(View.VISIBLE);
            requestList.clear();
            requestAdapter.notifyDataSetChanged();
            lastVisible = null;
            tvEmptyState.setVisibility(View.GONE);
        }

        // Ordering by isEmergency (true/false) and then by timestam
        // If index doesn't exist, it might fail. 
        // We'll use multiple orderBys to ensure emergency is on top.
        Query query = fStore.collection("blood_requests")
                .orderBy("isEmergency", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        // Apply blood type filter only if NOT in "View All" mode
        if (!isViewAllMode && userBloodType != null) {
            query = query.whereEqualTo("bloodType", userBloodType);
        }

        query = query.limit(PAGE_LIMIT);
        if (isLoadMore && lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (task.isSuccessful() && task.getResult() != null) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                
                if (!documents.isEmpty()) {
                    lastVisible = documents.get(documents.size() - 1);
                    for (DocumentSnapshot doc : documents) {
                        BloodRequest req = doc.toObject(BloodRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            requestList.add(req);
                        }
                    }
                    
                    requestAdapter.notifyDataSetChanged();
                    btnLoadMore.setVisibility(documents.size() == PAGE_LIMIT ? View.VISIBLE : View.GONE);
                } else if (!isLoadMore) {
                    // No results at all
                    tvEmptyState.setVisibility(View.VISIBLE);
                    if (!isViewAllMode) {
                        tvEmptyState.setText("No requests for your blood group (" + userBloodType + ").");
                        btnViewAllCenter.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyState.setText("No blood requests found in the system.");
                    }
                }
            } else {
                Toast.makeText(this, "Failed to load: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Query error", task.getException());
                
                // Fallback: If composite index is missing, try loading without sorting by emergency first
                if (task.getException() != null && task.getException().getMessage().contains("FAILED_PRECONDITION")) {
                   loadRequestsWithoutSorting(isLoadMore);
                }
            }
        });
    }

    private void loadRequestsWithoutSorting(boolean isLoadMore) {
        // This is a fallback if the composite index hasn't been created yet.
        Query query = fStore.collection("blood_requests")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (!isViewAllMode && userBloodType != null) {
            query = query.whereEqualTo("bloodType", userBloodType);
        }

        query = query.limit(PAGE_LIMIT);
        if (isLoadMore && lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<DocumentSnapshot> documents = task.getResult().getDocuments();
                if (!documents.isEmpty()) {
                    lastVisible = documents.get(documents.size() - 1);
                    for (DocumentSnapshot doc : documents) {
                        BloodRequest req = doc.toObject(BloodRequest.class);
                        if (req != null) {
                            req.setRequestId(doc.getId());
                            requestList.add(req);
                        }
                    }
                    // Client side sort as fallback
                    Collections.sort(requestList, (o1, o2) -> {
                        if (o1.isEmergency() != o2.isEmergency()) {
                            return o1.isEmergency() ? -1 : 1;
                        }
                        return Long.compare(o2.getTimestamp(), o1.getTimestamp());
                    });
                    requestAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
