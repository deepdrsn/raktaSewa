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
import java.util.Locale;

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
        TextView tvDistance = convertView.findViewById(R.id.tvDonorDistance);
        TextView tvBloodType = convertView.findViewById(R.id.tvDonorBloodType);
        TextView tvPhone = convertView.findViewById(R.id.tvDonorPhone);
        TextView tvAddress = convertView.findViewById(R.id.tvDonorAddress);
        TextView tvCity = convertView.findViewById(R.id.tvDonorCity);
        TextView tvLastDonated = convertView.findViewById(R.id.tvLastDonated);
        TextView tvAvailability = convertView.findViewById(R.id.tvAvailability);
        Button btnContact = convertView.findViewById(R.id.btnContactDonor);

        tvName.setText(donor.getName());
        
        if (donor.getDistance() > 0) {
            tvDistance.setVisibility(View.VISIBLE);
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", donor.getDistance()));
        } else {
            tvDistance.setVisibility(View.GONE);
        }

        tvBloodType.setText("Blood Type: " + donor.getBloodType());
        tvAddress.setText("Address: " + donor.getAddress());
        
        if (donor.getCity() != null && !donor.getCity().isEmpty()) {
            tvCity.setVisibility(View.VISIBLE);
            tvCity.setText("City: " + donor.getCity());
        } else {
            tvCity.setVisibility(View.GONE);
        }

        String lastDonated = donor.getLastDonatedDate();
        if (lastDonated != null && !lastDonated.isEmpty()) {
            tvLastDonated.setText("Last Donated: " + lastDonated);
        } else {
            tvLastDonated.setText("Last Donated: Not available");
        }

        if (donor.isAvailableToDonate()) {
            tvAvailability.setText("Available");
            tvAvailability.setTextColor(convertView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            tvPhone.setVisibility(View.VISIBLE);
            tvPhone.setText("Phone: " + donor.getPhone());
            btnContact.setVisibility(View.VISIBLE);
        } else {
            tvAvailability.setText("Not Available");
            tvAvailability.setTextColor(convertView.getContext().getResources().getColor(android.R.color.holo_red_dark));
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
