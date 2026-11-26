package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SosAlert {
    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("senderName")
    private String senderName;

    @SerializedName("journeyId")
    private String journeyId;

    @SerializedName("triggerSource")
    private String triggerSource; // MANUAL_SOS, MISSED_CHECKIN, VOICE_TRIGGER

    @SerializedName("status")
    private String status; // ACTIVE, RESOLVED, CANCELLED

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("accuracyMeters")
    private float accuracyMeters;

    @SerializedName("addressLabel")
    private String addressLabel;

    @SerializedName("notificationDeliveryStatus")
    private String notificationDeliveryStatus;

    @SerializedName("createdAt")
    private long createdAt;

    @SerializedName("resolvedAt")
    private long resolvedAt;

    @SerializedName("resolutionNote")
    private String resolutionNote;

    @SerializedName("acknowledgements")
    private List<ResponseAcknowledgement> acknowledgements;

    public SosAlert() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public String getSenderName() { return senderName; }
    public String getJourneyId() { return journeyId; }
    public String getTriggerSource() { return triggerSource; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getAccuracyMeters() { return accuracyMeters; }
    public void setAccuracyMeters(float accuracyMeters) { this.accuracyMeters = accuracyMeters; }

    public String getAddressLabel() { return addressLabel; }
    public String getNotificationDeliveryStatus() { return notificationDeliveryStatus; }
    public long getCreatedAt() { return createdAt; }
    public long getResolvedAt() { return resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }
    public List<ResponseAcknowledgement> getAcknowledgements() { return acknowledgements; }
    public void setAcknowledgements(List<ResponseAcknowledgement> acknowledgements) { this.acknowledgements = acknowledgements; }
}
