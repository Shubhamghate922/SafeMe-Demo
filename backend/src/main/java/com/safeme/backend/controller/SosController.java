package com.safeme.backend.controller;

import com.safeme.backend.dto.ApiResponse;
import com.safeme.backend.dto.JourneyDtos;
import com.safeme.backend.exception.ApiException;
import com.safeme.backend.model.*;
import com.safeme.backend.repository.*;
import com.safeme.backend.service.FcmNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sos")
public class SosController {

    private final SosAlertRepository sosRepository;
    private final LocationUpdateRepository locationRepository;
    private final ResponseAcknowledgementRepository acknowledgementRepository;
    private final TrustedContactRepository contactRepository;
    private final UserRepository userRepository;
    private final FcmNotificationService notificationService;

    public SosController(
            SosAlertRepository sosRepository,
            LocationUpdateRepository locationRepository,
            ResponseAcknowledgementRepository acknowledgementRepository,
            TrustedContactRepository contactRepository,
            UserRepository userRepository,
            FcmNotificationService notificationService) {
        this.sosRepository = sosRepository;
        this.locationRepository = locationRepository;
        this.acknowledgementRepository = acknowledgementRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ApiResponse<SosAlertDocument> triggerSos(
            Authentication auth,
            @RequestBody JourneyDtos.TriggerSosRequest request) {
        UserDocument sender = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));

        long now = System.currentTimeMillis();
        SosAlertDocument sos = new SosAlertDocument();
        sos.setUserId(sender.getId());
        sos.setSenderName(sender.getFullName());
        sos.setJourneyId(request.getJourneyId());
        sos.setTriggerSource(request.getTriggerSource() != null ? request.getTriggerSource() : "MANUAL_SOS");
        sos.setStatus("ACTIVE");
        sos.setLatitude(request.getLatitude());
        sos.setLongitude(request.getLongitude());
        sos.setAccuracyMeters(request.getAccuracyMeters());
        sos.setNotificationDeliveryStatus("DISPATCHED");
        sos.setCreatedAt(now);
        SosAlertDocument saved = sosRepository.save(sos);

        LocationUpdateDocument initialLoc = new LocationUpdateDocument();
        initialLoc.setSessionId(saved.getId());
        initialLoc.setSessionType("SOS");
        initialLoc.setUserId(sender.getId());
        initialLoc.setLatitude(request.getLatitude());
        initialLoc.setLongitude(request.getLongitude());
        initialLoc.setAccuracyMeters(request.getAccuracyMeters());
        initialLoc.setRecordedAt(now);
        locationRepository.save(initialLoc);

        List<TrustedContactDocument> contacts = contactRepository.findByOwnerUserIdOrderByPriorityAsc(sender.getId());
        for (TrustedContactDocument c : contacts) {
            if (c.isNotifyPush() && c.getLinkedSafeMeUserId() != null) {
                notificationService.notifyUser(
                        c.getLinkedSafeMeUserId(),
                        "SOS_ALERT",
                        saved.getId(),
                        "Emergency SOS from " + sender.getFullName(),
                        "Immediate assistance requested. Tap to view live location and respond.");
            }
        }

