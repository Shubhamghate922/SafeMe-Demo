package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Request DTOs for Journey, SOS, and Escalation operations.
 */
public final class JourneyDtos {

    private JourneyDtos() {}

    public static class CreateJourneyRequest {
        @SerializedName("startLocationName")
        public final String startLocationName;
        @SerializedName("startLatitude")
        public final double startLatitude;
        @SerializedName("startLongitude")
        public final double startLongitude;
        @SerializedName("destinationName")
        public final String destinationName;
        @SerializedName("destinationLatitude")
        public final double destinationLatitude;
        @SerializedName("destinationLongitude")
        public final double destinationLongitude;
        @SerializedName("expectedDurationMinutes")
        public final int expectedDurationMinutes;
        @SerializedName("expectedArrivalTime")
        public final long expectedArrivalTime;
        @SerializedName("transportMode")
        public final String transportMode;
        @SerializedName("trustedContactIds")
        public final List<String> trustedContactIds;
        @SerializedName("checkInIntervalMinutes")
        public final int checkInIntervalMinutes;
        @SerializedName("monitoringEnabled")
        public final boolean monitoringEnabled;

        public CreateJourneyRequest(String startLocationName, double startLatitude, double startLongitude,
                                    String destinationName, double destinationLatitude, double destinationLongitude,
                                    int expectedDurationMinutes, long expectedArrivalTime, String transportMode,
                                    List<String> trustedContactIds, int checkInIntervalMinutes, boolean monitoringEnabled) {
            this.startLocationName = startLocationName;
            this.startLatitude = startLatitude;
            this.startLongitude = startLongitude;
            this.destinationName = destinationName;
            this.destinationLatitude = destinationLatitude;
            this.destinationLongitude = destinationLongitude;
            this.expectedDurationMinutes = expectedDurationMinutes;
            this.expectedArrivalTime = expectedArrivalTime;
            this.transportMode = transportMode;
            this.trustedContactIds = trustedContactIds;
            this.checkInIntervalMinutes = checkInIntervalMinutes;
            this.monitoringEnabled = monitoringEnabled;
        }
    }

    public static class UpdateJourneyRequest {
        @SerializedName("destinationName")
        public final String destinationName;
        @SerializedName("destinationLatitude")
        public final double destinationLatitude;
        @SerializedName("destinationLongitude")
        public final double destinationLongitude;

        public UpdateJourneyRequest(String destinationName, double destinationLatitude, double destinationLongitude) {
            this.destinationName = destinationName;
            this.destinationLatitude = destinationLatitude;
            this.destinationLongitude = destinationLongitude;
        }
    }

    public static class ExtendJourneyRequest {
        @SerializedName("additionalMinutes")
        public final int additionalMinutes;

        public ExtendJourneyRequest(int additionalMinutes) {
            this.additionalMinutes = additionalMinutes;
        }
    }

    public static class TriggerSosRequest {
        @SerializedName("journeyId")
        public final String journeyId;
        @SerializedName("triggerSource")
        public final String triggerSource;
        @SerializedName("latitude")
        public final double latitude;
        @SerializedName("longitude")
        public final double longitude;
        @SerializedName("accuracyMeters")
        public final float accuracyMeters;
        @SerializedName("note")
        public final String note;

        public TriggerSosRequest(String journeyId, String triggerSource, double latitude,
                                 double longitude, float accuracyMeters, String note) {
            this.journeyId = journeyId;
            this.triggerSource = triggerSource;
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracyMeters = accuracyMeters;
            this.note = note;
        }
    }

    public static class ResolveSosRequest {
        @SerializedName("resolutionNote")
        public final String resolutionNote;

        public ResolveSosRequest(String resolutionNote) {
            this.resolutionNote = resolutionNote;
        }
    }

    public static class EscalationRequest {
        @SerializedName("stage")
        public final int stage;
        @SerializedName("reason")
        public final String reason;

        public EscalationRequest(int stage, String reason) {
            this.stage = stage;
            this.reason = reason;
        }
    }

    public static class EmergencyEscalation {
        @SerializedName("id")
        private String id;
        @SerializedName("journeyId")
        private String journeyId;
        @SerializedName("currentStage")
        private int currentStage;
        @SerializedName("status")
        private String status;
        @SerializedName("notifiedContactsCount")
        private int notifiedContactsCount;
        @SerializedName("triggeredAt")
        private long triggeredAt;

        public String getId() { return id; }
        public String getJourneyId() { return journeyId; }
        public int getCurrentStage() { return currentStage; }
        public String getStatus() { return status; }
        public int getNotifiedContactsCount() { return notifiedContactsCount; }
        public long getTriggeredAt() { return triggeredAt; }
    }
}
