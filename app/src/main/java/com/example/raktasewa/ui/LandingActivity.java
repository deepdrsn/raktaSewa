package com.example.raktasewa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.raktasewa.ui.login.LoginActivity;
import com.example.raktasewa.ui.register.RegisterActivity;
import com.example.raktasewa.ui.register.RegisterSeekerActivity;

public class LandingActivity extends AppCompatActivity {

    Button btnLogin, btnDonor, btnSeeker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
}
