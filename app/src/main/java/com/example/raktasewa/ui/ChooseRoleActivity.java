package com.example.raktasewa.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.raktasewa.R;import com.example.raktasewa.ui.register.RegisterActivity; // Your existing register activity
import com.example.raktasewa.ui.register.RegisterSeekerActivity; // New activity (we'll create this next)

public class ChooseRoleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_role);

        findViewById(R.id.btnRegisterDonor).setOnClickListener(v -> {
            Intent intent = new Intent(ChooseRoleActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnRegisterSeeker).setOnClickListener(v -> {
            Intent intent = new Intent(ChooseRoleActivity.this, RegisterSeekerActivity.class);
            startActivity(intent);
        });
    }
}
