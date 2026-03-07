package com.example.raktasewa.ui;

import android.content.Context; // Added: Use Context instead of specific Activity
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.raktasewa.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// IMPORTANT: If BloodRequest and ViewRequestsActivity are in 'com.example.raktasewa',
// you must import them here:
// import com.example.raktasewa.BloodRequest;
// import com.example.raktasewa.ViewRequestsActivity;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<BloodRequest> requestList;
    private String currentUserId;
    private FirebaseFirestore fStore;
    private Context context; // Added to handle context properly

    // FIXED: Changed ViewRequestsActivity to Context for better compatibility
    public RequestAdapter(Context context, List<BloodRequest> requestList, String userId) {
        this.context = context;
        this.requestList = requestList;
        this.currentUserId = userId;
        this.fStore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blood_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        BloodRequest request = requestList.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return requestList != null ? requestList.size() : 0;
    }

    class RequestViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView tvPatientName, tvHospital, tvBloodType, tvUnits, tvDate, tvAdditionalInfo;
        private Chip chipStatus, chipEmergency;
        private MaterialButton btnContact, btnViewDetails;
        private ImageButton btnShare;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvHospital = itemView.findViewById(R.id.tvHospital);
            tvBloodType = itemView.findViewById(R.id.tvBloodType);
            tvUnits = itemView.findViewById(R.id.tvUnits);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAdditionalInfo = itemView.findViewById(R.id.tvAdditionalInfo);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipEmergency = itemView.findViewById(R.id.chipEmergency);
            btnContact = itemView.findViewById(R.id.btnContact);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnShare = itemView.findViewById(R.id.btnShare);
        }

        public void bind(BloodRequest request) {
            tvPatientName.setText(request.getPatientName());
            tvHospital.setText(request.getHospital());
            tvBloodType.setText(request.getBloodType());
            tvUnits.setText(request.getUnits() + " units needed");

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(request.getTimestamp())));

            // Additional info
            if (request.getAdditionalInfo() != null && !request.getAdditionalInfo().isEmpty()) {
                tvAdditionalInfo.setVisibility(View.VISIBLE);
                tvAdditionalInfo.setText(request.getAdditionalInfo());
            } else {
                tvAdditionalInfo.setVisibility(View.GONE);
            }

            // Status chip colors (Ensure these colors exist in your colors.xml)
            switch (request.getStatus().toLowerCase()) {
                case "pending":
                    chipStatus.setText("PENDING");
                    chipStatus.setChipBackgroundColorResource(R.color.status_pending);
                    break;
                case "approved":
                    chipStatus.setText("APPROVED");
                    chipStatus.setChipBackgroundColorResource(R.color.status_approved);
                    break;
                case "completed":
                    chipStatus.setText("COMPLETED");
                    chipStatus.setChipBackgroundColorResource(R.color.status_completed);
                    break;
                default:
                    chipStatus.setText(request.getStatus().toUpperCase());
                    chipStatus.setChipBackgroundColorResource(R.color.status_default);
            }

            chipEmergency.setVisibility(request.isEmergency() ? View.VISIBLE : View.GONE);

            btnContact.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + request.getContact()));
                context.startActivity(intent);
            });

            btnViewDetails.setOnClickListener(v -> {
                Toast.makeText(context, "Viewing details for " + request.getPatientName(), Toast.LENGTH_SHORT).show();
            });

            btnShare.setOnClickListener(v -> {
                String shareText = "URGENT: Need " + request.getBloodType() +
                        " blood for " + request.getPatientName() +
                        " at " + request.getHospital() +
                        ". Contact: " + request.getContact();

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                context.startActivity(Intent.createChooser(shareIntent, "Share via"));
            });
        }
    }
}