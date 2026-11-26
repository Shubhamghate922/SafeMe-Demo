package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request and Response DTOs for authentication, OTP verification, and account management.
 */
public final class AuthDtos {

    private AuthDtos() {}

    public static class RegisterRequest {
        @SerializedName("fullName")
        public final String fullName;
        @SerializedName("email")
        public final String email;
        @SerializedName("mobileNumber")
        public final String mobileNumber;
        @SerializedName("password")
        public final String password;

        public RegisterRequest(String fullName, String email, String mobileNumber, String password) {
            this.fullName = fullName;
            this.email = email;
            this.mobileNumber = mobileNumber;
            this.password = password;
        }
    }

    public static class LoginRequest {
        @SerializedName("identifier")
        public final String identifier;
        @SerializedName("password")
        public final String password;

        public LoginRequest(String identifier, String password) {
            this.identifier = identifier;
            this.password = password;
        }
    }

    public static class VerifyOtpRequest {
        @SerializedName("mobileNumber")
        public final String mobileNumber;
        @SerializedName("email")
        public final String email;
        @SerializedName("otp")
        public final String otp;
        @SerializedName("purpose")
        public final String purpose; // REGISTRATION or PASSWORD_RESET

        public VerifyOtpRequest(String mobileNumber, String email, String otp, String purpose) {
            this.mobileNumber = mobileNumber;
            this.email = email;
            this.otp = otp;
            this.purpose = purpose;
        }
    }

    public static class ResendOtpRequest {
        @SerializedName("mobileNumber")
        public final String mobileNumber;
        @SerializedName("purpose")
        public final String purpose;

        public ResendOtpRequest(String mobileNumber, String purpose) {
            this.mobileNumber = mobileNumber;
            this.purpose = purpose;
        }
    }

    public static class ForgotPasswordRequest {
        @SerializedName("identifier")
        public final String identifier;

        public ForgotPasswordRequest(String identifier) {
            this.identifier = identifier;
        }
    }

    public static class ResetPasswordRequest {
        @SerializedName("identifier")
        public final String identifier;
        @SerializedName("otp")
        public final String otp;
        @SerializedName("newPassword")
        public final String newPassword;

        public ResetPasswordRequest(String identifier, String otp, String newPassword) {
            this.identifier = identifier;
            this.otp = otp;
            this.newPassword = newPassword;
        }
    }

    public static class ChangePasswordRequest {
        @SerializedName("currentPassword")
        public final String currentPassword;
        @SerializedName("newPassword")
        public final String newPassword;

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
    }

    public static class UpdateProfileRequest {
        @SerializedName("fullName")
        public final String fullName;
        @SerializedName("mobileNumber")
        public final String mobileNumber;
        @SerializedName("profilePhotoUrl")
        public final String profilePhotoUrl;

        public UpdateProfileRequest(String fullName, String mobileNumber, String profilePhotoUrl) {
            this.fullName = fullName;
            this.mobileNumber = mobileNumber;
            this.profilePhotoUrl = profilePhotoUrl;
        }
    }

    public static class DeviceTokenRequest {
        @SerializedName("fcmToken")
        public final String fcmToken;
        @SerializedName("platform")
        public final String platform = "ANDROID";

        public DeviceTokenRequest(String fcmToken) {
            this.fcmToken = fcmToken;
        }
    }

    public static class AuthResponse {
        @SerializedName("accessToken")
        private String accessToken;

        @SerializedName("tokenType")
        private String tokenType;

        @SerializedName("expiresInSeconds")
        private long expiresInSeconds;

        @SerializedName("user")
        private User user;

        @SerializedName("otpRequired")
        private boolean otpRequired;

        @SerializedName("maskedDestination")
        private String maskedDestination;

        public String getAccessToken() { return accessToken; }
        public String getTokenType() { return tokenType; }
        public long getExpiresInSeconds() { return expiresInSeconds; }
        public User getUser() { return user; }
        public boolean isOtpRequired() { return otpRequired; }
        public String getMaskedDestination() { return maskedDestination; }
    }
}
