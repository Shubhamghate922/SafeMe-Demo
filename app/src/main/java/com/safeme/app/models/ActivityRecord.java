package com.safeme.app.models;

public class ActivityRecord {
    private final String id;
    private final String category; // JOURNEY, CHECKIN, MISSED_CHECKIN, SOS, RESPONSE, INCIDENT
    private final String title;
    private final String subtitle;
    private final String status;
    private final long timestamp;

    public ActivityRecord(String id, String category, String title, String subtitle, String status, long timestamp) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.subtitle = subtitle;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
}
