package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;

public class IncidentReport {
    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("category")
    private String category;

    @SerializedName("description")
    private String description;

    @SerializedName("incidentDate")
    private String incidentDate;

    @SerializedName("locationName")
    private String locationName;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("photoUrl")
    private String photoUrl;

    @SerializedName("audioUrl")
    private String audioUrl;

    @SerializedName("status")
    private String status; // SUBMITTED, UNDER_REVIEW, RESOLVED

    @SerializedName("createdAt")
    private long createdAt;

    public IncidentReport() {}

    public IncidentReport(String category, String description, String incidentDate,
                          String locationName, double latitude, double longitude) {
        this.category = category;
        this.description = description;
        this.incidentDate = incidentDate;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getIncidentDate() { return incidentDate; }
    public String getLocationName() { return locationName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getPhotoUrl() { return photoUrl; }
    public String getAudioUrl() { return audioUrl; }
    public String getStatus() { return status; }
    public long getCreatedAt() { return createdAt; }
}
