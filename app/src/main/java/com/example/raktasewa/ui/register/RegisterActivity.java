package com.example.raktasewa.ui.register;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.example.raktasewa.ui.login.LoginActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    public static final String TAG = "RegisterDonorActivity";

    EditText etName, etEmail, etPassword, etPhone, etAddress, etLastDonated;
    Spinner spinnerBloodType, spinnerGender;
    Button btnRegister, btnGetLocation;
    TextView tvLogin, tvLocationStatus;
    ProgressBar progressBar;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    FusedLocationProviderClient fusedLocationClient;
    Double latitude = null, longitude = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etLastDonated = findViewById(R.id.etLastDonated);
        spinnerBloodType = findViewById(R.id.spinnerBloodType);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnRegister = findViewById(R.id.btnRegister);
        btnGetLocation = findViewById(R.id.btnGetLocation);
        tvLogin = findViewById(R.id.tvLogin);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        progressBar = findViewById(R.id.progressBar);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupSpinners();
        
        etLastDonated.setFocusable(false);
        etLastDonated.setOnClickListener(v -> showDatePickerDialog());

        btnGetLocation.setOnClickListener(v -> checkLocationPermission());
        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String date = String.format(Locale.getDefault(), "%d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    etLastDonated.setText(date);
                }, year, month, day);
        
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(bloodAdapter);

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1002);
        } else {
            fetchLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1002 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            Toast.makeText(this, "Permission denied. Location is required for donor search.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            progressBar.setVisibility(View.GONE);
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                tvLocationStatus.setText("Location Captured Successfully");
                tvLocationStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                Toast.makeText(this, "Location detected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Could not get location. Make sure GPS is on.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void registerUser() {
        String nameStr = etName.getText().toString().trim();
        String emailStr = etEmail.getText().toString().trim();
        String passwordStr = etPassword.getText().toString().trim();
        String phoneStr = etPhone.getText().toString().trim();
        String addressStr = etAddress.getText().toString().trim();
        String lastDonatedStr = etLastDonated.getText().toString().trim();
        String bloodTypeStr = spinnerBloodType.getSelectedItem().toString();
        String genderStr = spinnerGender.getSelectedItem().toString();

        if (TextUtils.isEmpty(nameStr)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(emailStr)) { etEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(passwordStr) || passwordStr.length() < 6) { etPassword.setError(">= 6 chars"); return; }
        if (TextUtils.isEmpty(phoneStr)) { etPhone.setError("Required"); return; }
        if (latitude == null || longitude == null) {
            Toast.makeText(this, "Please capture your location first", Toast.LENGTH_LONG).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        fAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fUser = fAuth.getCurrentUser();
                        fUser.sendEmailVerification();

                        String userID = fUser.getUid();
                        
                        // Fetch FCM Token before saving user to Firestore
                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tokenTask -> {
                            String fcmToken = tokenTask.isSuccessful() ? tokenTask.getResult() : null;
                            saveUserToFirestore(userID, nameStr, emailStr, phoneStr, addressStr, bloodTypeStr, genderStr, lastDonatedStr, fcmToken);
                        });
                        
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error! " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void saveUserToFirestore(String userID, String name, String email, String phone, String address, String blood, String gender, String lastDonated, String fcmToken) {
        DocumentReference documentReference = fStore.collection("users").document(userID);

        boolean isEligible = isEligibleToDonate(lastDonated);

        Map<String, Object> user = new HashMap<>();
        user.put("role", "donor");
        user.put("fullName", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("address", address);
        user.put("bloodType", blood);
        user.put("gender", gender);
        user.put("lastDonatedDate", lastDonated);
        user.put("latitude", latitude);
        user.put("longitude", longitude);
        user.put("available", isEligible); // Set availability based on date
        if (fcmToken != null) user.put("fcmToken", fcmToken);

        documentReference.set(user).addOnSuccessListener(aVoid -> {
            if (!TextUtils.isEmpty(lastDonated)) {
                Map<String, Object> historyRecord = new HashMap<>();
                historyRecord.put("date", lastDonated);
                historyRecord.put("timestamp", System.currentTimeMillis());
                documentReference.collection("donation_history").add(historyRecord);
            }
            
            Toast.makeText(RegisterActivity.this, "Donor Registered Successfully", Toast.LENGTH_LONG).show();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finishAffinity();
        });
    }

    private boolean isEligibleToDonate(String lastDonatedDateStr) {
        if (TextUtils.isEmpty(lastDonatedDateStr)) return true;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date lastDate = sdf.parse(lastDonatedDateStr);
            if (lastDate == null) return true;
            
            long diffInMillis = System.currentTimeMillis() - lastDate.getTime();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            return diffInDays >= 90; // 3 months gap required
        } catch (ParseException e) {
            return true;
        }
    }
}
