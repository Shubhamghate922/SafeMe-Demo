package com.safeme.backend.controller;

import com.safeme.backend.dto.ApiResponse;
import com.safeme.backend.exception.ApiException;
import com.safeme.backend.model.AppNotificationDocument;
import com.safeme.backend.model.CommunityReportDocument;
import com.safeme.backend.model.IncidentReportDocument;
import com.safeme.backend.repository.AppNotificationRepository;
import com.safeme.backend.repository.CommunityReportRepository;
import com.safeme.backend.repository.IncidentReportRepository;
import com.safeme.backend.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SafetyFeaturesController {

    private static final long SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L;

    private final CommunityReportRepository communityRepository;
    private final IncidentReportRepository incidentRepository;
    private final AppNotificationRepository notificationRepository;
    private final FileStorageService fileStorageService;

    public SafetyFeaturesController(
            CommunityReportRepository communityRepository,
            IncidentReportRepository incidentRepository,
            AppNotificationRepository notificationRepository,
            FileStorageService fileStorageService) {
        this.communityRepository = communityRepository;
        this.incidentRepository = incidentRepository;
        this.notificationRepository = notificationRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/community/reports")
    public ApiResponse<CommunityReportDocument> submitCommunityReport(
            Authentication auth,
            @RequestBody CommunityReportDocument request) {
        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Report description is required.");
        }
        long now = System.currentTimeMillis();
        CommunityReportDocument doc = new CommunityReportDocument();
        doc.setReporterUserId(auth.getName());
        doc.setCategory(request.getCategory() != null ? request.getCategory() : "OTHER");
        doc.setDescription(request.getDescription().trim());
        doc.setLocationName(request.getLocationName() != null ? request.getLocationName().trim() : "Nearby Area");
        doc.setLatitude(request.getLatitude());
        doc.setLongitude(request.getLongitude());
        doc.setReportedAt(now);
        doc.setExpiresAt(now + SEVEN_DAYS_MS);
        doc.setVerificationStatus("VERIFIED");
        return ApiResponse.ok(communityRepository.save(doc), "Community advisory submitted.");
    }

    @GetMapping("/community/reports")
    public ApiResponse<List<CommunityReportDocument>> getCommunityReports() {
        return ApiResponse.ok(communityRepository.findByExpiresAtGreaterThanOrderByReportedAtDesc(System.currentTimeMillis()));
    }

    @PostMapping("/incidents/reports")
    public ApiResponse<IncidentReportDocument> submitIncidentReport(
            Authentication auth,
            @RequestParam("category") String category,
            @RequestParam("description") String description,
            @RequestParam("incidentDate") String incidentDate,
            @RequestParam("locationName") String locationName,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value = "audio", required = false) MultipartFile audio) {

        IncidentReportDocument doc = new IncidentReportDocument();
        doc.setUserId(auth.getName());
        doc.setCategory(category);
        doc.setDescription(description);
        doc.setIncidentDate(incidentDate);
        doc.setLocationName(locationName);
        doc.setLatitude(latitude);
        doc.setLongitude(longitude);
        doc.setPhotoUrl(fileStorageService.storeEvidenceFile(photo, "photo"));
        doc.setAudioUrl(fileStorageService.storeEvidenceFile(audio, "audio"));
        doc.setStatus("SUBMITTED");
        doc.setCreatedAt(System.currentTimeMillis());
        return ApiResponse.ok(incidentRepository.save(doc), "Incident report submitted.");
    }

    @GetMapping("/incidents/reports")
    public ApiResponse<List<IncidentReportDocument>> getIncidentReports(Authentication auth) {
        return ApiResponse.ok(incidentRepository.findByUserIdOrderByCreatedAtDesc(auth.getName()));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<AppNotificationDocument>> getNotifications(Authentication auth) {
        return ApiResponse.ok(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(auth.getName()));
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<AppNotificationDocument> markNotificationRead(
            Authentication auth,
            @PathVariable String id) {
        AppNotificationDocument doc = notificationRepository.findByIdAndRecipientUserId(id, auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found."));
        doc.setRead(true);
        return ApiResponse.ok(notificationRepository.save(doc));
    }
}
