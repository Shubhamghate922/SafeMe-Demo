package com.safeme.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "safety_checkins")
public class SafetyCheckInDocument {

    @Id
    private String id;

    @Indexed
    private String journeyId;

    @Indexed
    private String userId;

    private String status; // CONFIRMED_SAFE, NEED_HELP, MISSED
    private double latitude;
    private double longitude;
    private String note;
    private long checkedInAt;
    private long nextCheckInDueAt;

    public SafetyCheckInDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJourneyId() { return journeyId; }
    public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(long checkedInAt) { this.checkedInAt = checkedInAt; }

    public long getNextCheckInDueAt() { return nextCheckInDueAt; }
    public void setNextCheckInDueAt(long nextCheckInDueAt) { this.nextCheckInDueAt = nextCheckInDueAt; }
}
