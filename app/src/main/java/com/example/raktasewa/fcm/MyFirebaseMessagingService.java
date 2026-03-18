package com.example.raktasewa.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.raktasewa.ui.LandingActivity;
import com.example.raktasewa.R;
import com.example.raktasewa.ui.RequestDetailsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFCMService";
    private static final String CHANNEL_ID = "blood_request_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "";
        String body = "";
        String requestId = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            if (title.isEmpty()) title = data.get("title");
            if (body.isEmpty()) body = data.get("body");
            requestId = data.get("requestId");
        }

        sendNotification(title, body, requestId);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateTokenInFirestore(token);
    }

    private void updateTokenInFirestore(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .update("fcmToken", token)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update token", e));
        }
    }

    private void sendNotification(String title, String body, String requestId) {
        Intent intent;
        if (requestId != null && !requestId.isEmpty()) {
            intent = new Intent(this, RequestDetailsActivity.class);
            intent.putExtra("requestId", requestId);
        } else {
            intent = new Intent(this, LandingActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Blood Requests",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}
