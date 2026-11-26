package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Journey implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("startLocationName")
    private String startLocationName;

    @SerializedName("startLatitude")
    private double startLatitude;

    @SerializedName("startLongitude")
    private double startLongitude;

    @SerializedName("destinationName")
    private String destinationName;

    @SerializedName("destinationLatitude")
    private double destinationLatitude;

    @SerializedName("destinationLongitude")
    private double destinationLongitude;

    @SerializedName("expectedDurationMinutes")
    private int expectedDurationMinutes;

    @SerializedName("expectedArrivalTime")
    private long expectedArrivalTime;

    @SerializedName("transportMode")
    private String transportMode; // WALKING, BICYCLE, TWO_WHEELER, CAR, PUBLIC_TRANSPORT, OTHER

    @SerializedName("trustedContactIds")
    private List<String> trustedContactIds;

    @SerializedName("checkInIntervalMinutes")
    private int checkInIntervalMinutes;

    @SerializedName("monitoringEnabled")
    private boolean monitoringEnabled = true;

    @SerializedName("status")
    private String status; // CREATED, ACTIVE, COMPLETED, ESCALATED, CANCELLED

    @SerializedName("startedAt")
    private long startedAt;

    @SerializedName("completedAt")
    private long completedAt;

    @SerializedName("nextCheckInDueAt")
    private long nextCheckInDueAt;

    @SerializedName("lastLatitude")
    private double lastLatitude;

    @SerializedName("lastLongitude")
    private double lastLongitude;

    @SerializedName("lastAccuracyMeters")
    private float lastAccuracyMeters;

    @SerializedName("lastLocationTimestamp")
    private long lastLocationTimestamp;

    @SerializedName("distanceRemainingMeters")
    private double distanceRemainingMeters;

    public Journey() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStartLocationName() { return startLocationName; }
    public void setStartLocationName(String startLocationName) { this.startLocationName = startLocationName; }

    public double getStartLatitude() { return startLatitude; }
    public void setStartLatitude(double startLatitude) { this.startLatitude = startLatitude; }

    public double getStartLongitude() { return startLongitude; }
    public void setStartLongitude(double startLongitude) { this.startLongitude = startLongitude; }

    public String getDestinationName() { return destinationName; }
    public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

    public double getDestinationLatitude() { return destinationLatitude; }
    public void setDestinationLatitude(double destinationLatitude) { this.destinationLatitude = destinationLatitude; }

    public double getDestinationLongitude() { return destinationLongitude; }
    public void setDestinationLongitude(double destinationLongitude) { this.destinationLongitude = destinationLongitude; }

    public int getExpectedDurationMinutes() { return expectedDurationMinutes; }
    public void setExpectedDurationMinutes(int expectedDurationMinutes) { this.expectedDurationMinutes = expectedDurationMinutes; }

    public long getExpectedArrivalTime() { return expectedArrivalTime; }
    public void setExpectedArrivalTime(long expectedArrivalTime) { this.expectedArrivalTime = expectedArrivalTime; }

    public String getTransportMode() { return transportMode; }
    public void setTransportMode(String transportMode) { this.transportMode = transportMode; }

    public List<String> getTrustedContactIds() { return trustedContactIds; }
    public void setTrustedContactIds(List<String> trustedContactIds) { this.trustedContactIds = trustedContactIds; }

    public int getCheckInIntervalMinutes() { return checkInIntervalMinutes; }
    public void setCheckInIntervalMinutes(int checkInIntervalMinutes) { this.checkInIntervalMinutes = checkInIntervalMinutes; }

    public boolean isMonitoringEnabled() { return monitoringEnabled; }
    public void setMonitoringEnabled(boolean monitoringEnabled) { this.monitoringEnabled = monitoringEnabled; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getStartedAt() { return startedAt; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getNextCheckInDueAt() { return nextCheckInDueAt; }
    public void setNextCheckInDueAt(long nextCheckInDueAt) { this.nextCheckInDueAt = nextCheckInDueAt; }

    public double getLastLatitude() { return lastLatitude; }
    public void setLastLatitude(double lastLatitude) { this.lastLatitude = lastLatitude; }

    public double getLastLongitude() { return lastLongitude; }
    public void setLastLongitude(double lastLongitude) { this.lastLongitude = lastLongitude; }

    public float getLastAccuracyMeters() { return lastAccuracyMeters; }
    public void setLastAccuracyMeters(float lastAccuracyMeters) { this.lastAccuracyMeters = lastAccuracyMeters; }

    public long getLastLocationTimestamp() { return lastLocationTimestamp; }
    public void setLastLocationTimestamp(long lastLocationTimestamp) { this.lastLocationTimestamp = lastLocationTimestamp; }

    public double getDistanceRemainingMeters() { return distanceRemainingMeters; }
    public void setDistanceRemainingMeters(double distanceRemainingMeters) { this.distanceRemainingMeters = distanceRemainingMeters; }
}
