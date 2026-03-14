package com.example.raktasewa.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.raktasewa.R;
import com.example.raktasewa.ui.DashboardActivity;
import com.example.raktasewa.ui.login.LoginActivity;
import com.example.raktasewa.ui.register.RegisterActivity;
import com.example.raktasewa.ui.register.RegisterSeekerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LandingActivity extends AppCompatActivity {

    Button btnLogin, btnDonor, btnSeeker;
    private FirebaseAuth fAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        fAuth = FirebaseAuth.getInstance();
        
        // Session Management: Check if user should be auto-logged in
        if (checkSession()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_landing);

        btnLogin = findViewById(R.id.btnLogin);
        btnDonor = findViewById(R.id.btnDonor);
        btnSeeker = findViewById(R.id.btnSeeker);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnDonor.setOnClickListener(v -> {
            Intent i = new Intent(this, RegisterActivity.class);
            i.putExtra("role", "donor");
            startActivity(i);
        });

        btnSeeker.setOnClickListener(v -> {
            Intent i = new Intent(this, RegisterSeekerActivity.class);
            i.putExtra("role", "seeker");
            startActivity(i);
        });
    }

    private boolean checkSession() {
        FirebaseUser currentUser = fAuth.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        SharedPreferences sharedPref = getSharedPreferences("RaktaSewaPrefs", Context.MODE_PRIVATE);
        long lastLoginTime = sharedPref.getLong("lastLoginTime", 0);
        long currentTime = System.currentTimeMillis();
        
        long oneWeekMillis = 7L * 24 * 60 * 60 * 1000;

        if (currentTime - lastLoginTime > oneWeekMillis) {
            fAuth.signOut();
            return false;
        }

        return true;
    }
}
