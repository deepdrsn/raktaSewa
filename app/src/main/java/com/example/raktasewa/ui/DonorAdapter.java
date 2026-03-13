package com.example.raktasewa.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.raktasewa.R;

import java.util.List;

public class DonorAdapter extends BaseAdapter {

    private List<Donor> donorList;
    private LayoutInflater inflater;

    public DonorAdapter(DonorListActivity context, List<Donor> donorList) {
        this.donorList = donorList;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return donorList.size();
    }

    @Override
    public Object getItem(int position) {
        return donorList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_donor, parent, false);
        }

        Donor donor = donorList.get(position);

        TextView tvName = convertView.findViewById(R.id.tvDonorName);
        TextView tvBloodType = convertView.findViewById(R.id.tvDonorBloodType);
        TextView tvPhone = convertView.findViewById(R.id.tvDonorPhone);
        TextView tvAddress = convertView.findViewById(R.id.tvDonorAddress);
        TextView tvLastDonated = convertView.findViewById(R.id.tvLastDonated);
        TextView tvAvailability = convertView.findViewById(R.id.tvAvailability); // Assuming this ID exists or should be added
        Button btnContact = convertView.findViewById(R.id.btnContactDonor);

        tvName.setText(donor.getName());
        tvBloodType.setText("Blood Type: " + donor.getBloodType());
        tvAddress.setText("Address: " + donor.getAddress());

        String lastDonated = donor.getLastDonatedDate();
        if (lastDonated != null && !lastDonated.isEmpty()) {
            tvLastDonated.setText("Last Donated: " + lastDonated);
        } else {
            tvLastDonated.setText("Last Donated: Not available");
        }

        // Rules:
        // - If availableToDonate = false, show "Not Available" in the UI.
        // - Hide contact information if donor is not available.
        if (donor.isAvailableToDonate()) {
            if (tvAvailability != null) {
                tvAvailability.setText("Available");
                tvAvailability.setTextColor(convertView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            }
            tvPhone.setVisibility(View.VISIBLE);
            tvPhone.setText("Phone: " + donor.getPhone());
            btnContact.setVisibility(View.VISIBLE);
        } else {
            if (tvAvailability != null) {
                tvAvailability.setText("Not Available");
                tvAvailability.setTextColor(convertView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            }
            tvPhone.setVisibility(View.GONE);
            btnContact.setVisibility(View.GONE);
        }

        btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + donor.getPhone()));
            v.getContext().startActivity(intent);
        });

        return convertView;
    }
}