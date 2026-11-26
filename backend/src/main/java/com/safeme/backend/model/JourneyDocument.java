package com.safeme.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "journeys")
@CompoundIndex(name = "idx_journey_user_status", def = "{'userId': 1, 'status': 1}")
public class JourneyDocument {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String startLocationName;
    private double startLatitude;
    private double startLongitude;
    private String destinationName;
    private double destinationLatitude;
    private double destinationLongitude;
    private int expectedDurationMinutes;
    private long expectedArrivalTime;
    private String transportMode;
    private List<String> trustedContactIds = new ArrayList<>();
    private int checkInIntervalMinutes;
    private boolean monitoringEnabled = true;
    private String status; // CREATED, ACTIVE, COMPLETED, ESCALATED, CANCELLED
    private long startedAt;
    private long completedAt;
    private long nextCheckInDueAt;
    private double lastLatitude;
    private double lastLongitude;
    private float lastAccuracyMeters;
    private long lastLocationTimestamp;
    private double distanceRemainingMeters;

    public JourneyDocument() {}

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
