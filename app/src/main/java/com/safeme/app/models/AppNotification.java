package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;

public class AppNotification {
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type; // SOS_ALERT, CHECKIN_REMINDER, ACKNOWLEDGEMENT, ESCALATION, SYSTEM

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("referenceId")
    private String referenceId;

    @SerializedName("read")
    private boolean read;

    @SerializedName("createdAt")
    private long createdAt;

    public String getId() { return id; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getReferenceId() { return referenceId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public long getCreatedAt() { return createdAt; }
}
