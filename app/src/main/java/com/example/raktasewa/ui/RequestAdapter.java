package com.example.raktasewa.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.raktasewa.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<BloodRequest> requestList;
    private String currentUserId;
    private FirebaseFirestore fStore;
    private Context context;
    private boolean isManageMode;

    public RequestAdapter(Context context, List<BloodRequest> requestList, String userId) {
        this(context, requestList, userId, false);
    }

    public RequestAdapter(Context context, List<BloodRequest> requestList, String userId, boolean isManageMode) {
        this.context = context;
        this.requestList = requestList;
        this.currentUserId = userId;
        this.fStore = FirebaseFirestore.getInstance();
        this.isManageMode = isManageMode;
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
        private TextView tvPatientName, tvHospital, tvBloodType, tvUnits, tvDate, tvAdditionalInfo, tvDonorInfo;
        private Chip chipStatus, chipEmergency;
        private MaterialButton btnContact, btnViewDetails, btnAction;
        private ImageButton btnShare, btnDelete;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvHospital = itemView.findViewById(R.id.tvHospital);
            tvBloodType = itemView.findViewById(R.id.tvBloodType);
            tvUnits = itemView.findViewById(R.id.tvUnits);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAdditionalInfo = itemView.findViewById(R.id.tvAdditionalInfo);
            tvDonorInfo = itemView.findViewById(R.id.tvDonorInfo);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            chipEmergency = itemView.findViewById(R.id.chipEmergency);
            btnContact = itemView.findViewById(R.id.btnContact);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnShare = itemView.findViewById(R.id.btnShare);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(BloodRequest request) {
            if (request == null) return;

            // Rule: Automatically expire fulfilled requests after 24 hours
            if ("fulfilled".equalsIgnoreCase(request.getStatus())) {
                long currentTime = System.currentTimeMillis();
                long fulfilledTime = request.getFulfilledTimestamp();
                if (fulfilledTime > 0 && (currentTime - fulfilledTime) > (24 * 60 * 60 * 1000)) {
                    itemView.setVisibility(View.GONE);
                    itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }
            } else {
                itemView.setVisibility(View.VISIBLE);
                itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            tvPatientName.setText(request.getPatientName() != null ? request.getPatientName() : "Unknown Patient");
            tvHospital.setText(request.getHospital() != null ? request.getHospital() : "Unknown Hospital");
            tvBloodType.setText(request.getBloodType() != null ? request.getBloodType() : "--");
            tvUnits.setText(request.getUnits() + " units needed");

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(request.getTimestamp())));

            if (request.getAdditionalInfo() != null && !request.getAdditionalInfo().isEmpty()) {
                tvAdditionalInfo.setVisibility(View.VISIBLE);
                tvAdditionalInfo.setText(request.getAdditionalInfo());
            } else {
                tvAdditionalInfo.setVisibility(View.GONE);
            }

            String status = request.getStatus();
            if (status == null) status = "pending";
            
            updateStatusChip(status);
            chipEmergency.setVisibility(request.isEmergency() ? View.VISIBLE : View.GONE);

            boolean isRequester = currentUserId.equals(request.getUserId());
            boolean isDonor = currentUserId.equals(request.getDonorId());
            boolean isPending = "pending".equalsIgnoreCase(status);
            boolean isAccepted = "accepted".equalsIgnoreCase(status);
            boolean isFulfilled = "fulfilled".equalsIgnoreCase(status);

            // Contact Info visibility: Show if seeker or accepted donor
            if (isRequester || isDonor) {
                btnContact.setVisibility(View.VISIBLE);
                btnContact.setOnClickListener(v -> {
                    if (request.getContact() != null) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + request.getContact()));
                        context.startActivity(intent);
                    }
                });
            } else {
                btnContact.setVisibility(View.GONE);
            }

            // Action Button Logic
            btnAction.setVisibility(View.GONE);
            if (!isRequester) {
                if (isPending) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText("Accept Request");
                    btnAction.setOnClickListener(v -> acceptRequest(request));
                } else if (isAccepted && isDonor) {
                    btnAction.setVisibility(View.VISIBLE);
                    btnAction.setText("Mark as Fulfilled");
                    btnAction.setOnClickListener(v -> fulfillRequest(request));
                }
            }

            // Donor info visibility for seeker
            if (isAccepted || isFulfilled) {
                tvDonorInfo.setVisibility(View.VISIBLE);
                if (request.getDonorId() != null) {
                    fetchDonorName(request.getDonorId(), tvDonorInfo);
                }
            } else {
                tvDonorInfo.setVisibility(View.GONE);
            }

            btnViewDetails.setOnClickListener(v -> {
                Toast.makeText(context, "Address: " + request.getAddress(), Toast.LENGTH_LONG).show();
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

            if (isManageMode && isRequester) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> showDeleteConfirmation(request, getAdapterPosition()));
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        }

        private void updateStatusChip(String status) {
            chipStatus.setText(status.toUpperCase());
            switch (status.toLowerCase()) {
                case "pending":
                    chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_dark);
                    break;
                case "accepted":
                    chipStatus.setChipBackgroundColorResource(android.R.color.holo_blue_dark);
                    break;
                case "fulfilled":
                    chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_dark);
                    break;
                default:
                    chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray);
            }
        }

        private void acceptRequest(BloodRequest request) {
            fStore.collection("blood_requests").document(request.getRequestId())
                    .update("status", "accepted", "donorId", currentUserId)
                    .addOnSuccessListener(aVoid -> {
                        request.setStatus("accepted");
                        request.setDonorId(currentUserId);
                        notifyItemChanged(getAdapterPosition());
                        Toast.makeText(context, "Request Accepted!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        private void fulfillRequest(BloodRequest request) {
            long timestamp = System.currentTimeMillis();
            fStore.collection("blood_requests").document(request.getRequestId())
                    .update("status", "fulfilled", "fulfilledTimestamp", timestamp)
                    .addOnSuccessListener(aVoid -> {
                        request.setStatus("fulfilled");
                        request.setFulfilledTimestamp(timestamp);
                        notifyItemChanged(getAdapterPosition());
                        Toast.makeText(context, "Marked as Fulfilled!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        private void fetchDonorName(String donorId, TextView tv) {
            fStore.collection("users").document(donorId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            tv.setText("Accepted by: " + documentSnapshot.getString("fullName"));
                        }
                    });
        }

        private void showDeleteConfirmation(BloodRequest request, int position) {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Request")
                    .setMessage("Are you sure you want to delete this blood request?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteRequest(request, position))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void deleteRequest(BloodRequest request, int position) {
            if (request.getRequestId() == null) return;
            fStore.collection("blood_requests").document(request.getRequestId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        requestList.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "Request deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}