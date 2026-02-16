package com.example.raktasewa;

public class Donor {
    private String name;
    private String bloodType;
    private String phone;
    private String address;
    private String lastDonated;

    public Donor(String name, String bloodType, String phone, String address, String lastDonated) {
        this.name = name;
        this.bloodType = bloodType;
        this.phone = phone;
        this.address = address;
        this.lastDonated = lastDonated;
    }

    // Getters
    public String getName() { return name; }
    public String getBloodType() { return bloodType; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getLastDonated() { return lastDonated; }
}