package com.example.raktasewa.ui;

public class Donor {
    private String name;
    private String bloodType;
    private String phone;
    private String address;
    private String lastDonatedDate;
    private boolean availableToDonate;

    public Donor(String name, String bloodType, String phone, String address, String lastDonatedDate, boolean availableToDonate) {
        this.name = name;
        this.bloodType = bloodType;
        this.phone = phone;
        this.address = address;
        this.lastDonatedDate = lastDonatedDate;
        this.availableToDonate = availableToDonate;
    }

    // Getters
    public String getName() { return name; }
    public String getBloodType() { return bloodType; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getLastDonatedDate() { return lastDonatedDate; }
    public boolean isAvailableToDonate() { return availableToDonate; }
}