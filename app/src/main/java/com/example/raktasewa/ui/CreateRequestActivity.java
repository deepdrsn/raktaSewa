package com.example.raktasewa.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateRequestActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    EditText etPatientName, etHospital, etUnits, etContact, etAdditionalInfo;
    Spinner spinnerBloodType, spinnerRequestType;
    Button btnSubmitRequest, btnGetLocation;
    TextView tvLocationStatus;
    ProgressBar progressBar;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userId;

    private FusedLocationProviderClient fusedLocationClient;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private String currentAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_request);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeUI();
        setupSpinners();

        btnSubmitRequest.setOnClickListener(v -> submitRequest());
        btnGetLocation.setOnClickListener(v -> checkLocationPermission());
    }

    private void initializeUI() {
        etPatientName = findViewById(R.id.etPatientName);
        etHospital = findViewById(R.id.etHospital);
        etUnits = findViewById(R.id.etUnits);
        etContact = findViewById(R.id.etContact);
        etAdditionalInfo = findViewById(R.id.etAdditionalInfo);
        spinnerBloodType = findViewById(R.id.spinnerBloodType);
        spinnerRequestType = findViewById(R.id.spinnerRequestType);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(bloodAdapter);

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.request_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRequestType.setAdapter(typeAdapter);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        tvLocationStatus.setText("Fetching location...");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                getAddressFromLocation(latitude, longitude);
            } else {
                tvLocationStatus.setText("Could not get location. Try again.");
                Toast.makeText(this, "Unable to find location. Is GPS on?", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getAddressFromLocation(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                currentAddress = address.getAddressLine(0);
                tvLocationStatus.setText("Location: " + currentAddress);
            } else {
                tvLocationStatus.setText("Location set (Coordinates: " + lat + ", " + lon + ")");
            }
        } catch (IOException e) {
            tvLocationStatus.setText("Location set (Coordinates: " + lat + ", " + lon + ")");
            e.printStackTrace();
        }
    }

    private void submitRequest() {
        String patientName = etPatientName.getText().toString().trim();
        String hospital = etHospital.getText().toString().trim();
        String units = etUnits.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String additionalInfo = etAdditionalInfo.getText().toString().trim();
        String bloodType = spinnerBloodType.getSelectedItem().toString();
        String requestType = spinnerRequestType.getSelectedItem().toString();

        if (TextUtils.isEmpty(patientName)) {
            etPatientName.setError("Patient name is required");
            return;
        }
        if (TextUtils.isEmpty(hospital)) {
            etHospital.setError("Hospital name is required");
            return;
        }
        if (TextUtils.isEmpty(units)) {
            etUnits.setError("Units required");
            return;
        }
        if (bloodType.equals("Select Blood Type")) {
            Toast.makeText(this, "Please select blood type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (latitude == 0.0 || longitude == 0.0) {
            Toast.makeText(this, "Please get current location before submitting", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("patientName", patientName);
        request.put("hospital", hospital);
        request.put("units", Integer.parseInt(units));
        request.put("contact", contact);
        request.put("additionalInfo", additionalInfo);
        request.put("bloodType", bloodType);
        request.put("requestType", requestType);
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());
        request.put("isEmergency", requestType.equals("Emergency"));
        
        // Location data
        request.put("latitude", latitude);
        request.put("longitude", longitude);
        request.put("address", currentAddress);

        fStore.collection("blood_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CreateRequestActivity.this,
                            "Request submitted successfully", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateRequestActivity.this,
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }
}