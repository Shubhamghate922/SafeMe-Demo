package com.safeme.backend.controller;

import com.safeme.backend.dto.ApiResponse;
import com.safeme.backend.dto.JourneyDtos;
import com.safeme.backend.exception.ApiException;
import com.safeme.backend.model.*;
import com.safeme.backend.repository.*;
import com.safeme.backend.service.FcmNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/journeys")
public class JourneyController {

    private final JourneyRepository journeyRepository;
    private final SafetyCheckInRepository checkInRepository;
    private final LocationUpdateRepository locationRepository;
    private final EmergencyEscalationRepository escalationRepository;
    private final TrustedContactRepository contactRepository;
    private final SosAlertRepository sosRepository;
    private final UserRepository userRepository;
    private final FcmNotificationService notificationService;

    public JourneyController(
            JourneyRepository journeyRepository,
            SafetyCheckInRepository checkInRepository,
            LocationUpdateRepository locationRepository,
            EmergencyEscalationRepository escalationRepository,
            TrustedContactRepository contactRepository,
            SosAlertRepository sosRepository,
            UserRepository userRepository,
            FcmNotificationService notificationService) {
        this.journeyRepository = journeyRepository;
        this.checkInRepository = checkInRepository;
        this.locationRepository = locationRepository;
        this.escalationRepository = escalationRepository;
        this.contactRepository = contactRepository;
        this.sosRepository = sosRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ApiResponse<JourneyDocument> createJourney(
            Authentication auth,
            @Valid @RequestBody JourneyDtos.CreateJourneyRequest request) {
        long now = System.currentTimeMillis();
        JourneyDocument doc = new JourneyDocument();
        doc.setUserId(auth.getName());
        doc.setStartLocationName(request.getStartLocationName() != null ? request.getStartLocationName() : "Current Location");
        doc.setStartLatitude(request.getStartLatitude());
        doc.setStartLongitude(request.getStartLongitude());
        doc.setDestinationName(request.getDestinationName().trim());
        doc.setDestinationLatitude(request.getDestinationLatitude());
        doc.setDestinationLongitude(request.getDestinationLongitude());
        doc.setExpectedDurationMinutes(request.getExpectedDurationMinutes());
        doc.setExpectedArrivalTime(request.getExpectedArrivalTime() > 0
                ? request.getExpectedArrivalTime()
                : now + (long) request.getExpectedDurationMinutes() * 60_000L);
        doc.setTransportMode(request.getTransportMode() != null ? request.getTransportMode() : "WALKING");
        doc.setTrustedContactIds(request.getTrustedContactIds() != null ? request.getTrustedContactIds() : new ArrayList<>());
        doc.setCheckInIntervalMinutes(Math.max(1, request.getCheckInIntervalMinutes()));
        doc.setMonitoringEnabled(request.isMonitoringEnabled());
        doc.setStatus("CREATED");
        doc.setLastLatitude(request.getStartLatitude());
        doc.setLastLongitude(request.getStartLongitude());
        doc.setLastLocationTimestamp(now);
        return ApiResponse.ok(journeyRepository.save(doc), "Journey created.");
    }

    @GetMapping
    public ApiResponse<List<JourneyDocument>> getJourneys(Authentication auth) {
        return ApiResponse.ok(journeyRepository.findByUserIdOrderByStartedAtDesc(auth.getName()));
    }

    @GetMapping("/active")
    public ApiResponse<JourneyDocument> getActiveJourney(Authentication auth) {
        JourneyDocument active = journeyRepository
                .findFirstByUserIdAndStatusInOrderByStartedAtDesc(auth.getName(), List.of("ACTIVE", "ESCALATED"))
                .orElse(null);
        return ApiResponse.ok(active, active != null ? "Active journey found." : "No active journey.");
    }

    @GetMapping("/{id}")
    public ApiResponse<JourneyDocument> getJourneyById(Authentication auth, @PathVariable String id) {
        return ApiResponse.ok(getOwnedJourney(auth.getName(), id));
    }

    @PutMapping("/{id}")
    public ApiResponse<JourneyDocument> updateJourney(
            Authentication auth,
            @PathVariable String id,
            @Valid @RequestBody JourneyDtos.UpdateJourneyRequest request) {
        JourneyDocument journey = getOwnedJourney(auth.getName(), id);
        journey.setDestinationName(request.getDestinationName().trim());
        journey.setDestinationLatitude(request.getDestinationLatitude());
        journey.setDestinationLongitude(request.getDestinationLongitude());
        return ApiResponse.ok(journeyRepository.save(journey), "Journey destination updated.");
    }

    @PostMapping("/{id}/start")
    public ApiResponse<JourneyDocument> startJourney(Authentication auth, @PathVariable String id) {
        JourneyDocument journey = getOwnedJourney(auth.getName(), id);
        long now = System.currentTimeMillis();
        journey.setStatus("ACTIVE");
        if (journey.getStartedAt() == 0) {
            journey.setStartedAt(now);
        }
        journey.setNextCheckInDueAt(now + (long) Math.max(1, journey.getCheckInIntervalMinutes()) * 60_000L);
        JourneyDocument saved = journeyRepository.save(journey);

        notifyJourneyContacts(saved, "SYSTEM", "Journey Monitoring Started",
                "Started monitored journey to " + saved.getDestinationName(), false);
        return ApiResponse.ok(saved, "Journey monitoring started.");
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<JourneyDocument> completeJourney(Authentication auth, @PathVariable String id) {
        JourneyDocument journey = getOwnedJourney(auth.getName(), id);
        journey.setStatus("COMPLETED");
        journey.setCompletedAt(System.currentTimeMillis());
        JourneyDocument saved = journeyRepository.save(journey);

        notifyJourneyContacts(saved, "SYSTEM", "Safe Arrival Confirmed",
                "Completed journey safely at " + saved.getDestinationName(), false);
        return ApiResponse.ok(saved, "Journey completed safely.");
    }

    @PostMapping("/{id}/extend")
    public ApiResponse<JourneyDocument> extendJourney(
            Authentication auth,
            @PathVariable String id,
            @Valid @RequestBody JourneyDtos.ExtendJourneyRequest request) {
        JourneyDocument journey = getOwnedJourney(auth.getName(), id);
        long addMs = (long) request.getAdditionalMinutes() * 60_000L;
        journey.setExpectedDurationMinutes(journey.getExpectedDurationMinutes() + request.getAdditionalMinutes());
        journey.setExpectedArrivalTime(Math.max(System.currentTimeMillis(), journey.getExpectedArrivalTime()) + addMs);
        if (journey.getNextCheckInDueAt() > 0) {
            journey.setNextCheckInDueAt(journey.getNextCheckInDueAt() + addMs);
        }
        return ApiResponse.ok(journeyRepository.save(journey), "Journey ETA extended.");
    }

    @PostMapping("/{id}/locations")
    public ApiResponse<LocationUpdateDocument> sendJourneyLocation(
            Authentication auth,
            @PathVariable String id,
            @RequestBody LocationUpdateDocument request) {
        JourneyDocument journey = getOwnedJourney(auth.getName(), id);
        long now = request.getRecordedAt() > 0 ? request.getRecordedAt() : System.currentTimeMillis();

        LocationUpdateDocument loc = new LocationUpdateDocument();
        loc.setSessionId(journey.getId());
        loc.setSessionType("JOURNEY");
        loc.setUserId(auth.getName());
        loc.setLatitude(request.getLatitude());
        loc.setLongitude(request.getLongitude());
        loc.setAccuracyMeters(request.getAccuracyMeters());
        loc.setSpeedMps(request.getSpeedMps());
        loc.setBearing(request.getBearing());
        loc.setRecordedAt(now);
        locationRepository.save(loc);

        journey.setLastLatitude(request.getLatitude());
        journey.setLastLongitude(request.getLongitude());
        journey.setLastAccuracyMeters(request.getAccuracyMeters());
        journey.setLastLocationTimestamp(now);
        journey.setDistanceRemainingMeters(haversineMeters(
                request.getLatitude(), request.getLongitude(),
                journey.getDestinationLatitude(), journey.getDestinationLongitude()));
        journeyRepository.save(journey);

        return ApiResponse.ok(loc, "Journey location recorded.");
    }

    @PostMapping("/{id}/checkins")
    public ApiResponse<SafetyCheckInDocument> submitCheckIn(
            Authentication auth,
            @PathVariable String id,
            @RequestBody SafetyCheckInDocument request) {
        JourneyDocument journey = getOwnedJourney(auth.getName(), id);
        long now = System.currentTimeMillis();
        long nextDue = now + (long) Math.max(1, journey.getCheckInIntervalMinutes()) * 60_000L;

        SafetyCheckInDocument checkIn = new SafetyCheckInDocument();
        checkIn.setJourneyId(journey.getId());
        checkIn.setUserId(auth.getName());
        checkIn.setStatus(request.getStatus() != null ? request.getStatus() : "CONFIRMED_SAFE");
        checkIn.setLatitude(request.getLatitude());
        checkIn.setLongitude(request.getLongitude());
        checkIn.setNote(request.getNote());
        checkIn.setCheckedInAt(now);
        checkIn.setNextCheckInDueAt(nextDue);
        SafetyCheckInDocument saved = checkInRepository.save(checkIn);

        journey.setNextCheckInDueAt(nextDue);
        journeyRepository.save(journey);

        return ApiResponse.ok(saved, "Safety check-in recorded.");
    }

    @GetMapping("/{id}/checkins")
    public ApiResponse<List<SafetyCheckInDocument>> getCheckIns(Authentication auth, @PathVariable String id) {
        getOwnedJourney(auth.getName(), id);
        return ApiResponse.ok(checkInRepository.findByJourneyIdOrderByCheckedInAtDesc(id));
    }

    @PostMapping("/{id}/escalate")
    public ApiResponse<EmergencyEscalationDocument> escalateJourney(
            Authentication auth,
            @PathVariable String id,
            @Valid @RequestBody JourneyDtos.EscalationRequest request) {
        JourneyDocument journey = getOwnedJourney(auth.getName(), id);
        journey.setStatus("ESCALATED");
        journeyRepository.save(journey);

        int notifiedCount = 0;
        if (request.getStage() == 2) {
            notifiedCount = notifyJourneyContacts(journey, "ESCALATION", "Missed Check-In Alert",
                    "Priority contact alert: Missed safety check-in on journey to " + journey.getDestinationName(), true);
        } else if (request.getStage() >= 3) {
            notifiedCount = notifyJourneyContacts(journey, "ESCALATION", "Urgent Emergency Escalation",
                    "Urgent: Unacknowledged missed check-in on journey to " + journey.getDestinationName(), false);
            SosAlertDocument sos = new SosAlertDocument();
            sos.setUserId(auth.getName());
            sos.setSenderName(userRepository.findById(auth.getName()).map(UserDocument::getFullName).orElse("SafeMe User"));
            sos.setJourneyId(journey.getId());
            sos.setTriggerSource("MISSED_CHECKIN");
            sos.setStatus("ACTIVE");
            sos.setLatitude(journey.getLastLatitude());
            sos.setLongitude(journey.getLastLongitude());
            sos.setAccuracyMeters(journey.getLastAccuracyMeters());
            sos.setAddressLabel(journey.getDestinationName());
            sos.setNotificationDeliveryStatus("SENT");
            sos.setCreatedAt(System.currentTimeMillis());
            sosRepository.save(sos);
        }

        EmergencyEscalationDocument escalation = new EmergencyEscalationDocument();
        escalation.setJourneyId(journey.getId());
        escalation.setUserId(auth.getName());
        escalation.setCurrentStage(request.getStage());
        escalation.setStatus("ESCALATED_STAGE_" + request.getStage());
        escalation.setNotifiedContactsCount(notifiedCount);
        escalation.setTriggeredAt(System.currentTimeMillis());
        return ApiResponse.ok(escalationRepository.save(escalation), "Escalation stage " + request.getStage() + " executed.");
    }

    private JourneyDocument getOwnedJourney(String userId, String journeyId) {
        return journeyRepository.findByIdAndUserId(journeyId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Journey not found."));
    }

    private int notifyJourneyContacts(JourneyDocument journey, String type, String title, String msg, boolean priorityOneOnly) {
        List<TrustedContactDocument> contacts = contactRepository.findByOwnerUserIdOrderByPriorityAsc(journey.getUserId());
        int count = 0;
        for (TrustedContactDocument c : contacts) {
            if (journey.getTrustedContactIds() != null && !journey.getTrustedContactIds().isEmpty()
                    && !journey.getTrustedContactIds().contains(c.getId())) {
                continue;
            }
            if (priorityOneOnly && c.getPriority() != 1) {
                continue;
            }
            if (c.isNotifyPush() && c.getLinkedSafeMeUserId() != null) {
                notificationService.notifyUser(c.getLinkedSafeMeUserId(), type, journey.getId(), title, msg);
            }
            count++;
        }
        return count;
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        if (lat2 == 0 && lon2 == 0) return 0;
        double r = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
