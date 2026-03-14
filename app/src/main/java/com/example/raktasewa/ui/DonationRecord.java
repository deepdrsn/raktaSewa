package com.example.raktasewa.ui;

public class DonationRecord {
    private String date;
    private long timestamp;

    public DonationRecord() {}

    public DonationRecord(String date, long timestamp) {
        this.date = date;
        this.timestamp = timestamp;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
