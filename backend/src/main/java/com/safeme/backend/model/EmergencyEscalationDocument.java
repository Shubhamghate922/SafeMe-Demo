package com.safeme.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "emergency_escalations")
public class EmergencyEscalationDocument {

    @Id
    private String id;

    @Indexed
    private String journeyId;

    @JsonIgnore
    @Indexed
    private String userId;

    private int currentStage;
    private String status;
    private int notifiedContactsCount;
    private long triggeredAt;

    public EmergencyEscalationDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJourneyId() { return journeyId; }
    public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getCurrentStage() { return currentStage; }
    public void setCurrentStage(int currentStage) { this.currentStage = currentStage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getNotifiedContactsCount() { return notifiedContactsCount; }
    public void setNotifiedContactsCount(int notifiedContactsCount) { this.notifiedContactsCount = notifiedContactsCount; }

    public long getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(long triggeredAt) { this.triggeredAt = triggeredAt; }
}
