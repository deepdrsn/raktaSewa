package com.example.raktasewa.ui;
/// /////
import android.content.Intent;
import android.os.Bundle;
// ... other imports ...
import com.example.raktasewa.R;
/// ///
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
        Button btnContact = convertView.findViewById(R.id.btnContactDonor);

        tvName.setText(donor.getName());
        tvBloodType.setText("Blood Type: " + donor.getBloodType());
        tvPhone.setText("Phone: " + donor.getPhone());
        tvAddress.setText("Address: " + donor.getAddress());

        String lastDonated = donor.getLastDonated();
        if (lastDonated != null && !lastDonated.isEmpty()) {
            tvLastDonated.setText("Last Donated: " + lastDonated);
        } else {
            tvLastDonated.setText("Last Donated: Not available");
        }

        btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + donor.getPhone()));
            v.getContext().startActivity(intent);
        });

        return convertView;
    }
}