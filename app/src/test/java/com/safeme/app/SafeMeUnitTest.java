package com.safeme.app;

import static org.junit.Assert.*;

import com.safeme.app.api.ApiErrorParser;
import com.safeme.app.utils.DateTimeUtils;
import com.safeme.app.utils.ValidationUtils;

import org.junit.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Pure JVM unit tests for SafeMe validation, time/freshness formatting,
 * and API error translation.
 */
public class SafeMeUnitTest {

    @Test
    public void validationUtils_validatesEmailMobileAndPassword() {
        assertTrue(ValidationUtils.isValidFullName("Ananya Sharma"));
        assertFalse(ValidationUtils.isValidFullName("A"));

        assertTrue(ValidationUtils.isValidEmail("ananya@example.com"));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));

        assertTrue(ValidationUtils.isValidMobile("+91 98765-43210"));
        assertEquals("+919876543210", ValidationUtils.normalizeMobile("+91 98765-43210"));
        assertFalse(ValidationUtils.isValidMobile("12345"));

        assertNotNull(ValidationUtils.getPasswordError("weak"));
        assertNotNull(ValidationUtils.getPasswordError("alllowercase1"));
        assertNotNull(ValidationUtils.getPasswordError("NoDigitsHere"));
        assertNull(ValidationUtils.getPasswordError("SafeMe@2026"));

        assertTrue(ValidationUtils.isValidOtp("482910"));
        assertFalse(ValidationUtils.isValidOtp("12ab56"));

        assertTrue(ValidationUtils.isValidServerUrl("http://10.0.2.2:8080/api/v1/"));
        assertFalse(ValidationUtils.isValidServerUrl("ftp://invalid"));
    }

    @Test
    public void dateTimeUtils_formatsCountdownAndFreshnessHonestly() {
        assertEquals("00:00", DateTimeUtils.formatCountdownSeconds(0));
        assertEquals("02:05", DateTimeUtils.formatCountdownSeconds(125));
        assertEquals("01:01:01", DateTimeUtils.formatCountdownSeconds(3661));

        assertEquals("No GPS fix recorded yet", DateTimeUtils.getLocationFreshnessText(0));
        assertTrue(DateTimeUtils.getLocationFreshnessText(System.currentTimeMillis() - 5_000L).startsWith("Live"));
        assertTrue(DateTimeUtils.getLocationFreshnessText(System.currentTimeMillis() - 300_000L).contains("Stale"));
        assertFalse(DateTimeUtils.isLocationLive(System.currentTimeMillis() - 300_000L));
    }

    @Test
    public void apiErrorParser_translatesNetworkExceptions() {
        assertTrue(ApiErrorParser.parseThrowable(new SocketTimeoutException()).contains("timed out"));
        assertTrue(ApiErrorParser.parseThrowable(new ConnectException()).contains("Unable to reach"));
    }
}
