package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;

public class SafetyCheckIn {
    @SerializedName("id")
    private String id;

    @SerializedName("journeyId")
    private String journeyId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("status")
    private String status; // CONFIRMED_SAFE, NEED_HELP, MISSED

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("note")
    private String note;

    @SerializedName("checkedInAt")
    private long checkedInAt;

    @SerializedName("nextCheckInDueAt")
    private long nextCheckInDueAt;

    public SafetyCheckIn() {}

    public SafetyCheckIn(String journeyId, String status, double latitude, double longitude, String note) {
        this.journeyId = journeyId;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.note = note;
    }

    public String getId() { return id; }
    public String getJourneyId() { return journeyId; }
    public String getUserId() { return userId; }
    public String getStatus() { return status; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getNote() { return note; }
    public long getCheckedInAt() { return checkedInAt; }
    public long getNextCheckInDueAt() { return nextCheckInDueAt; }
}
