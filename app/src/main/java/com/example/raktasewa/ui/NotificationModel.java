package com.example.raktasewa.ui;

import com.google.firebase.firestore.PropertyName;

public class NotificationModel {
    private String title;
    private String body;
    private long timestamp;
    private String type;
    private boolean isRead;
    private String notificationId;

    public NotificationModel() {
        // Required for Firestore
    }

    public NotificationModel(String title, String body, long timestamp, String type) {
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.type = type;
        this.isRead = false;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @PropertyName("isRead")
    public boolean isRead() { return isRead; }

    @PropertyName("isRead")
    public void setRead(boolean read) { isRead = read; }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
}
