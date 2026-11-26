package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;

public class LocationUpdate {
    @SerializedName("id")
    private String id;

    @SerializedName("sessionId")
    private String sessionId; // sosId or journeyId

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("accuracyMeters")
    private float accuracyMeters;

    @SerializedName("speedMps")
    private float speedMps;

    @SerializedName("bearing")
    private float bearing;

    @SerializedName("recordedAt")
    private long recordedAt;

    public LocationUpdate() {}

    public LocationUpdate(double latitude, double longitude, float accuracyMeters, float speedMps, float bearing, long recordedAt) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.speedMps = speedMps;
        this.bearing = bearing;
        this.recordedAt = recordedAt;
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getAccuracyMeters() { return accuracyMeters; }
    public float getSpeedMps() { return speedMps; }
    public float getBearing() { return bearing; }
    public long getRecordedAt() { return recordedAt; }
}
