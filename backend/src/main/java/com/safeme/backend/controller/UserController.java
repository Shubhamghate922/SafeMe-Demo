package com.safeme.backend.controller;

import com.safeme.backend.dto.ApiResponse;
import com.safeme.backend.dto.AuthDtos;
import com.safeme.backend.exception.ApiException;
import com.safeme.backend.model.UserDocument;
import com.safeme.backend.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationRepository otpRepository;
    private final TrustedContactRepository contactRepository;
    private final JourneyRepository journeyRepository;
    private final SafetyCheckInRepository checkInRepository;
    private final SosAlertRepository sosRepository;
    private final LocationUpdateRepository locationRepository;
    private final EmergencyEscalationRepository escalationRepository;
    private final IncidentReportRepository incidentRepository;
    private final AppNotificationRepository notificationRepository;

    public UserController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OtpVerificationRepository otpRepository,
            TrustedContactRepository contactRepository,
            JourneyRepository journeyRepository,
            SafetyCheckInRepository checkInRepository,
            SosAlertRepository sosRepository,
            LocationUpdateRepository locationRepository,
            EmergencyEscalationRepository escalationRepository,
            IncidentReportRepository incidentRepository,
            AppNotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpRepository = otpRepository;
        this.contactRepository = contactRepository;
        this.journeyRepository = journeyRepository;
        this.checkInRepository = checkInRepository;
        this.sosRepository = sosRepository;
        this.locationRepository = locationRepository;
        this.escalationRepository = escalationRepository;
        this.incidentRepository = incidentRepository;
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/me")
    public ApiResponse<UserDocument> getCurrentUser(Authentication auth) {
        UserDocument user = getAuthenticatedUser(auth);
        return ApiResponse.ok(user);
    }

    @PutMapping("/me")
    public ApiResponse<UserDocument> updateCurrentUser(
            Authentication auth,
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        UserDocument user = getAuthenticatedUser(auth);
        String mobile = request.getMobileNumber().replaceAll("[\\s\\-()]", "");
        userRepository.findByMobileNumber(mobile).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new ApiException(HttpStatus.CONFLICT, "Mobile number is already linked to another account.");
            }
        });
        user.setFullName(request.getFullName().trim());
        user.setMobileNumber(mobile);
        if (request.getProfilePhotoUrl() != null) {
            user.setProfilePhotoUrl(request.getProfilePhotoUrl().trim());
        }
        return ApiResponse.ok(userRepository.save(user), "Profile updated.");
    }

    @PostMapping("/me/change-password")
    public ApiResponse<Void> changePassword(
            Authentication auth,
            @Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        UserDocument user = getAuthenticatedUser(auth);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.ok(null, "Password updated successfully.");
    }

    @PostMapping("/me/device-token")
    public ApiResponse<Void> registerDeviceToken(
            Authentication auth,
            @Valid @RequestBody AuthDtos.DeviceTokenRequest request) {
        UserDocument user = getAuthenticatedUser(auth);
        user.setFcmToken(request.getFcmToken().trim());
        userRepository.save(user);
        return ApiResponse.ok(null, "Device FCM token registered.");
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteAccount(Authentication auth) {
        UserDocument user = getAuthenticatedUser(auth);
        String userId = user.getId();
        otpRepository.deleteByUserId(userId);
        contactRepository.deleteByOwnerUserId(userId);
        checkInRepository.deleteByUserId(userId);
        locationRepository.deleteByUserId(userId);
        escalationRepository.deleteByUserId(userId);
        journeyRepository.deleteByUserId(userId);
        sosRepository.deleteByUserId(userId);
        incidentRepository.deleteByUserId(userId);
        notificationRepository.deleteByRecipientUserId(userId);
        userRepository.deleteById(userId);
        return ApiResponse.ok(null, "Account and associated personal records deleted.");
    }

    private UserDocument getAuthenticatedUser(Authentication auth) {
        return userRepository.findById(auth.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found."));
    }
}
