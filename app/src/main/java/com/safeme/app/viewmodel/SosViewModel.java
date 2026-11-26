package com.safeme.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.safeme.app.api.ApiResult;
import com.safeme.app.models.JourneyDtos;
import com.safeme.app.models.LocationUpdate;
import com.safeme.app.models.ResponseAcknowledgement;
import com.safeme.app.models.SosAlert;
import com.safeme.app.repository.SosRepository;

import java.util.List;

public class SosViewModel extends AndroidViewModel {

    private final SosRepository sosRepository;

    public SosViewModel(@NonNull Application application) {
        super(application);
        this.sosRepository = new SosRepository(application);
    }

    public LiveData<ApiResult<SosAlert>> triggerSos(String journeyId, String source, double lat, double lng, float accuracy, String note) {
        return sosRepository.triggerSos(new JourneyDtos.TriggerSosRequest(journeyId, source, lat, lng, accuracy, note));
    }

    public LiveData<ApiResult<SosAlert>> getActiveSos() {
        return sosRepository.getActiveSos();
    }

    public LiveData<ApiResult<SosAlert>> resolveSos(String sosId, String note) {
        return sosRepository.resolveSos(sosId, note);
    }

    public LiveData<ApiResult<List<ResponseAcknowledgement>>> getAcknowledgements(String sosId) {
        return sosRepository.getAcknowledgements(sosId);
    }

    public LiveData<ApiResult<ResponseAcknowledgement>> submitAcknowledgement(String sosId, String status, String note) {
        return sosRepository.submitAcknowledgement(sosId, status, note);
    }

    public LiveData<ApiResult<LocationUpdate>> getLatestSosLocation(String sosId) {
        return sosRepository.getLatestSosLocation(sosId);
    }
}
