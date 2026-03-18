package com.example.raktasewa.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.raktasewa.R;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CreateRequestActivity extends AppCompatActivity {

    private static final String TAG = "CreateRequestActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String VERCEL_URL = "https://raktasewa-notification-server.vercel.app/api/notify";

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
        if (fAuth.getCurrentUser() != null) {
            userId = fAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeUI();
        setupSpinners();

        btnSubmitRequest.setOnClickListener(v -> submitRequest());
        btnGetLocation.setOnClickListener(v -> handleLocationButtonClick());
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

    private void handleLocationButtonClick() {
        String[] options = {"Use Current Location", "Enter Hospital Address Manually"};
        new AlertDialog.Builder(this)
                .setTitle("Set Hospital Location")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        handleGetGPSLocation();
                    } else {
                        showManualLocationDialog();
                    }
                })
                .show();
    }

    private void handleGetGPSLocation() {
        if (!isLocationEnabled()) {
            showLocationSettingsDialog();
        } else {
            checkLocationPermission();
        }
    }

    private void showManualLocationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hospital Address");
        
        final EditText input = new EditText(this);
        input.setHint("e.g. City Hospital, Pokhara");
        builder.setView(input);

        builder.setPositiveButton("Set Location", (dialog, which) -> {
            String address = input.getText().toString().trim();
            if (!TextUtils.isEmpty(address)) {
                getCoordinatesFromAddress(address);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void getCoordinatesFromAddress(String addressStr) {
        tvLocationStatus.setText("Searching location...");
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    latitude = address.getLatitude();
                    longitude = address.getLongitude();
                    currentAddress = address.getAddressLine(0);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        tvLocationStatus.setText("Location: " + currentAddress);
                        Toast.makeText(this, "Hospital location set.", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        tvLocationStatus.setText("Location not found.");
                        Toast.makeText(this, "Could not find address. Try being more specific.", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    tvLocationStatus.setText("Network error.");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Disabled")
                .setMessage("Please enable GPS/Location to fetch your current address.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                Toast.makeText(this, "Permission denied. Location needed for address.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        tvLocationStatus.setText("Locating...");
        btnGetLocation.setEnabled(false);

        CurrentLocationRequest locationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(10000)
                .build();

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(locationRequest, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    btnGetLocation.setEnabled(true);
                    if (location != null) {
                        updateLocationData(location);
                    } else {
                        tryLastKnownLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    btnGetLocation.setEnabled(true);
                    tryLastKnownLocation();
                });
    }

    private void tryLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationData(location);
            } else {
                tvLocationStatus.setText("Location not found.");
            }
        });
    }

    private void updateLocationData(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        getAddressFromLocation(latitude, longitude);
    }

    private void getAddressFromLocation(double lat, double lon) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            String resultText;
            String addr;
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    addr = address.getAddressLine(0);
                    resultText = "Location: " + addr;
                } else {
                    addr = "Lat: " + lat + ", Lon: " + lon;
                    resultText = "Address not found.";
                }
            } catch (IOException e) {
                addr = "Lat: " + lat + ", Lon: " + lon;
                resultText = "Network error.";
            }

            final String finalAddr = addr;
            final String finalStatus = resultText;
            new Handler(Looper.getMainLooper()).post(() -> {
                currentAddress = finalAddr;
                tvLocationStatus.setText(finalStatus);
            });
        }).start();
    }

    private void submitRequest() {
        String patientName = etPatientName.getText().toString().trim();
        String hospital = etHospital.getText().toString().trim();
        String unitsStr = etUnits.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String additionalInfo = etAdditionalInfo.getText().toString().trim();
        String bloodType = spinnerBloodType.getSelectedItem().toString();
        String requestType = spinnerRequestType.getSelectedItem().toString();

        if (TextUtils.isEmpty(patientName) || TextUtils.isEmpty(hospital) || TextUtils.isEmpty(unitsStr)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (bloodType.equals("Select Blood Type")) {
            Toast.makeText(this, "Select blood type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (latitude == 0.0) {
            Toast.makeText(this, "Please capture location", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        boolean isEmergency = "Emergency".equalsIgnoreCase(requestType);

        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("patientName", patientName);
        request.put("hospital", hospital);
        request.put("units", Integer.parseInt(unitsStr));
        request.put("contact", contact);
        request.put("additionalInfo", additionalInfo);
        request.put("bloodType", bloodType);
        request.put("requestType", requestType);
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());
        request.put("isEmergency", isEmergency);
        request.put("latitude", latitude);
        request.put("longitude", longitude);
        request.put("address", currentAddress);

        fStore.collection("blood_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    sendNotificationsToDonors(isEmergency, bloodType, latitude, longitude);
                    Toast.makeText(this, "Request submitted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void sendNotificationsToDonors(boolean isEmergency, String bloodType, double reqLat, double reqLon) {
        fStore.collection("users")
                .whereEqualTo("role", "donor")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> tokens = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String token = doc.getString("fcmToken");
                        String donorId = doc.getId();
                        if (token == null) continue;

                        Double donorLat = doc.getDouble("latitude");
                        Double donorLon = doc.getDouble("longitude");
                        String donorBlood = doc.getString("bloodType");

                        boolean shouldNotify = false;
                        if (isEmergency) {
                            if (donorLat != null && donorLon != null) {
                                float[] results = new float[1];
                                Location.distanceBetween(reqLat, reqLon, donorLat, donorLon, results);
                                if (results[0] <= 10000) {
                                    shouldNotify = true;
                                }
                            }
                        } else {
                            if (bloodType.equals(donorBlood)) {
                                shouldNotify = true;
                            }
                        }

                        if (shouldNotify) {
                            tokens.add(token);
                            saveNotificationToDb(donorId, isEmergency ? "EMERGENCY Blood Request" : "Blood Needed", 
                                    "A request for " + bloodType + " blood has been made in your area.");
                        }
                    }
                    
                    if (!tokens.isEmpty()) {
                        String title = isEmergency ? "EMERGENCY Blood Request" : "Blood Needed";
                        String body = isEmergency ? "Emergency " + bloodType + " blood request near you!" 
                                                   : "A request for " + bloodType + " blood has been made in your area.";
                        triggerNotification(tokens, title, body);
                    }
                });
    }

    private void saveNotificationToDb(String targetUserId, String title, String body) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("isRead", false);
        notification.put("type", "request");

        fStore.collection("users").document(targetUserId)
                .collection("notifications").add(notification);
    }

    private void triggerNotification(List<String> tokens, String title, String body) {
        OkHttpClient client = new OkHttpClient();
        
        JSONObject json = new JSONObject();
        try {
            json.put("title", title);
            json.put("body", body);
            json.put("tokens", new JSONArray(tokens));
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody reqBody = RequestBody.create(
            json.toString(), MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(VERCEL_URL)
                .post(reqBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Notification trigger failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Notification trigger successful");
                } else {
                    Log.e(TAG, "Notification trigger error: " + response.code());
                }
                response.close();
            }
        });
    }
}
