package com.example.raktasewa;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateRequestActivity extends AppCompatActivity {

    EditText etPatientName, etHospital, etUnits, etContact, etAdditionalInfo;
    Spinner spinnerBloodType, spinnerRequestType;
    Button btnSubmitRequest;
    ProgressBar progressBar;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_request);

        initializeUI();
        setupSpinners();

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        userId = fAuth.getCurrentUser().getUid();

        btnSubmitRequest.setOnClickListener(v -> submitRequest());
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
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        // Blood Type Spinner
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(bloodAdapter);

        // Request Type Spinner
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(this,
                R.array.request_types, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRequestType.setAdapter(typeAdapter);
    }

    private void submitRequest() {
        String patientName = etPatientName.getText().toString().trim();
        String hospital = etHospital.getText().toString().trim();
        String units = etUnits.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String additionalInfo = etAdditionalInfo.getText().toString().trim();
        String bloodType = spinnerBloodType.getSelectedItem().toString();
        String requestType = spinnerRequestType.getSelectedItem().toString();

        // Validation
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

        progressBar.setVisibility(View.VISIBLE);

        // Create request object
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

        // Save to Firestore
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