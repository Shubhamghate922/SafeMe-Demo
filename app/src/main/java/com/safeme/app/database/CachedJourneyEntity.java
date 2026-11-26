package com.safeme.app.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Local Room cache entity for offline viewing of the user's active or recent journeys.
 * Never replaces authoritative Spring Boot + MongoDB persistence.
 */
@Entity(tableName = "cached_journeys")
public class CachedJourneyEntity {
    @PrimaryKey
    @NonNull
    public String id;
    public String destinationName;
    public String startLocationName;
    public String transportMode;
    public String status;
    public long expectedArrivalTime;
    public long nextCheckInDueAt;
    public double lastLatitude;
    public double lastLongitude;
    public long lastLocationTimestamp;
    public long cachedAt;

    public CachedJourneyEntity(@NonNull String id, String destinationName, String startLocationName,
                               String transportMode, String status, long expectedArrivalTime,
                               long nextCheckInDueAt, double lastLatitude, double lastLongitude,
                               long lastLocationTimestamp, long cachedAt) {
        this.id = id;
        this.destinationName = destinationName;
        this.startLocationName = startLocationName;
        this.transportMode = transportMode;
        this.status = status;
        this.expectedArrivalTime = expectedArrivalTime;
        this.nextCheckInDueAt = nextCheckInDueAt;
        this.lastLatitude = lastLatitude;
        this.lastLongitude = lastLongitude;
        this.lastLocationTimestamp = lastLocationTimestamp;
        this.cachedAt = cachedAt;
    }
}
