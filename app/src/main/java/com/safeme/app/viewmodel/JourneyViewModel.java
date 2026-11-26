package com.safeme.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.safeme.app.api.ApiResult;
import com.safeme.app.models.Journey;
import com.safeme.app.models.JourneyDtos;
import com.safeme.app.models.SafetyCheckIn;
import com.safeme.app.models.TrustedContact;
import com.safeme.app.repository.ContactsRepository;
import com.safeme.app.repository.JourneyRepository;

import java.util.List;

public class JourneyViewModel extends AndroidViewModel {

    private final JourneyRepository journeyRepository;
    private final ContactsRepository contactsRepository;

    public JourneyViewModel(@NonNull Application application) {
        super(application);
        this.journeyRepository = new JourneyRepository(application);
        this.contactsRepository = new ContactsRepository(application);
    }

    public LiveData<ApiResult<List<TrustedContact>>> loadTrustedContacts() {
        return contactsRepository.getContacts();
    }

    public LiveData<ApiResult<Journey>> createAndStartJourney(JourneyDtos.CreateJourneyRequest request) {
        return journeyRepository.createAndStartJourney(request);
    }

    public LiveData<ApiResult<Journey>> getActiveJourney() {
        return journeyRepository.getActiveJourney();
    }

    public LiveData<ApiResult<Journey>> completeJourney(String journeyId) {
        return journeyRepository.completeJourney(journeyId);
    }

    public LiveData<ApiResult<Journey>> extendJourney(String journeyId, int minutes) {
        return journeyRepository.extendJourney(journeyId, minutes);
    }

    public LiveData<ApiResult<Journey>> changeDestination(String journeyId, String destName, double lat, double lng) {
        return journeyRepository.updateDestination(journeyId, destName, lat, lng);
    }

    public LiveData<ApiResult<SafetyCheckIn>> submitCheckIn(String journeyId, String status, double lat, double lng, String note) {
        return journeyRepository.submitCheckIn(journeyId, new SafetyCheckIn(journeyId, status, lat, lng, note));
    }

    public LiveData<ApiResult<JourneyDtos.EmergencyEscalation>> escalateJourney(String journeyId, int stage, String reason) {
        return journeyRepository.escalateJourney(journeyId, stage, reason);
    }
}
