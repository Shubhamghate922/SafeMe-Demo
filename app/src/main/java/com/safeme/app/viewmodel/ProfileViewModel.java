package com.safeme.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.safeme.app.api.ApiResult;
import com.safeme.app.models.User;
import com.safeme.app.repository.AuthRepository;

public class ProfileViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public LiveData<ApiResult<User>> getCurrentUser() {
        return authRepository.fetchCurrentUser();
    }

    public LiveData<ApiResult<User>> updateProfile(String fullName, String mobile, String photoUrl) {
        return authRepository.updateProfile(fullName, mobile, photoUrl);
    }

    public LiveData<ApiResult<Void>> changePassword(String currentPass, String newPass) {
        return authRepository.changePassword(currentPass, newPass);
    }

    public LiveData<ApiResult<Void>> deleteAccount() {
        return authRepository.deleteAccount();
    }

    public void logout(Runnable onComplete) {
        authRepository.logout(onComplete);
    }
}
