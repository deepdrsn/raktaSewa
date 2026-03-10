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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    public static final String TAG = "RegisterDonorActivity";

    EditText etName, etEmail, etPassword, etPhone, etAddress, etLastDonated;
    Spinner spinnerBloodType, spinnerGender;
    Button btnRegister;
    TextView tvLogin;
    ProgressBar progressBar;

    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

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
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(bloodAdapter);

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
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

        progressBar.setVisibility(View.VISIBLE);

        fAuth.createUserWithEmailAndPassword(emailStr, passwordStr)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveLoginTimestamp(); // Save session start time
                        
                        String userID = fAuth.getCurrentUser().getUid();
                        DocumentReference documentReference = fStore.collection("users").document(userID);

                        Map<String, Object> user = new HashMap<>();
                        user.put("role", "donor");
                        user.put("fullName", nameStr);
                        user.put("email", emailStr);
                        user.put("phone", phoneStr);
                        user.put("address", addressStr);
                        user.put("bloodType", bloodTypeStr);
                        user.put("gender", genderStr);
                        user.put("lastDonatedDate", lastDonatedStr);

                        documentReference.set(user).addOnSuccessListener(aVoid -> {
                            Toast.makeText(RegisterActivity.this, "User Created.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                            finishAffinity();
                        });
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error! " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void saveLoginTimestamp() {
        SharedPreferences sharedPref = getSharedPreferences("RaktaSewaPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().putLong("lastLoginTime", System.currentTimeMillis()).apply();
    }
}