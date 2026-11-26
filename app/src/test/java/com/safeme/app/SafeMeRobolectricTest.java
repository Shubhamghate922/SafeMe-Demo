package com.safeme.app;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.example.R;
import com.safeme.app.activities.LoginActivity;
import com.safeme.app.activities.MainActivity;
import com.safeme.app.activities.SplashActivity;
import com.safeme.app.services.CheckInTimerManager;
import com.safeme.app.utils.PermissionHelper;
import com.safeme.app.utils.SecureTokenManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

/**
 * Robolectric local JVM tests verifying SafeMe resources, permission checks,
 * session gating, timer deduplication, and authentication navigation without an emulator.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {36})
public class SafeMeRobolectricTest {

    @Test
    public void appNameMatchesSafeMeBranding() {
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals("SafeMe", context.getString(R.string.app_name));
        assertEquals("Your Safety, Our Priority", context.getString(R.string.app_tagline));
    }

    @Test
    public void permissionHelperDoesNotFalselyReportLocationGranted() {
        Context context = ApplicationProvider.getApplicationContext();
        assertFalse(PermissionHelper.hasLocationPermission(context));
        assertFalse(PermissionHelper.hasMicrophonePermission(context));
    }

    @Test
    public void mainActivityRedirectsToLoginWhenUnauthenticated() {
        Context context = ApplicationProvider.getApplicationContext();
        assertFalse(SafeMeApplication.AUTH_DISABLED_FOR_UI_PREVIEW);
        SecureTokenManager.getInstance(context).clearSession();

        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
        Intent nextIntent = Shadows.shadowOf(activity).getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(LoginActivity.class.getName(), nextIntent.getComponent().getClassName());
        assertTrue(activity.isFinishing());
    }

    @Test
    public void secureTokenManagerPersistsConfigurableApiUrlAndPreferences() {
        Context context = ApplicationProvider.getApplicationContext();
        SecureTokenManager tm = SecureTokenManager.getInstance(context);
        tm.setApiBaseUrl("http://192.168.1.50:8080/api/v1");
        assertEquals("http://192.168.1.50:8080/api/v1/", tm.getApiBaseUrl());

        tm.setDefaultCheckInIntervalMinutes(10);
        assertEquals(10, tm.getDefaultCheckInIntervalMinutes());
    }

    @Test
    public void checkInTimerManagerPreventsDuplicateCheckInsAndEscalations() {
        CheckInTimerManager manager = CheckInTimerManager.getInstance();
        manager.stopTimer();

        manager.scheduleNextCheckIn(null, "journey_123", System.currentTimeMillis() + 600_000L);
        assertTrue(manager.tryLockCheckInSubmission());
        assertFalse(manager.tryLockCheckInSubmission());

        assertTrue(manager.tryLockEscalationStage(1));
        assertFalse(manager.tryLockEscalationStage(1));
        assertTrue(manager.tryLockEscalationStage(2));
        assertFalse(manager.tryLockEscalationStage(2));

        manager.stopTimer();
    }

    @Test
    public void splashActivityCreatesWithoutCrashing() {
        SplashActivity activity = Robolectric.buildActivity(SplashActivity.class).create().start().resume().get();
        assertNotNull(activity);
    }

    @Test
    public void localBackendRegisterLoginAndAuthenticatedEndpointsWorkEndToEnd() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SecureTokenManager tm = SecureTokenManager.getInstance(context);
        tm.clearSession();
        tm.setLocalBackendMode(true);
        com.safeme.app.api.RetrofitClient.resetClient();

        com.safeme.app.api.SafeMeApiService api = com.safeme.app.api.RetrofitClient.getApiService(context);

        retrofit2.Response<com.safeme.app.models.ApiResponse<com.safeme.app.models.AuthDtos.AuthResponse>> regResp =
                api.register(new com.safeme.app.models.AuthDtos.RegisterRequest(
                        "Anita Rao", "anita.rao@example.com", "9876543210", "SafePass123"))
                        .execute();
        assertTrue(regResp.isSuccessful());
        assertNotNull(regResp.body());
        assertTrue(regResp.body().isSuccess());
        assertNotNull(regResp.body().getData().getAccessToken());

        retrofit2.Response<com.safeme.app.models.ApiResponse<com.safeme.app.models.AuthDtos.AuthResponse>> badLogin =
                api.login(new com.safeme.app.models.AuthDtos.LoginRequest("anita.rao@example.com", "WrongPass9"))
                        .execute();
        assertEquals(401, badLogin.code());

        retrofit2.Response<com.safeme.app.models.ApiResponse<com.safeme.app.models.AuthDtos.AuthResponse>> goodLogin =
                api.login(new com.safeme.app.models.AuthDtos.LoginRequest("anita.rao@example.com", "SafePass123"))
                        .execute();
        assertTrue(goodLogin.isSuccessful());
        assertNotNull(goodLogin.body());
        String token = goodLogin.body().getData().getAccessToken();
        assertNotNull(token);
        tm.saveToken(token);

        retrofit2.Response<com.safeme.app.models.ApiResponse<com.safeme.app.models.User>> meResp =
                api.getCurrentUser().execute();
        assertTrue(meResp.isSuccessful());
        assertNotNull(meResp.body());
        assertEquals("Anita Rao", meResp.body().getData().getFullName());

        retrofit2.Response<com.safeme.app.models.ApiResponse<com.safeme.app.models.TrustedContact>> addContactResp =
                api.addContact(new com.safeme.app.models.TrustedContact(
                        "Meera Rao", "9123456789", "Sister", "EXTERNAL_INVITE", 1, true, true, true))
                        .execute();
        assertTrue(addContactResp.isSuccessful());

        retrofit2.Response<com.safeme.app.models.ApiResponse<java.util.List<com.safeme.app.models.TrustedContact>>> listResp =
                api.getContacts().execute();
        assertTrue(listResp.isSuccessful());
        assertNotNull(listResp.body());
        assertEquals(1, listResp.body().getData().size());
        assertEquals("Meera Rao", listResp.body().getData().get(0).getName());
    }
}
