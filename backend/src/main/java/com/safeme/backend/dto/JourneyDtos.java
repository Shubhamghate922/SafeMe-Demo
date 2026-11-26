package com.safeme.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public final class JourneyDtos {

    private JourneyDtos() {}

    public static class CreateJourneyRequest {
        private String startLocationName;
        private double startLatitude;
        private double startLongitude;

        @NotBlank(message = "Destination name is required.")
        private String destinationName;

        private double destinationLatitude;
        private double destinationLongitude;

        @Min(value = 1, message = "Expected duration must be at least 1 minute.")
        private int expectedDurationMinutes;

        private long expectedArrivalTime;
        private String transportMode;
        private List<String> trustedContactIds;

        @Min(value = 1, message = "Check-in interval must be at least 1 minute.")
        private int checkInIntervalMinutes;

        private boolean monitoringEnabled = true;

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
    }

    public static class UpdateJourneyRequest {
        @NotBlank(message = "Destination name is required.")
        private String destinationName;
        private double destinationLatitude;
        private double destinationLongitude;

        public String getDestinationName() { return destinationName; }
        public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

        public double getDestinationLatitude() { return destinationLatitude; }
        public void setDestinationLatitude(double destinationLatitude) { this.destinationLatitude = destinationLatitude; }

        public double getDestinationLongitude() { return destinationLongitude; }
        public void setDestinationLongitude(double destinationLongitude) { this.destinationLongitude = destinationLongitude; }
    }

    public static class ExtendJourneyRequest {
        @Min(value = 1, message = "Additional minutes must be positive.")
        private int additionalMinutes;

        public int getAdditionalMinutes() { return additionalMinutes; }
        public void setAdditionalMinutes(int additionalMinutes) { this.additionalMinutes = additionalMinutes; }
    }

    public static class TriggerSosRequest {
        private String journeyId;
        private String triggerSource;
        private double latitude;
        private double longitude;
        private float accuracyMeters;
        private String note;

        public String getJourneyId() { return journeyId; }
        public void setJourneyId(String journeyId) { this.journeyId = journeyId; }

        public String getTriggerSource() { return triggerSource; }
        public void setTriggerSource(String triggerSource) { this.triggerSource = triggerSource; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public float getAccuracyMeters() { return accuracyMeters; }
        public void setAccuracyMeters(float accuracyMeters) { this.accuracyMeters = accuracyMeters; }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class ResolveSosRequest {
        private String resolutionNote;

        public String getResolutionNote() { return resolutionNote; }
        public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    }

    public static class EscalationRequest {
        @Min(value = 1, message = "Escalation stage must be at least 1.")
        private int stage;
        private String reason;

        public int getStage() { return stage; }
        public void setStage(int stage) { this.stage = stage; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
