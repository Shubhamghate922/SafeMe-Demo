package com.safeme.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.safeme.app.api.ApiResult;
import com.safeme.app.models.Journey;
import com.safeme.app.models.JourneyDtos;
import com.safeme.app.models.SosAlert;
import com.safeme.app.models.TrustedContact;
import com.safeme.app.models.User;
import com.safeme.app.repository.AuthRepository;
import com.safeme.app.repository.ContactsRepository;
import com.safeme.app.repository.JourneyRepository;
import com.safeme.app.repository.SosRepository;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final JourneyRepository journeyRepository;
    private final ContactsRepository contactsRepository;
    private final SosRepository sosRepository;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.journeyRepository = new JourneyRepository(application);
        this.contactsRepository = new ContactsRepository(application);
        this.sosRepository = new SosRepository(application);
    }

    public LiveData<ApiResult<User>> loadUserProfile() {
        return authRepository.fetchCurrentUser();
    }

    public LiveData<ApiResult<Journey>> loadActiveJourney() {
        return journeyRepository.getActiveJourney();
    }

    public LiveData<ApiResult<List<TrustedContact>>> loadContacts() {
        return contactsRepository.getContacts();
    }

    public LiveData<ApiResult<SosAlert>> loadActiveSos() {
        return sosRepository.getActiveSos();
    }

    public LiveData<ApiResult<SosAlert>> triggerQuickSos(String journeyId, double lat, double lng, float accuracy) {
        return sosRepository.triggerSos(new JourneyDtos.TriggerSosRequest(
                journeyId, "MANUAL_SOS", lat, lng, accuracy, "Manual emergency SOS triggered from SafeMe."
        ));
    }
}
