package com.safeme.app.api;

import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.AppNotification;
import com.safeme.app.models.AuthDtos;
import com.safeme.app.models.CommunityReport;
import com.safeme.app.models.IncidentReport;
import com.safeme.app.models.Journey;
import com.safeme.app.models.JourneyDtos;
import com.safeme.app.models.LocationUpdate;
import com.safeme.app.models.ResponseAcknowledgement;
import com.safeme.app.models.SafetyCheckIn;
import com.safeme.app.models.SosAlert;
import com.safeme.app.models.TrustedContact;
import com.safeme.app.models.User;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

/**
 * Retrofit REST API contract for the SafeMe Java + Spring Boot backend.
 */
public interface SafeMeApiService {

    // ==================== AUTHENTICATION ====================
    @POST("auth/register")
    Call<ApiResponse<AuthDtos.AuthResponse>> register(@Body AuthDtos.RegisterRequest request);

    @POST("auth/login")
    Call<ApiResponse<AuthDtos.AuthResponse>> login(@Body AuthDtos.LoginRequest request);

    @POST("auth/verify-otp")
    Call<ApiResponse<AuthDtos.AuthResponse>> verifyOtp(@Body AuthDtos.VerifyOtpRequest request);

    @POST("auth/resend-otp")
    Call<ApiResponse<Void>> resendOtp(@Body AuthDtos.ResendOtpRequest request);

    @POST("auth/forgot-password")
    Call<ApiResponse<AuthDtos.AuthResponse>> forgotPassword(@Body AuthDtos.ForgotPasswordRequest request);

    @POST("auth/reset-password")
    Call<ApiResponse<Void>> resetPassword(@Body AuthDtos.ResetPasswordRequest request);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();

    // ==================== USERS ====================
    @GET("users/me")
    Call<ApiResponse<User>> getCurrentUser();

    @PUT("users/me")
    Call<ApiResponse<User>> updateCurrentUser(@Body AuthDtos.UpdateProfileRequest request);

    @POST("users/me/change-password")
    Call<ApiResponse<Void>> changePassword(@Body AuthDtos.ChangePasswordRequest request);

    @DELETE("users/me")
    Call<ApiResponse<Void>> deleteAccount();

    @POST("users/me/device-token")
    Call<ApiResponse<Void>> registerDeviceToken(@Body AuthDtos.DeviceTokenRequest request);

    // ==================== TRUSTED CONTACTS ====================
    @POST("contacts")
    Call<ApiResponse<TrustedContact>> addContact(@Body TrustedContact contact);

    @GET("contacts")
    Call<ApiResponse<List<TrustedContact>>> getContacts();

    @PUT("contacts/{id}")
    Call<ApiResponse<TrustedContact>> updateContact(@Path("id") String contactId, @Body TrustedContact contact);

    @DELETE("contacts/{id}")
    Call<ApiResponse<Void>> deleteContact(@Path("id") String contactId);

    // ==================== JOURNEYS ====================
    @POST("journeys")
    Call<ApiResponse<Journey>> createJourney(@Body JourneyDtos.CreateJourneyRequest request);

    @GET("journeys")
    Call<ApiResponse<List<Journey>>> getJourneys();

    @GET("journeys/active")
    Call<ApiResponse<Journey>> getActiveJourney();

    @GET("journeys/{id}")
    Call<ApiResponse<Journey>> getJourneyById(@Path("id") String journeyId);

    @PUT("journeys/{id}")
    Call<ApiResponse<Journey>> updateJourney(@Path("id") String journeyId, @Body JourneyDtos.UpdateJourneyRequest request);

    @POST("journeys/{id}/start")
    Call<ApiResponse<Journey>> startJourney(@Path("id") String journeyId);

    @POST("journeys/{id}/complete")
    Call<ApiResponse<Journey>> completeJourney(@Path("id") String journeyId);

    @POST("journeys/{id}/extend")
    Call<ApiResponse<Journey>> extendJourney(@Path("id") String journeyId, @Body JourneyDtos.ExtendJourneyRequest request);

    @POST("journeys/{id}/locations")
    Call<ApiResponse<LocationUpdate>> sendJourneyLocation(@Path("id") String journeyId, @Body LocationUpdate update);

    @POST("journeys/{id}/escalate")
    Call<ApiResponse<JourneyDtos.EmergencyEscalation>> escalateJourney(@Path("id") String journeyId, @Body JourneyDtos.EscalationRequest request);

    // ==================== SAFETY CHECK-INS ====================
    @POST("journeys/{id}/checkins")
    Call<ApiResponse<SafetyCheckIn>> submitCheckIn(@Path("id") String journeyId, @Body SafetyCheckIn checkIn);

    @GET("journeys/{id}/checkins")
    Call<ApiResponse<List<SafetyCheckIn>>> getCheckIns(@Path("id") String journeyId);

    // ==================== SOS & LOCATION ====================
    @POST("sos")
    Call<ApiResponse<SosAlert>> triggerSos(@Body JourneyDtos.TriggerSosRequest request);

    @GET("sos/active")
    Call<ApiResponse<SosAlert>> getActiveSos();

    @GET("sos/history")
    Call<ApiResponse<List<SosAlert>>> getSosHistory();

    @POST("sos/{id}/resolve")
    Call<ApiResponse<SosAlert>> resolveSos(@Path("id") String sosId, @Body JourneyDtos.ResolveSosRequest request);

    @POST("sos/{id}/locations")
    Call<ApiResponse<LocationUpdate>> sendSosLocation(@Path("id") String sosId, @Body LocationUpdate update);

    @GET("sos/{id}/locations/latest")
    Call<ApiResponse<LocationUpdate>> getLatestSosLocation(@Path("id") String sosId);

    // ==================== EMERGENCY ACKNOWLEDGEMENTS ====================
    @POST("sos/{id}/acknowledgements")
    Call<ApiResponse<ResponseAcknowledgement>> submitAcknowledgement(@Path("id") String sosId, @Body ResponseAcknowledgement acknowledgement);

    @GET("sos/{id}/acknowledgements")
    Call<ApiResponse<List<ResponseAcknowledgement>>> getAcknowledgements(@Path("id") String sosId);

    // ==================== COMMUNITY SAFETY REPORTS ====================
    @POST("community/reports")
    Call<ApiResponse<CommunityReport>> submitCommunityReport(@Body CommunityReport report);

    @GET("community/reports")
    Call<ApiResponse<List<CommunityReport>>> getCommunityReports();

    // ==================== INCIDENT REPORTS ====================
    @Multipart
    @POST("incidents/reports")
    Call<ApiResponse<IncidentReport>> submitIncidentReport(
            @Part("category") RequestBody category,
            @Part("description") RequestBody description,
            @Part("incidentDate") RequestBody incidentDate,
            @Part("locationName") RequestBody locationName,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part MultipartBody.Part photo,
            @Part MultipartBody.Part audio
    );

    @GET("incidents/reports")
    Call<ApiResponse<List<IncidentReport>>> getIncidentReports();

    // ==================== NOTIFICATIONS ====================
    @GET("notifications")
    Call<ApiResponse<List<AppNotification>>> getNotifications();

    @PUT("notifications/{id}/read")
    Call<ApiResponse<AppNotification>> markNotificationRead(@Path("id") String notificationId);
}
