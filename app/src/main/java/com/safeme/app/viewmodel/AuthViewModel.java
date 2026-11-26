package com.safeme.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.safeme.app.api.ApiResult;
import com.safeme.app.models.AuthDtos;
import com.safeme.app.models.User;
import com.safeme.app.repository.AuthRepository;

public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> register(String fullName, String email, String mobile, String password) {
        return authRepository.register(fullName, email, mobile, password);
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> login(String identifier, String password, boolean rememberLogin) {
        return authRepository.login(identifier, password, rememberLogin);
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> verifyOtp(String mobile, String email, String otp, String purpose) {
        return authRepository.verifyOtp(mobile, email, otp, purpose);
    }

    public LiveData<ApiResult<Void>> resendOtp(String mobile, String purpose) {
        return authRepository.resendOtp(mobile, purpose);
    }

    public LiveData<ApiResult<AuthDtos.AuthResponse>> forgotPassword(String identifier) {
        return authRepository.forgotPassword(identifier);
    }

    public LiveData<ApiResult<Void>> resetPassword(String identifier, String otp, String newPassword) {
        return authRepository.resetPassword(identifier, otp, newPassword);
    }

    public LiveData<ApiResult<User>> validateCurrentSession() {
        return authRepository.fetchCurrentUser();
    }
}
