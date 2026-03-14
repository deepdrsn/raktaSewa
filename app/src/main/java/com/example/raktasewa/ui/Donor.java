package com.example.raktasewa.ui;

public class Donor {
    private String name;
    private String bloodType;
    private String phone;
    private String address;
    private String lastDonatedDate;
    private boolean availableToDonate;
    private double latitude;
    private double longitude;
    private String city;
    private double distance; // Added distance field

    public Donor(String name, String bloodType, String phone, String address, String lastDonatedDate, boolean availableToDonate) {
        this.name = name;
        this.bloodType = bloodType;
        this.phone = phone;
        this.address = address;
        this.lastDonatedDate = lastDonatedDate;
        this.availableToDonate = availableToDonate;
    }

    public Donor(String name, String bloodType, String phone, String address, String lastDonatedDate, boolean availableToDonate, double latitude, double longitude, String city) {
        this.name = name;
        this.bloodType = bloodType;
        this.phone = phone;
        this.address = address;
        this.lastDonatedDate = lastDonatedDate;
        this.availableToDonate = availableToDonate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
    }

    // Getters
    public String getName() { return name; }
    public String getBloodType() { return bloodType; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getLastDonatedDate() { return lastDonatedDate; }
    public boolean isAvailableToDonate() { return availableToDonate; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getCity() { return city; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
}