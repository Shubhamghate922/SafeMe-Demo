package com.safeme.app.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.messaging.FirebaseMessaging;
import com.safeme.app.api.ApiErrorParser;
import com.safeme.app.api.ApiResult;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.api.SafeMeApiService;
import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.AuthDtos;
import com.safeme.app.models.User;
import com.safeme.app.utils.NetworkUtils;
import com.safeme.app.utils.SecureTokenManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles user registration, login, OTP verification, password reset, profile updates, and logout.
 */
public class AuthRepository {

    private final Context appContext;
    private final SecureTokenManager tokenManager;

    public AuthRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.tokenManager = SecureTokenManager.getInstance(appContext);
    }

    private SafeMeApiService api() {
        return RetrofitClient.getApiService(appContext);
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> register(String fullName, String email, String mobile, String password) {
        MutableLiveData<ApiResult<AuthDtos.AuthResponse>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        if (!tokenManager.isLocalBackendMode() && !NetworkUtils.isInternetAvailable(appContext)) {
            liveData.setValue(ApiResult.error("No internet connection. Registration requires backend connectivity."));
            return liveData;
        }

        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest(fullName, email, mobile, password);
        api().register(request).enqueue(new Callback<ApiResponse<AuthDtos.AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call,
                                   @NonNull Response<ApiResponse<AuthDtos.AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthDtos.AuthResponse data = response.body().getData();
                    if (data != null && data.getAccessToken() != null) {
                        persistAuth(data);
                    }
                    liveData.postValue(ApiResult.success(data, response.body().getMessage()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> login(String identifier, String password, boolean rememberLogin) {
        MutableLiveData<ApiResult<AuthDtos.AuthResponse>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        if (!tokenManager.isLocalBackendMode() && !NetworkUtils.isInternetAvailable(appContext)) {
            liveData.setValue(ApiResult.error("No internet connection. Please connect and retry."));
            return liveData;
        }

        tokenManager.setRememberLogin(rememberLogin);
        api().login(new AuthDtos.LoginRequest(identifier, password)).enqueue(new Callback<ApiResponse<AuthDtos.AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call,
                                   @NonNull Response<ApiResponse<AuthDtos.AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthDtos.AuthResponse data = response.body().getData();
                    if (data != null && data.getAccessToken() != null) {
                        persistAuth(data);
                        syncFcmToken();
                    }
                    liveData.postValue(ApiResult.success(data, response.body().getMessage()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> verifyOtp(String mobile, String email, String otp, String purpose) {
        MutableLiveData<ApiResult<AuthDtos.AuthResponse>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        if (!tokenManager.isLocalBackendMode() && !NetworkUtils.isInternetAvailable(appContext)) {
            liveData.setValue(ApiResult.error("No internet connection. Unable to verify OTP."));
            return liveData;
        }

        api().verifyOtp(new AuthDtos.VerifyOtpRequest(mobile, email, otp, purpose)).enqueue(new Callback<ApiResponse<AuthDtos.AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call,
                                   @NonNull Response<ApiResponse<AuthDtos.AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthDtos.AuthResponse data = response.body().getData();
                    if (data != null && data.getAccessToken() != null) {
                        persistAuth(data);
                        syncFcmToken();
                    }
                    liveData.postValue(ApiResult.success(data, response.body().getMessage()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Void>> resendOtp(String mobile, String purpose) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api().resendOtp(new AuthDtos.ResendOtpRequest(mobile, purpose)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(null, response.body().getMessage()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> forgotPassword(String identifier) {
        MutableLiveData<ApiResult<AuthDtos.AuthResponse>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api().forgotPassword(new AuthDtos.ForgotPasswordRequest(identifier)).enqueue(new Callback<ApiResponse<AuthDtos.AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call,
                                   @NonNull Response<ApiResponse<AuthDtos.AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), response.body().getMessage()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AuthDtos.AuthResponse>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Void>> resetPassword(String identifier, String otp, String newPassword) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api().resetPassword(new AuthDtos.ResetPasswordRequest(identifier, otp, newPassword)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(null, response.body().getMessage()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<User>> fetchCurrentUser() {
        MutableLiveData<ApiResult<User>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api().getCurrentUser().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call, @NonNull Response<ApiResponse<User>> response) {
                if (response.code() == 401) {
                    tokenManager.clearSession();
                    liveData.postValue(ApiResult.unauthorized("Session expired. Please login again."));
                } else if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null) {
                        tokenManager.saveUserSession(user.getId(), user.getFullName(),
                                user.getEmail(), user.getMobileNumber(), user.isVerified());
                    }
                    liveData.postValue(ApiResult.success(user));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<User>> updateProfile(String fullName, String mobileNumber, String photoUrl) {
        MutableLiveData<ApiResult<User>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api().updateCurrentUser(new AuthDtos.UpdateProfileRequest(fullName, mobileNumber, photoUrl)).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call, @NonNull Response<ApiResponse<User>> response) {
                if (response.code() == 401) {
                    liveData.postValue(ApiResult.unauthorized("Session expired."));
                } else if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    User user = response.body().getData();
                    if (user != null) {
                        tokenManager.saveUserSession(user.getId(), user.getFullName(),
                                user.getEmail(), user.getMobileNumber(), user.isVerified());
                    }
                    liveData.postValue(ApiResult.success(user, "Profile updated successfully."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Void>> changePassword(String currentPassword, String newPassword) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api().changePassword(new AuthDtos.ChangePasswordRequest(currentPassword, newPassword)).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(null, "Password changed successfully."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Void>> deleteAccount() {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api().deleteAccount().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    tokenManager.clearSession();
                    liveData.postValue(ApiResult.success(null, "Account deleted."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public void logout(Runnable onComplete) {
        api().logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                tokenManager.clearSession();
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                tokenManager.clearSession();
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void persistAuth(AuthDtos.AuthResponse data) {
        tokenManager.saveToken(data.getAccessToken());
        if (data.getUser() != null) {
            User u = data.getUser();
            tokenManager.saveUserSession(u.getId(), u.getFullName(), u.getEmail(), u.getMobileNumber(), u.isVerified());
        }
    }

    public void syncFcmToken() {
        try {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(fcmToken -> {
                if (fcmToken != null && !fcmToken.isEmpty()) {
                    tokenManager.saveFcmToken(fcmToken);
                    api().registerDeviceToken(new AuthDtos.DeviceTokenRequest(fcmToken))
                            .enqueue(new Callback<ApiResponse<Void>>() {
                                @Override
                                public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {}

                                @Override
                                public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {}
                            });
                }
            });
        } catch (Exception ignored) {
            // Firebase may not be initialized in unit test environment
        }
    }
}
