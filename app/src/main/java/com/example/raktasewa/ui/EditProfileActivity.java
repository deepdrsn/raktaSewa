package com.example.raktasewa.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
    private TextView tvLocationStatus, tvBloodTypeLabel;
    private Spinner spinnerBloodType;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser currentUser;
    private String userId;
    private FusedLocationProviderClient fusedLocationClient;
    private Double latitude = null, longitude = null;
    private boolean registerAsDonor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        currentUser = fAuth.getCurrentUser();
        userId = currentUser.getUid();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if we are here to upgrade to donor
        registerAsDonor = getIntent().getBooleanExtra("registerAsDonor", false);

        initializeUI();
        setupSpinner();
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
        tvBloodTypeLabel = findViewById(R.id.tv_blood_type_label);
        spinnerBloodType = findViewById(R.id.spinner_blood_type);
        
        if (registerAsDonor) {
            btnSaveChanges.setText("Register as Donor");
            tvBloodTypeLabel.setVisibility(View.VISIBLE);
            spinnerBloodType.setVisibility(View.VISIBLE);
            btnUpdateLocation.setVisibility(View.VISIBLE);
            tvLocationStatus.setVisibility(View.VISIBLE);
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(bloodAdapter);
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
                    btnUpdateLocation.setVisibility(View.VISIBLE);
                    tvLocationStatus.setVisibility(View.VISIBLE);
                    tvBloodTypeLabel.setVisibility(View.VISIBLE);
                    spinnerBloodType.setVisibility(View.VISIBLE);
                    
                    latitude = documentSnapshot.getDouble("latitude");
                    longitude = documentSnapshot.getDouble("longitude");
                    
                    String bloodType = documentSnapshot.getString("bloodType");
                    if (bloodType != null) {
                        ArrayAdapter adapter = (ArrayAdapter) spinnerBloodType.getAdapter();
                        int pos = adapter.getPosition(bloodType);
                        spinnerBloodType.setSelection(pos);
                    }

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
                Toast.makeText(this, "Location captured", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not get location. Is GPS on?", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileChanges() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String bloodType = spinnerBloodType.getSelectedItem().toString();

        if (fullName.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if ((registerAsDonor || spinnerBloodType.getVisibility() == View.VISIBLE) && bloodType.equals("Select Blood Type")) {
            Toast.makeText(this, "Please select your blood type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (registerAsDonor && (latitude == null || longitude == null)) {
            Toast.makeText(this, "Location is required to register as a donor", Toast.LENGTH_LONG).show();
            return;
        }

        DocumentReference docRef = fStore.collection("users").document(userId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        
        if (registerAsDonor) {
            updates.put("role", "donor");
            updates.put("available", true);
        }
        
        if (spinnerBloodType.getVisibility() == View.VISIBLE) {
            updates.put("bloodType", bloodType);
        }
        
        if (latitude != null) updates.put("latitude", latitude);
        if (longitude != null) updates.put("longitude", longitude);

        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    String msg = registerAsDonor ? "Registered as Donor successfully" : "Profile updated successfully";
                    Toast.makeText(EditProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
