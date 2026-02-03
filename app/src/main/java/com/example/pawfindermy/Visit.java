package com.example.pawfindermy;

public class Visit {
    public String id;
    public String clinicName;
    public String notes;
    public String timestamp;
    public double latitude;
    public double longitude;
    public String imageBase64;


    public Visit() { }

    public Visit(String id, String clinicName, String notes, String timestamp, double latitude, double longitude, String imageBase64) {
        this.id = id;
        this.clinicName = clinicName;
        this.notes = notes;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageBase64 = imageBase64;
    }
}