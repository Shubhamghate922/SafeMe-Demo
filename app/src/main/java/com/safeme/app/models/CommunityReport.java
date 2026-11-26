package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;

public class CommunityReport {
    @SerializedName("id")
    private String id;

    @SerializedName("category")
    private String category; // POOR_LIGHTING, ROAD_HAZARD, HARASSMENT, UNSAFE_AREA, OTHER

    @SerializedName("description")
    private String description;

    @SerializedName("locationName")
    private String locationName;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("reportedAt")
    private long reportedAt;

    @SerializedName("expiresAt")
    private long expiresAt;

    @SerializedName("verificationStatus")
    private String verificationStatus; // PENDING_MODERATION, VERIFIED, EXPIRED

    public CommunityReport() {}

    public CommunityReport(String category, String description, String locationName, double latitude, double longitude) {
        this.category = category;
        this.description = description;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getLocationName() { return locationName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getReportedAt() { return reportedAt; }
    public long getExpiresAt() { return expiresAt; }
    public String getVerificationStatus() { return verificationStatus; }
}
