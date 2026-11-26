package com.safeme.backend.controller;

import com.safeme.backend.config.JwtService;
import com.safeme.backend.dto.ApiResponse;
import com.safeme.backend.dto.AuthDtos;
import com.safeme.backend.exception.ApiException;
import com.safeme.backend.model.OtpVerificationDocument;
import com.safeme.backend.model.UserDocument;
import com.safeme.backend.repository.OtpVerificationRepository;
import com.safeme.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean otpRequired;

    public AuthController(
            UserRepository userRepository,
            OtpVerificationRepository otpRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${safeme.auth.otp-required:false}") boolean otpRequired) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpRequired = otpRequired;
    }

    @PostMapping("/register")
    public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String mobile = normalizeMobile(request.getMobileNumber());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        if (userRepository.existsByMobileNumber(mobile)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this mobile number already exists.");
        }

        UserDocument user = new UserDocument();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setVerified(!otpRequired);
        user.setCreatedAt(System.currentTimeMillis());
        user = userRepository.save(user);

        if (otpRequired) {
            issueOtp(user.getId(), mobile, "REGISTRATION");
            return ApiResponse.ok(new AuthDtos.AuthResponse(
                    null, "Bearer", 0, user, true, maskValue(mobile)
            ), "Account created. Please verify OTP.");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return ApiResponse.ok(new AuthDtos.AuthResponse(
                token, "Bearer", jwtService.getExpirationSeconds(), user, false, null
        ), "Registration successful.");
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        UserDocument user = findByEmailOrMobile(identifier)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile or password.");
        }

        if (otpRequired && !user.isVerified()) {
            issueOtp(user.getId(), user.getMobileNumber(), "REGISTRATION");
            return ApiResponse.ok(new AuthDtos.AuthResponse(
                    null, "Bearer", 0, user, true, maskValue(user.getMobileNumber())
            ), "Account verification required.");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return ApiResponse.ok(new AuthDtos.AuthResponse(
                token, "Bearer", jwtService.getExpirationSeconds(), user, false, null
        ), "Login successful.");
    }

    @PostMapping("/verify-otp")
    public ApiResponse<AuthDtos.AuthResponse> verifyOtp(@Valid @RequestBody AuthDtos.VerifyOtpRequest request) {
        String target = (request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty())
                ? normalizeMobile(request.getMobileNumber())
                : (request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "");

        if (target.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email or mobile number is required.");
        }

        String purpose = request.getPurpose() != null ? request.getPurpose().trim().toUpperCase() : "REGISTRATION";
        OtpVerificationDocument otpDoc = otpRepository
                .findTopByTargetIgnoreCaseAndPurposeOrderByCreatedAtDesc(target, purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "No active OTP found. Please request a new code."));

        if (otpDoc.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OTP has expired. Please request a new code.");
        }
        if (otpDoc.getAttempts() >= 5) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid attempts. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(request.getOtp().trim(), otpDoc.getOtpHash())) {
            otpDoc.setAttempts(otpDoc.getAttempts() + 1);
            otpRepository.save(otpDoc);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid OTP code.");
        }

        UserDocument user = userRepository.findById(otpDoc.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User account not found."));
        user.setVerified(true);
        user = userRepository.save(user);
        otpRepository.deleteByTargetIgnoreCaseAndPurpose(target, purpose);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return ApiResponse.ok(new AuthDtos.AuthResponse(
                token, "Bearer", jwtService.getExpirationSeconds(), user, false, null
        ), "Verification successful.");
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody AuthDtos.ResendOtpRequest request) {
        String target = normalizeTarget(request.getMobileNumber());
        String purpose = request.getPurpose() != null ? request.getPurpose().trim().toUpperCase() : "REGISTRATION";
        findByEmailOrMobile(target).ifPresent(user -> issueOtp(user.getId(), target, purpose));
        return ApiResponse.ok(null, "If an account matches that contact, a fresh OTP has been issued.");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<AuthDtos.AuthResponse> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        String identifier = normalizeTarget(request.getIdentifier());
        findByEmailOrMobile(identifier).ifPresent(user -> issueOtp(user.getId(), identifier, "PASSWORD_RESET"));
        return ApiResponse.ok(new AuthDtos.AuthResponse(
                null, "Bearer", 0, null, true, maskValue(identifier)
        ), "If an account exists for this identifier, a reset OTP has been dispatched.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        String target = normalizeTarget(request.getIdentifier());
        OtpVerificationDocument otpDoc = otpRepository
                .findTopByTargetIgnoreCaseAndPurposeOrderByCreatedAtDesc(target, "PASSWORD_RESET")
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset OTP."));

        if (otpDoc.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Reset OTP has expired.");
        }
        if (otpDoc.getAttempts() >= 5) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many invalid OTP attempts.");
        }
        if (!passwordEncoder.matches(request.getOtp().trim(), otpDoc.getOtpHash())) {
            otpDoc.setAttempts(otpDoc.getAttempts() + 1);
            otpRepository.save(otpDoc);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid reset OTP.");
        }

        UserDocument user = userRepository.findById(otpDoc.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User account not found."));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        otpRepository.deleteByTargetIgnoreCaseAndPurpose(target, "PASSWORD_RESET");

        return ApiResponse.ok(null, "Password reset successfully.");
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication) {
        if (authentication != null && authentication.getName() != null) {
            userRepository.findById(authentication.getName()).ifPresent(user -> {
                user.setFcmToken(null);
                userRepository.save(user);
            });
        }
        return ApiResponse.ok(null, "Signed out successfully.");
    }

    private Optional<UserDocument> findByEmailOrMobile(String identifier) {
        if (identifier.contains("@")) {
            return userRepository.findByEmailIgnoreCase(identifier.trim().toLowerCase());
        }
        return userRepository.findByMobileNumber(normalizeMobile(identifier));
    }

    private String normalizeTarget(String input) {
        String trimmed = input.trim();
        return trimmed.contains("@") ? trimmed.toLowerCase() : normalizeMobile(trimmed);
    }

    private String normalizeMobile(String mobile) {
        return mobile.replaceAll("[\\s\\-()]", "");
    }

    private void issueOtp(String userId, String target, String purpose) {
        otpRepository.deleteByTargetIgnoreCaseAndPurpose(target, purpose);
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        OtpVerificationDocument doc = new OtpVerificationDocument();
        doc.setUserId(userId);
        doc.setTarget(target);
        doc.setPurpose(purpose);
        doc.setOtpHash(passwordEncoder.encode(code));
        doc.setExpiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));
        doc.setAttempts(0);
        doc.setCreatedAt(System.currentTimeMillis());
        otpRepository.save(doc);
        // Delivery integration hook (SMTP/SMS provider goes here; never return OTP in API response)
        log.info("Issued {} OTP record for userId={}", purpose, userId);
    }

    private String maskValue(String raw) {
        if (raw == null || raw.length() < 4) return "****";
        return "****" + raw.substring(raw.length() - 4);
    }
}
