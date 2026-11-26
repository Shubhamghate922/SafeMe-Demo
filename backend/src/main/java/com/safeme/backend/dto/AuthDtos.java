package com.safeme.backend.dto;

import com.safeme.backend.model.UserDocument;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public static class RegisterRequest {
        @NotBlank(message = "Full name is required.")
        @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters.")
        private String fullName;

        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        private String email;

        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Mobile number must be 10 to 13 digits.")
        private String mobileNumber;

        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 128, message = "Password must be at least 8 characters.")
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getMobileNumber() { return mobileNumber; }
        public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {
        @NotBlank(message = "Email or mobile number is required.")
        private String identifier;

        @NotBlank(message = "Password is required.")
        private String password;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class VerifyOtpRequest {
        private String mobileNumber;
        private String email;

        @NotBlank(message = "OTP is required.")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits.")
        private String otp;

        private String purpose;

        public String getMobileNumber() { return mobileNumber; }
        public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }

        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
    }

    public static class ResendOtpRequest {
        @NotBlank(message = "Mobile number or email is required.")
        private String mobileNumber;
        private String purpose;

        public String getMobileNumber() { return mobileNumber; }
        public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
    }

    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email or mobile number is required.")
        private String identifier;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
    }

    public static class ResetPasswordRequest {
        @NotBlank(message = "Email or mobile number is required.")
        private String identifier;

        @NotBlank(message = "OTP is required.")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits.")
        private String otp;

        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 128, message = "Password must be at least 8 characters.")
        private String newPassword;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }

        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required.")
        private String currentPassword;

        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 128, message = "New password must be at least 8 characters.")
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class UpdateProfileRequest {
        @NotBlank(message = "Full name is required.")
        @Size(min = 2, max = 80, message = "Full name must be between 2 and 80 characters.")
        private String fullName;

        @NotBlank(message = "Mobile number is required.")
        @Pattern(regexp = "^[+]?[0-9]{10,13}$", message = "Mobile number must be 10 to 13 digits.")
        private String mobileNumber;

        private String profilePhotoUrl;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getMobileNumber() { return mobileNumber; }
        public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

        public String getProfilePhotoUrl() { return profilePhotoUrl; }
        public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
    }

    public static class DeviceTokenRequest {
        @NotBlank(message = "FCM token is required.")
        private String fcmToken;
        private String platform;

        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
    }

    public static class AuthResponse {
        private String accessToken;
        private String tokenType;
        private long expiresInSeconds;
        private UserDocument user;
        private boolean otpRequired;
        private String maskedDestination;

        public AuthResponse() {}

        public AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                            UserDocument user, boolean otpRequired, String maskedDestination) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresInSeconds = expiresInSeconds;
            this.user = user;
            this.otpRequired = otpRequired;
            this.maskedDestination = maskedDestination;
        }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public long getExpiresInSeconds() { return expiresInSeconds; }
        public void setExpiresInSeconds(long expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }

        public UserDocument getUser() { return user; }
        public void setUser(UserDocument user) { this.user = user; }

        public boolean isOtpRequired() { return otpRequired; }
        public void setOtpRequired(boolean otpRequired) { this.otpRequired = otpRequired; }

        public String getMaskedDestination() { return maskedDestination; }
        public void setMaskedDestination(String maskedDestination) { this.maskedDestination = maskedDestination; }
    }
}
