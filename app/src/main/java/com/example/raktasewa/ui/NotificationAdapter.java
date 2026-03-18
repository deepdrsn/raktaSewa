package com.example.raktasewa.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.raktasewa.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> notifications;
    private FirebaseFirestore fStore;
    private String currentUserId;

    public NotificationAdapter(List<NotificationModel> notifications) {
        this.notifications = notifications;
        this.fStore = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notification = notifications.get(position);
        holder.tvTitle.setText(notification.getTitle());
        holder.tvBody.setText(notification.getBody());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        holder.tvTimestamp.setText(sdf.format(new Date(notification.getTimestamp())));

        holder.itemView.setOnClickListener(v -> {
            if (notification.getRequestId() != null && !notification.getRequestId().isEmpty()) {
                Intent intent = new Intent(v.getContext(), RequestDetailsActivity.class);
                intent.putExtra("requestId", notification.getRequestId());
                v.getContext().startActivity(intent);
            } else {
                Toast.makeText(v.getContext(), "No details available for this notification.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDialog(v.getContext(), notification, position);
            return true;
        });
    }

    private void showDeleteDialog(Context context, NotificationModel notification, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> deleteNotification(context, notification, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNotification(Context context, NotificationModel notification, int position) {
        if (notification.getNotificationId() == null) {
            Toast.makeText(context, "Cannot delete: Notification ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        fStore.collection("users").document(currentUserId)
                .collection("notifications").document(notification.getNotificationId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    notifications.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTimestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvTimestamp = itemView.findViewById(R.id.tvNotificationTimestamp);
        }
    }
}
