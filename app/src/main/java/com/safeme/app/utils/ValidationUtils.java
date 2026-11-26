package com.safeme.app.utils;

import java.util.regex.Pattern;

/**
 * Input validation rules for authentication, registration, contacts, journeys, and configuration.
 * Avoids Android framework static dependencies where possible so unit tests run cleanly.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern MOBILE_PATTERN = Pattern.compile(
            "^[+]?[0-9]{10,13}$"
    );

    private static final Pattern PASSWORD_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASSWORD_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern PASSWORD_DIGIT = Pattern.compile(".*[0-9].*");
    private static final Pattern OTP_PATTERN = Pattern.compile("^[0-9]{6}$");

    private ValidationUtils() {}

    public static boolean isValidFullName(String name) {
        return name != null && name.trim().length() >= 2 && name.trim().length() <= 80;
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidMobile(String mobile) {
        if (mobile == null) return false;
        String cleaned = mobile.replaceAll("[\\s\\-()]", "");
        return MOBILE_PATTERN.matcher(cleaned).matches();
    }

    public static String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        return mobile.replaceAll("[\\s\\-()]", "");
    }

    public static boolean isValidEmailOrMobile(String input) {
        return isValidEmail(input) || isValidMobile(input);
    }

    public static String getPasswordError(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (!PASSWORD_UPPER.matcher(password).matches()) {
            return "Password must include at least one uppercase letter.";
        }
        if (!PASSWORD_LOWER.matcher(password).matches()) {
            return "Password must include at least one lowercase letter.";
        }
        if (!PASSWORD_DIGIT.matcher(password).matches()) {
            return "Password must include at least one number.";
        }
        return null;
    }

    public static boolean isValidOtp(String otp) {
        return otp != null && OTP_PATTERN.matcher(otp.trim()).matches();
    }

    public static boolean isValidServerUrl(String url) {
        if (url == null) return false;
        String trimmed = url.trim().toLowerCase();
        return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) && trimmed.length() > 10;
    }
}
