package com.example.raktasewa.ui.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.raktasewa.R;
import com.example.raktasewa.ui.DashboardActivity;
import com.example.raktasewa.ui.ChooseRoleActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister, tvForgotPassword;
    ProgressBar progressBar;
    FirebaseAuth fAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeUI();
        fAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ChooseRoleActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
    }

    private void initializeUI() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleForgotPassword() {
        EditText resetMail = new EditText(this);
        AlertDialog.Builder passwordResetDialog = new AlertDialog.Builder(this);
        passwordResetDialog.setTitle("Reset Password?");
        passwordResetDialog.setMessage("Enter your email to receive reset link.");
        passwordResetDialog.setView(resetMail);

        passwordResetDialog.setPositiveButton("Send", (dialog, which) -> {
            String mail = resetMail.getText().toString().trim();
            if (TextUtils.isEmpty(mail)) {
                Toast.makeText(LoginActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }
            fAuth.sendPasswordResetEmail(mail).addOnSuccessListener(aVoid -> {
                Toast.makeText(LoginActivity.this, "Reset link sent to your email.", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(LoginActivity.this, "Error! Reset link not sent: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });

        passwordResetDialog.setNegativeButton("Cancel", null);
        passwordResetDialog.create().show();
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password is required"); return; }

        progressBar.setVisibility(View.VISIBLE);

        fAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        FirebaseUser user = fAuth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            saveLoginTimestamp();
                            updateFCMToken(user.getUid());
                            
                            Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                            fAuth.signOut();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Error: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String token = task.getResult();
                FirebaseFirestore.getInstance().collection("users").document(userId)
                        .update("fcmToken", token)
                        .addOnFailureListener(e -> Log.e("Login", "Failed to update token", e));
            }
        });
    }

    private void saveLoginTimestamp() {
        SharedPreferences sharedPref = getSharedPreferences("RaktaSewaPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong("lastLoginTime", System.currentTimeMillis());
        editor.apply();
    }
}
