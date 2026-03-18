package com.example.raktasewa.ui;

public class NotificationModel {
    private String title;
    private String body;
    private long timestamp;
    private String type;
    private boolean isRead;

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
    public String getBody() { return body; }
    public long getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
}
