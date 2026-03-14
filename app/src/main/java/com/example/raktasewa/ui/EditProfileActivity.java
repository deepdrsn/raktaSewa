package com.example.raktasewa.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.raktasewa.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone;
    private Button btnSaveChanges, btnUpdateLocation;
    private TextView tvLocationStatus;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser currentUser;
    private String userId;
    private FusedLocationProviderClient fusedLocationClient;
    private Double latitude = null, longitude = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();
        userId = currentUser.getUid();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeUI();
        loadUserProfile();

        btnUpdateLocation.setOnClickListener(v -> checkLocationPermission());
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
    }

    private void initializeUI() {
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnUpdateLocation = findViewById(R.id.btnUpdateLocation);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        
        // Ensure buttons exist in the layout before setting visibility
        if (btnUpdateLocation != null) btnUpdateLocation.setVisibility(View.GONE);
    }

    private void loadUserProfile() {
        DocumentReference docRef = fStore.collection("users").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                etFullName.setText(documentSnapshot.getString("fullName"));
                etEmail.setText(documentSnapshot.getString("email"));
                etPhone.setText(documentSnapshot.getString("phone"));
                
                String role = documentSnapshot.getString("role");
                if ("donor".equals(role)) {
                    if (btnUpdateLocation != null) btnUpdateLocation.setVisibility(View.VISIBLE);
                    if (tvLocationStatus != null) tvLocationStatus.setVisibility(View.VISIBLE);
                    
                    latitude = documentSnapshot.getDouble("latitude");
                    longitude = documentSnapshot.getDouble("longitude");
                    
                    if (latitude != null && longitude != null) {
                        tvLocationStatus.setText("Location already saved");
                        tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    }
                }
            }
        });
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1003);
        } else {
            fetchLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1003 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                tvLocationStatus.setText("New location captured!");
                tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not get location. Is GPS on?", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileChanges() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRef = fStore.collection("users").document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("email", email);
        updates.put("phone", phone);
        
        if (latitude != null) updates.put("latitude", latitude);
        if (longitude != null) updates.put("longitude", longitude);

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
