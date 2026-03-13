package com.example.raktasewa.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.raktasewa.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DonorListActivity extends AppCompatActivity {

    Spinner spinnerBloodType;
    Button btnSearchDonors;
    ListView listViewDonors;
    ProgressBar progressBar;

    FirebaseFirestore fStore;
    DonorAdapter donorAdapter;
    List<Donor> donorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_list);

        initializeUI();
        setupSpinner();

        fStore = FirebaseFirestore.getInstance();
        donorList = new ArrayList<>();
        donorAdapter = new DonorAdapter(this, donorList);
        listViewDonors.setAdapter(donorAdapter);

        btnSearchDonors.setOnClickListener(v -> searchDonors());
    }

    private void initializeUI() {
        spinnerBloodType = findViewById(R.id.spinnerBloodType);
        btnSearchDonors = findViewById(R.id.btnSearchDonors);
        listViewDonors = findViewById(R.id.listViewDonors);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> bloodAdapter = ArrayAdapter.createFromResource(this,
                R.array.blood_types, android.R.layout.simple_spinner_item);
        bloodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(bloodAdapter);
    }

    private void searchDonors() {
        String bloodType = spinnerBloodType.getSelectedItem().toString();

        if (bloodType.equals("Select Blood Type")) {
            Toast.makeText(this, "Please select blood type", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        donorList.clear();

        // Rules: Donors should always appear in the donor list.
        // So we don't filter by availability here, but we show it in the UI.
        fStore.collection("users")
                .whereEqualTo("role", "donor")
                .whereEqualTo("bloodType", bloodType)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Boolean isAvailable = document.getBoolean("available");
                            Donor donor = new Donor(
                                    document.getString("fullName"),
                                    document.getString("bloodType"),
                                    document.getString("phone"),
                                    document.getString("address"),
                                    document.getString("lastDonatedDate"),
                                    isAvailable != null ? isAvailable : false
                            );
                            donorList.add(donor);
                        }
                        donorAdapter.notifyDataSetChanged();

                        if (donorList.isEmpty()) {
                            Toast.makeText(DonorListActivity.this,
                                    "No donors found with blood type " + bloodType,
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(DonorListActivity.this,
                                "Error: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}