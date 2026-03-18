package com.example.raktasewa.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.raktasewa.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestDetailsActivity extends AppCompatActivity {

    private String requestId;
    private BloodRequest bloodRequest;
    private FirebaseFirestore fStore;
    private FirebaseAuth fAuth;
    private String currentUserId;
    private String currentUserName;
    private String userBloodType;

    private TextView tvBloodType, tvPatientName, tvHospital, tvAddress, tvUnits, tvDate, tvRequesterInfo, tvAdditionalInfo, tvDonorInfo, labelAdditionalInfo;
    private Chip chipStatus, chipEmergency;
    private MaterialButton btnAccept, btnContact, btnFulfill;
    private ProgressBar progressBar;

    private static final String VERCEL_URL = "https://raktasewa-notification-server.vercel.app/api/notify";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_details);

        requestId = getIntent().getStringExtra("requestId");
        if (requestId == null || requestId.isEmpty()) {
            Toast.makeText(this, "Invalid Request ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        if (fAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        currentUserId = fAuth.getCurrentUser().getUid();

        initializeUI();
        fetchCurrentUserDetails();
        loadRequestDetails();
    }

    private void initializeUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Request Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvBloodType = findViewById(R.id.tvBloodType);
        tvPatientName = findViewById(R.id.tvPatientName);
        tvHospital = findViewById(R.id.tvHospital);
        tvAddress = findViewById(R.id.tvAddress);
        tvUnits = findViewById(R.id.tvUnits);
        tvDate = findViewById(R.id.tvDate);
        tvRequesterInfo = findViewById(R.id.tvRequesterInfo);
        tvAdditionalInfo = findViewById(R.id.tvAdditionalInfo);
        tvDonorInfo = findViewById(R.id.tvDonorInfo);
        labelAdditionalInfo = findViewById(R.id.labelAdditionalInfo);
        chipStatus = findViewById(R.id.chipStatus);
        chipEmergency = findViewById(R.id.chipEmergency);
        btnAccept = findViewById(R.id.btnAccept);
        btnContact = findViewById(R.id.btnContact);
        btnFulfill = findViewById(R.id.btnFulfill);
        progressBar = findViewById(R.id.progressBar);

        tvAddress.setOnClickListener(v -> {
            String addressText = tvAddress.getText().toString();
            if (!addressText.isEmpty() && !addressText.equals("Full Address here")) {
                copyToClipboard(addressText);
            }
        });
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Hospital Address", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Address copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchCurrentUserDetails() {
        fStore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userBloodType = documentSnapshot.getString("bloodType");
                        currentUserName = documentSnapshot.getString("fullName");
                    }
                });
    }

    private void loadRequestDetails() {
        progressBar.setVisibility(View.VISIBLE);
        fStore.collection("blood_requests").document(requestId)
                .addSnapshotListener((snapshot, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        bloodRequest = snapshot.toObject(BloodRequest.class);
                        if (bloodRequest != null) {
                            bloodRequest.setRequestId(snapshot.getId());
                            displayDetails();
                        }
                    } else {
                        Toast.makeText(this, "Request not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void displayDetails() {
        tvBloodType.setText(bloodRequest.getBloodType());
        tvPatientName.setText(bloodRequest.getPatientName());
        tvHospital.setText(bloodRequest.getHospital());
        tvAddress.setText(bloodRequest.getAddress() != null ? bloodRequest.getAddress() : "N/A");
        tvUnits.setText(bloodRequest.getUnits() + " Units");

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(bloodRequest.getTimestamp())));

        if (bloodRequest.getAdditionalInfo() != null && !bloodRequest.getAdditionalInfo().isEmpty()) {
            tvAdditionalInfo.setText(bloodRequest.getAdditionalInfo());
            tvAdditionalInfo.setVisibility(View.VISIBLE);
            labelAdditionalInfo.setVisibility(View.VISIBLE);
        } else {
            tvAdditionalInfo.setVisibility(View.GONE);
            labelAdditionalInfo.setVisibility(View.GONE);
        }

        chipEmergency.setVisibility(bloodRequest.isEmergency() ? View.VISIBLE : View.GONE);
        updateStatusChip(bloodRequest.getStatus());

        fetchUserName(bloodRequest.getUserId(), tvRequesterInfo, "Requested by: ");

        boolean isRequester = currentUserId.equals(bloodRequest.getUserId());
        boolean isDonor = currentUserId.equals(bloodRequest.getDonorId());
        String status = bloodRequest.getStatus();

        btnAccept.setVisibility(View.GONE);
        btnContact.setVisibility(View.GONE);
        btnFulfill.setVisibility(View.GONE);
        tvDonorInfo.setVisibility(View.GONE);

        if ("pending".equalsIgnoreCase(status)) {
            if (!isRequester) {
                btnAccept.setVisibility(View.VISIBLE);
                btnAccept.setOnClickListener(v -> showAcceptConfirmationDialog());
                
                btnContact.setVisibility(View.VISIBLE);
                btnContact.setOnClickListener(v -> dialNumber(bloodRequest.getContact()));
            }
        } else if ("accepted".equalsIgnoreCase(status)) {
            tvDonorInfo.setVisibility(View.VISIBLE);
            fetchUserName(bloodRequest.getDonorId(), tvDonorInfo, "Accepted by: ");

            if (isRequester) {
                btnFulfill.setVisibility(View.VISIBLE);
                btnFulfill.setOnClickListener(v -> fulfillRequest());
                
                if (bloodRequest.getDonorId() != null) {
                    btnContact.setVisibility(View.VISIBLE);
                    btnContact.setText("Contact Donor");
                    btnContact.setOnClickListener(v -> {
                        fStore.collection("users").document(bloodRequest.getDonorId()).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        dialNumber(doc.getString("phone"));
                                    }
                                });
                    });
                }
            } else if (isDonor) {
                btnContact.setVisibility(View.VISIBLE);
                btnContact.setText("Contact Requester");
                btnContact.setOnClickListener(v -> dialNumber(bloodRequest.getContact()));
            }
        } else if ("fulfilled".equalsIgnoreCase(status)) {
            tvDonorInfo.setVisibility(View.VISIBLE);
            fetchUserName(bloodRequest.getDonorId(), tvDonorInfo, "Fulfilled by: ");
        }
    }

    private void dialNumber(String phone) {
        if (phone != null && !phone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatusChip(String status) {
        if (status == null) status = "pending";
        chipStatus.setText(status.toUpperCase());
        switch (status.toLowerCase()) {
            case "pending":
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_dark);
                break;
            case "accepted":
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_blue_dark);
                break;
            case "fulfilled":
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark);
                break;
            default:
                chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
        }
    }

    private void fetchUserName(String userId, TextView tv, String prefix) {
        fStore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tv.setText(prefix + documentSnapshot.getString("fullName"));
                    } else {
                        tv.setText(prefix + "Unknown User");
                    }
                });
    }

    private void showAcceptConfirmationDialog() {
        fStore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lastDonated = documentSnapshot.getString("lastDonatedDate");
                        boolean isAvailable = documentSnapshot.getBoolean("available") != null && documentSnapshot.getBoolean("available");

                        if (!isAvailable || !isEligibleToDonate(lastDonated)) {
                            Toast.makeText(this, "You are not eligible to donate yet.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        if (userBloodType == null || !userBloodType.equalsIgnoreCase(bloodRequest.getBloodType())) {
                            Toast.makeText(this, "Blood group mismatch (" + userBloodType + ").", Toast.LENGTH_LONG).show();
                            return;
                        }

                        new AlertDialog.Builder(this)
                                .setTitle("Accept Request?")
                                .setMessage("Are you sure you want to accept this request?")
                                .setPositiveButton("Accept", (dialog, which) -> acceptRequest())
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                });
    }

    private boolean isEligibleToDonate(String lastDonatedDateStr) {
        if (lastDonatedDateStr == null || lastDonatedDateStr.isEmpty()) return true;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date lastDate = sdf.parse(lastDonatedDateStr);
            if (lastDate == null) return true;
            long diffInMillis = System.currentTimeMillis() - lastDate.getTime();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            return diffInDays >= 90;
        } catch (ParseException e) {
            return true;
        }
    }

    private void acceptRequest() {
        fStore.collection("blood_requests").document(requestId)
                .update("status", "accepted", "donorId", currentUserId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request Accepted!", Toast.LENGTH_SHORT).show();
                    notifyRequester(bloodRequest.getUserId(), currentUserName);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fulfillRequest() {
        long timestamp = System.currentTimeMillis();
        fStore.collection("blood_requests").document(requestId)
                .update("status", "fulfilled", "fulfilledTimestamp", timestamp)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Marked as Fulfilled!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void notifyRequester(String requesterId, String donorName) {
        String title = "Request Accepted!";
        String body = donorName + " has accepted your blood request.";
        
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);
        notification.put("type", "acceptance");
        notification.put("requestId", requestId);

        fStore.collection("users").document(requesterId)
                .collection("notifications").add(notification);
        
        fStore.collection("users").document(requesterId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String token = doc.getString("fcmToken");
                        if (token != null) {
                            triggerPushNotification(token, title, body);
                        }
                    }
                });
    }

    private void triggerPushNotification(String token, String title, String body) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            json.put("title", title);
            json.put("body", body);
            json.put("tokens", new JSONArray(Collections.singletonList(token)));
            
            JSONObject data = new JSONObject();
            data.put("requestId", requestId);
            data.put("type", "acceptance");
            json.put("data", data);
            
        } catch (JSONException e) {
            return;
        }

        RequestBody reqBody = RequestBody.create(json.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(VERCEL_URL).post(reqBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                response.close();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
