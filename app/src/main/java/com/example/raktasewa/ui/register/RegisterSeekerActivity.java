package com.example.raktasewa.ui.register;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.appcompat.app.AppCompatActivity;

import com.example.raktasewa.R;
import com.example.raktasewa.ui.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterSeekerActivity extends AppCompatActivity {

    public static final String TAG = "RegisterSeekerActivity";

    EditText etName, etEmail, etPassword, etPhone, etAddress;
    Spinner spinnerGender;
    Button btnRegister;
    TextView tvLogin;
    ProgressBar progressBar;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_seeker);

        initializeUI();
        setupSpinners();
        initializeFirebase();
        setupClickListeners();
    }

    private void initializeUI() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        spinnerGender = findViewById(R.id.spinnerGender);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
    }

    private void initializeFirebase() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> registerSeeker());
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });
    }

    private void registerSeeker() {
        String nameStr = etName.getText().toString().trim();
        String emailStr = etEmail.getText().toString().trim();
        String passwordStr = etPassword.getText().toString().trim();
        String phoneStr = etPhone.getText().toString().trim();
        String addressStr = etAddress.getText().toString().trim();
        String genderStr = spinnerGender.getSelectedItem().toString();

        if (TextUtils.isEmpty(nameStr)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(emailStr)) { etEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(passwordStr)) { etPassword.setError("Required"); return; }
        if (passwordStr.length() < 6) { etPassword.setError(">= 6 chars"); return; }
        if (TextUtils.isEmpty(phoneStr)) { etPhone.setError("Required"); return; }

        progressBar.setVisibility(View.VISIBLE);

        fAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser fUser = fAuth.getCurrentUser();
                        fUser.sendEmailVerification().addOnSuccessListener(aVoid -> {
                            Toast.makeText(RegisterSeekerActivity.this, "Verification Email Sent.", Toast.LENGTH_SHORT).show();
                        }).addOnFailureListener(e -> {
                            Log.d(TAG, "onFailure: Email not sent " + e.getMessage());
                        });

                        String userID = fUser.getUid();
                        DocumentReference documentReference = fStore.collection("users").document(userID);

                        Map<String, Object> user = new HashMap<>();
                        user.put("role", "seeker");
                        user.put("fullName", nameStr);
                        user.put("email", emailStr);
                        user.put("phone", phoneStr);
                        user.put("address", addressStr);
                        user.put("gender", genderStr);
                        user.put("createdAt", System.currentTimeMillis());

                        documentReference.set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RegisterSeekerActivity.this, "Registration Successful! Please verify your email.", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(RegisterSeekerActivity.this, "Error creating profile.", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                });
                    } else {
                        Toast.makeText(RegisterSeekerActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }
}
