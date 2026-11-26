package com.safeme.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "location_updates")
@CompoundIndex(name = "idx_loc_session_time", def = "{'sessionId': 1, 'recordedAt': -1}")
public class LocationUpdateDocument {

    @Id
    private String id;

    @Indexed
    private String sessionId; // sosId or journeyId

    @JsonIgnore
    private String sessionType; // JOURNEY or SOS

    @JsonIgnore
    private String userId;

    private double latitude;
    private double longitude;
    private float accuracyMeters;
    private float speedMps;
    private float bearing;
    private long recordedAt;

    public LocationUpdateDocument() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getAccuracyMeters() { return accuracyMeters; }
    public void setAccuracyMeters(float accuracyMeters) { this.accuracyMeters = accuracyMeters; }

    public float getSpeedMps() { return speedMps; }
    public void setSpeedMps(float speedMps) { this.speedMps = speedMps; }

    public float getBearing() { return bearing; }
    public void setBearing(float bearing) { this.bearing = bearing; }

    public long getRecordedAt() { return recordedAt; }
    public void setRecordedAt(long recordedAt) { this.recordedAt = recordedAt; }
}