        return ApiResponse.ok(saved, "Emergency SOS triggered.");
    }

    @GetMapping("/active")
    public ApiResponse<SosAlertDocument> getActiveSos(Authentication auth) {
        SosAlertDocument active = sosRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(auth.getName(), "ACTIVE")
                .orElse(null);
        if (active != null) {
            active.setAcknowledgements(acknowledgementRepository.findBySosIdOrderByRespondedAtDesc(active.getId()));
        }
        return ApiResponse.ok(active);
    }

    @GetMapping("/history")
    public ApiResponse<List<SosAlertDocument>> getSosHistory(Authentication auth) {
        return ApiResponse.ok(sosRepository.findByUserIdOrderByCreatedAtDesc(auth.getName()));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<SosAlertDocument> resolveSos(
            Authentication auth,
            @PathVariable String id,
            @RequestBody(required = false) JourneyDtos.ResolveSosRequest request) {
        SosAlertDocument sos = sosRepository.findByIdAndUserId(id, auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOS alert not found."));
        sos.setStatus("RESOLVED");
        sos.setResolvedAt(System.currentTimeMillis());
        sos.setResolutionNote(request != null ? request.getResolutionNote() : "Marked safe by user.");
        SosAlertDocument saved = sosRepository.save(sos);

        List<TrustedContactDocument> contacts = contactRepository.findByOwnerUserIdOrderByPriorityAsc(auth.getName());
        for (TrustedContactDocument c : contacts) {
            if (c.isNotifyPush() && c.getLinkedSafeMeUserId() != null) {
                notificationService.notifyUser(
                        c.getLinkedSafeMeUserId(),
                        "SYSTEM",
                        saved.getId(),
                        "SOS Resolved",
                        sos.getSenderName() + " marked the emergency as resolved.");
            }
        }
        return ApiResponse.ok(saved, "SOS resolved.");
    }

    @PostMapping("/{id}/locations")
    public ApiResponse<LocationUpdateDocument> sendSosLocation(
            Authentication auth,
            @PathVariable String id,
            @RequestBody LocationUpdateDocument request) {
        SosAlertDocument sos = sosRepository.findByIdAndUserId(id, auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOS alert not found."));

        long now = request.getRecordedAt() > 0 ? request.getRecordedAt() : System.currentTimeMillis();
        LocationUpdateDocument loc = new LocationUpdateDocument();
        loc.setSessionId(sos.getId());
        loc.setSessionType("SOS");
        loc.setUserId(auth.getName());
        loc.setLatitude(request.getLatitude());
        loc.setLongitude(request.getLongitude());
        loc.setAccuracyMeters(request.getAccuracyMeters());
        loc.setSpeedMps(request.getSpeedMps());
        loc.setBearing(request.getBearing());
        loc.setRecordedAt(now);
        locationRepository.save(loc);

        sos.setLatitude(request.getLatitude());
        sos.setLongitude(request.getLongitude());
        sos.setAccuracyMeters(request.getAccuracyMeters());
        sosRepository.save(sos);

        return ApiResponse.ok(loc, "Live SOS location updated.");
    }

    @GetMapping("/{id}/locations/latest")
    public ApiResponse<LocationUpdateDocument> getLatestSosLocation(Authentication auth, @PathVariable String id) {
        SosAlertDocument sos = findAndAuthorizeResponderOrOwner(id, auth.getName());
        LocationUpdateDocument latest = locationRepository.findFirstBySessionIdOrderByRecordedAtDesc(sos.getId())
                .orElseGet(() -> {
                    LocationUpdateDocument fallback = new LocationUpdateDocument();
                    fallback.setSessionId(sos.getId());
                    fallback.setLatitude(sos.getLatitude());
                    fallback.setLongitude(sos.getLongitude());
                    fallback.setAccuracyMeters(sos.getAccuracyMeters());
                    fallback.setRecordedAt(sos.getCreatedAt());
                    return fallback;
                });
        return ApiResponse.ok(latest);
    }

    @PostMapping("/{id}/acknowledgements")
    public ApiResponse<ResponseAcknowledgementDocument> submitAcknowledgement(
            Authentication auth,
            @PathVariable String id,
            @RequestBody ResponseAcknowledgementDocument request) {
        SosAlertDocument sos = findAndAuthorizeResponderOrOwner(id, auth.getName());
        UserDocument responder = userRepository.findById(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));

        ResponseAcknowledgementDocument ack = new ResponseAcknowledgementDocument();
        ack.setSosId(sos.getId());
        ack.setResponderUserId(responder.getId());
        ack.setResponderName(responder.getFullName());
        ack.setResponseStatus(request.getResponseStatus() != null ? request.getResponseStatus() : "RESPONDING");
        ack.setNote(request.getNote());
        ack.setRespondedAt(System.currentTimeMillis());
        ResponseAcknowledgementDocument saved = acknowledgementRepository.save(ack);

        notificationService.notifyUser(
                sos.getUserId(),
                "ACKNOWLEDGEMENT",
                sos.getId(),
                responder.getFullName() + " responded to your SOS",
                "Status: " + saved.getResponseStatus());

        return ApiResponse.ok(saved, "Acknowledgement recorded.");
    }

    @GetMapping("/{id}/acknowledgements")
    public ApiResponse<List<ResponseAcknowledgementDocument>> getAcknowledgements(
            Authentication auth,
            @PathVariable String id) {
        findAndAuthorizeResponderOrOwner(id, auth.getName());
        return ApiResponse.ok(acknowledgementRepository.findBySosIdOrderByRespondedAtDesc(id));
    }

    private SosAlertDocument findAndAuthorizeResponderOrOwner(String sosId, String callerUserId) {
        SosAlertDocument sos = sosRepository.findById(sosId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOS alert not found."));
        boolean isOwner = sos.getUserId().equals(callerUserId);
        boolean isTrustedResponder = contactRepository.existsByOwnerUserIdAndLinkedSafeMeUserId(sos.getUserId(), callerUserId);
        if (!isOwner && !isTrustedResponder) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to access this SOS alert.");
        }
        return sos;
    }
}
