package com.safeme.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "community_reports")
public class CommunityReportDocument {

    @Id
    private String id;

    @JsonIgnore
    private String reporterUserId; // Never exposed in public community report responses

    private String category; // POOR_LIGHTING, ROAD_HAZARD, HARASSMENT, UNSAFE_AREA, OTHER
    private String description;
    private String locationName;
    private double latitude;
    private double longitude;
    private long reportedAt;
    private long expiresAt;
    private String verificationStatus; // PENDING_MODERATION, VERIFIED, EXPIRED

    public CommunityReportDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(String reporterUserId) { this.reporterUserId = reporterUserId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public long getReportedAt() { return reportedAt; }
    public void setReportedAt(long reportedAt) { this.reportedAt = reportedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
}
