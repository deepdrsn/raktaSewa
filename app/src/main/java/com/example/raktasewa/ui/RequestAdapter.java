package com.example.raktasewa.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<BloodRequest> requestList;
    private String currentUserId;
    private FirebaseFirestore fStore;
    private Context context;
    private boolean isManageMode;
    private String userBloodType;
    private String currentUserName;

    private static final String VERCEL_URL = "https://raktasewa-notification-server.vercel.app/api/notify";

    public RequestAdapter(Context context, List<BloodRequest> requestList, String userId) {
        this(context, requestList, userId, false);
    }

    public RequestAdapter(Context context, List<BloodRequest> requestList, String userId, boolean isManageMode) {
        this.context = context;
        this.requestList = requestList;
        this.currentUserId = userId;
        this.fStore = FirebaseFirestore.getInstance();
        this.isManageMode = isManageMode;
        fetchCurrentUserDetails();
    }

    private void fetchCurrentUserDetails() {
        fStore.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userBloodType = documentSnapshot.getString("bloodType");
                        currentUserName = documentSnapshot.getString("fullName");
                    }
                });
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
        private TextView tvPatientName, tvHospital, tvBloodType, tvUnits, tvDate, tvAdditionalInfo, tvDonorInfo, tvRequesterInfo;
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
            tvRequesterInfo = itemView.findViewById(R.id.tvRequesterInfo);
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

            // EXPIRY LOGIC: Hide fulfilled requests older than 24 hours
            if ("fulfilled".equalsIgnoreCase(request.getStatus())) {
                long currentTime = System.currentTimeMillis();
                long fulfilledTime = request.getFulfilledTimestamp();
                if (fulfilledTime > 0 && (currentTime - fulfilledTime) > (24 * 60 * 60 * 1000)) {
                    itemView.setVisibility(View.GONE);
                    itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                    return;
                }
            }
            
            // Ensure view is visible if not expired
            itemView.setVisibility(View.VISIBLE);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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

            if (tvRequesterInfo != null) {
                tvRequesterInfo.setVisibility(View.VISIBLE);
                fetchUserName(request.getUserId(), tvRequesterInfo, "Requested by: ");
            }

            if (isRequester) {
                if (isAccepted && request.getDonorId() != null) {
                    btnContact.setVisibility(View.VISIBLE);
                    btnContact.setOnClickListener(v -> {
                        fStore.collection("users").document(request.getDonorId()).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String phone = documentSnapshot.getString("phone");
                                        if (phone != null) {
                                            Intent intent = new Intent(Intent.ACTION_DIAL);
                                            intent.setData(Uri.parse("tel:" + phone));
                                            context.startActivity(intent);
                                        } else {
                                            Toast.makeText(context, "Donor phone not found", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    });
                } else {
                    btnContact.setVisibility(View.GONE);
                }
            } else {
                if (isPending || isDonor) {
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
            }

            btnAction.setVisibility(View.GONE);
            if (isPending && !isRequester) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Accept Request");
                btnAction.setOnClickListener(v -> showAcceptConfirmationDialog(request));
            } else if (isAccepted && isRequester) {
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Mark as Fulfilled");
                btnAction.setOnClickListener(v -> fulfillRequest(request));
            }

            if (isAccepted || "fulfilled".equalsIgnoreCase(status)) {
                tvDonorInfo.setVisibility(View.VISIBLE);
                if (request.getDonorId() != null) {
                    fetchUserName(request.getDonorId(), tvDonorInfo, "Accepted by: ");
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

        private void showAcceptConfirmationDialog(BloodRequest request) {
            fStore.collection("users").document(currentUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String lastDonated = documentSnapshot.getString("lastDonatedDate");
                            boolean isAvailable = documentSnapshot.getBoolean("available") != null && documentSnapshot.getBoolean("available");

                            if (!isAvailable || !isEligibleToDonate(lastDonated)) {
                                Toast.makeText(context, "You are not eligible to donate yet. Please wait 3 months between donations.", Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (userBloodType == null) {
                                Toast.makeText(context, "Please complete your profile with blood type first.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (!userBloodType.equalsIgnoreCase(request.getBloodType())) {
                                Toast.makeText(context, "You can only accept requests matching your blood group (" + userBloodType + ").", Toast.LENGTH_LONG).show();
                                return;
                            }

                            fStore.collection("blood_requests")
                                    .whereEqualTo("donorId", currentUserId)
                                    .whereEqualTo("status", "accepted")
                                    .get()
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                                            Toast.makeText(context, "You already have an active accepted request. Please fulfill it first.", Toast.LENGTH_LONG).show();
                                        } else {
                                            displayConfirmationDialog(request);
                                        }
                                    });
                        }
                    });
        }

        private boolean isEligibleToDonate(String lastDonatedDateStr) {
            if (lastDonatedDateStr == null || lastDonatedDateStr.isEmpty()) return true;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                Date lastDate = sdf.parse(lastDonatedDateStr);
                if (lastDate == null) return true;
                long diffInMillis = System.currentTimeMillis() - lastDate.getTime();
                long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);
                return diffInDays >= 90;
            } catch (ParseException e) {
                return true;
            }
        }

        private void displayConfirmationDialog(BloodRequest request) {
            String info = "Patient: " + request.getPatientName() + "\n" +
                          "Location: " + request.getHospital() + "\n" +
                          "Blood Group: " + request.getBloodType();

            new AlertDialog.Builder(context)
                    .setTitle("Accept Blood Request?")
                    .setMessage(info)
                    .setPositiveButton("Accept", (dialog, which) -> acceptRequest(request))
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void acceptRequest(BloodRequest request) {
            fStore.collection("blood_requests").document(request.getRequestId())
                    .update("status", "accepted", "donorId", currentUserId)
                    .addOnSuccessListener(aVoid -> {
                        request.setStatus("accepted");
                        request.setDonorId(currentUserId);
                        notifyItemChanged(getAdapterPosition());
                        Toast.makeText(context, "Request Accepted!", Toast.LENGTH_SHORT).show();
                        
                        // Notify the requester
                        notifyRequester(request.getUserId(), currentUserName);
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }

        private void notifyRequester(String requesterId, String donorName) {
            String title = "Request Accepted!";
            String body = donorName + " has accepted your blood request.";
            
            // 1. Save to Firestore Notification History
            saveNotificationToDb(requesterId, title, body);
            
            // 2. Trigger Push Notification via FCM
            fStore.collection("users").document(requesterId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String token = doc.getString("fcmToken");
                            if (token != null) {
                                triggerNotification(Collections.singletonList(token), title, body);
                            }
                        }
                    });
        }

        private void saveNotificationToDb(String targetUserId, String title, String body) {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("isRead", false);
            notification.put("type", "acceptance");

            fStore.collection("users").document(targetUserId)
                    .collection("notifications").add(notification);
        }

        private void triggerNotification(List<String> tokens, String title, String body) {
            OkHttpClient client = new OkHttpClient();
            JSONObject json = new JSONObject();
            try {
                json.put("title", title);
                json.put("body", body);
                json.put("tokens", new JSONArray(tokens));
            } catch (JSONException e) {
                return;
            }

            RequestBody reqBody = RequestBody.create(
                json.toString(), MediaType.parse("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(VERCEL_URL)
                    .post(reqBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("Notification", "Failed to notify requester: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    response.close();
                }
            });
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

        private void fetchUserName(String userId, TextView tv, String prefix) {
            fStore.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            tv.setText(prefix + documentSnapshot.getString("fullName"));
                        } else {
                            tv.setText(prefix + "Unknown User");
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
