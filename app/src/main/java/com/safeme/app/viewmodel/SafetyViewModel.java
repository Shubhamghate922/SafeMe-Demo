package com.safeme.app.viewmodel;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.safeme.app.api.ApiResult;
import com.safeme.app.models.AppNotification;
import com.safeme.app.models.CommunityReport;
import com.safeme.app.models.IncidentReport;
import com.safeme.app.models.Journey;
import com.safeme.app.models.SosAlert;
import com.safeme.app.repository.JourneyRepository;
import com.safeme.app.repository.SafetyFeaturesRepository;
import com.safeme.app.repository.SosRepository;

import java.util.List;

public class SafetyViewModel extends AndroidViewModel {

    private final SafetyFeaturesRepository safetyRepo;
    private final JourneyRepository journeyRepo;
    private final SosRepository sosRepo;

    public SafetyViewModel(@NonNull Application application) {
        super(application);
        this.safetyRepo = new SafetyFeaturesRepository(application);
        this.journeyRepo = new JourneyRepository(application);
        this.sosRepo = new SosRepository(application);
    }

    public LiveData<ApiResult<List<CommunityReport>>> getCommunityReports() {
        return safetyRepo.getCommunityReports();
    }

    public LiveData<ApiResult<CommunityReport>> submitCommunityReport(CommunityReport report) {
        return safetyRepo.submitCommunityReport(report);
    }

    public LiveData<ApiResult<List<IncidentReport>>> getIncidentReports() {
        return safetyRepo.getIncidentReports();
    }

    public LiveData<ApiResult<IncidentReport>> submitIncidentReport(
            String category, String description, String date, String locationName,
            double lat, double lng, Uri photoUri, Uri audioUri) {
        return safetyRepo.submitIncidentReport(category, description, date, locationName, lat, lng, photoUri, audioUri);
    }

    public LiveData<ApiResult<List<Journey>>> getJourneyHistory() {
        return journeyRepo.getJourneyHistory();
    }

    public LiveData<ApiResult<List<SosAlert>>> getSosHistory() {
        return sosRepo.getSosHistory();
    }

    public LiveData<ApiResult<List<AppNotification>>> getNotifications() {
        return safetyRepo.getNotifications();
    }

    public void markNotificationRead(String id) {
        safetyRepo.markNotificationRead(id);
    }
}
