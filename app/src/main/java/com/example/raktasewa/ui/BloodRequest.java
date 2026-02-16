package com.example.raktasewa;

public class BloodRequest {
    private String requestId;
    private String userId;
    private String patientName;
    private String hospital;
    private String bloodType;
    private int units;
    private String contact;
    private String additionalInfo;
    private String requestType;
    private String status;
    private long timestamp;
    private boolean isEmergency;

    // Empty constructor needed for Firestore
    public BloodRequest() {}

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getHospital() { return hospital; }
    public void setHospital(String hospital) { this.hospital = hospital; }

    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }

    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isEmergency() { return isEmergency; }
    public void setEmergency(boolean emergency) { isEmergency = emergency; }
}