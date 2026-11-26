package com.safeme.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "sos_alerts")
@CompoundIndex(name = "idx_sos_user_status", def = "{'userId': 1, 'status': 1}")
public class SosAlertDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String senderName;
    private String journeyId;
    private String triggerSource; // MANUAL_SOS, MISSED_CHECKIN, VOICE_TRIGGER
    private String status; // ACTIVE, RESOLVED, CANCELLED
    private double latitude;
    private double longitude;
    private float accuracyMeters;
    private String addressLabel;
    private String notificationDeliveryStatus;
    private long createdAt;
    private long resolvedAt;
    private String resolutionNote;
    private List<ResponseAcknowledgementDocument> acknowledgements = new ArrayList<>();

    public SosAlertDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getJourneyId() { return journeyId; }
    public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

    public String getTriggerSource() { return triggerSource; }
    public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getAccuracyMeters() { return accuracyMeters; }
    public void setAccuracyMeters(float accuracyMeters) { this.accuracyMeters = accuracyMeters; }

    public String getAddressLabel() { return addressLabel; }
    public void setAddressLabel(String addressLabel) { this.addressLabel = addressLabel; }

    public String getNotificationDeliveryStatus() { return notificationDeliveryStatus; }
    public void setNotificationDeliveryStatus(String notificationDeliveryStatus) { this.notificationDeliveryStatus = notificationDeliveryStatus; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(long resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public List<ResponseAcknowledgementDocument> getAcknowledgements() { return acknowledgements; }
    public void setAcknowledgements(List<ResponseAcknowledgementDocument> acknowledgements) { this.acknowledgements = acknowledgements; }
}
