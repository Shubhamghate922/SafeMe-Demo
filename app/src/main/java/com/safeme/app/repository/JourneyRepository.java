package com.safeme.app.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.safeme.app.api.ApiErrorParser;
import com.safeme.app.api.ApiResult;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.api.SafeMeApiService;
import com.safeme.app.database.CachedJourneyEntity;
import com.safeme.app.database.SafeMeDatabase;
import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.Journey;
import com.safeme.app.models.JourneyDtos;
import com.safeme.app.models.SafetyCheckIn;
import com.safeme.app.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles Smart Journey Monitoring lifecycle, scheduled safety check-ins, and escalation events.
 */
public class JourneyRepository {

    private final Context appContext;
    private final SafeMeApiService api;
    private final SafeMeDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public JourneyRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.api = RetrofitClient.getApiService(appContext);
        this.database = SafeMeDatabase.getInstance(appContext);
    }

    public LiveData<ApiResult<Journey>> createAndStartJourney(JourneyDtos.CreateJourneyRequest request) {
        MutableLiveData<ApiResult<Journey>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        if (!com.safeme.app.utils.SecureTokenManager.getInstance(appContext).isLocalBackendMode()
                && !NetworkUtils.isInternetAvailable(appContext)) {
            liveData.setValue(ApiResult.error("Internet connection is required to start monitored journey sharing."));
            return liveData;
        }

        api.createJourney(request).enqueue(new Callback<ApiResponse<Journey>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Journey>> call,
                                   @NonNull Response<ApiResponse<Journey>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Journey created = response.body().getData();
                    if (created != null && created.getId() != null && !"ACTIVE".equals(created.getStatus())) {
                        startCreatedJourney(created.getId(), liveData);
                    } else {
                        if (created != null) cacheJourney(created);
                        liveData.postValue(ApiResult.success(created, "Journey monitoring started."));
                    }
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Journey>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    private void startCreatedJourney(String journeyId, MutableLiveData<ApiResult<Journey>> liveData) {
        api.startJourney(journeyId).enqueue(new Callback<ApiResponse<Journey>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Journey>> call,
                                   @NonNull Response<ApiResponse<Journey>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Journey started = response.body().getData();
                    if (started != null) cacheJourney(started);
                    liveData.postValue(ApiResult.success(started, "Journey monitoring started."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Journey>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
    }

    public LiveData<ApiResult<Journey>> getActiveJourney() {
        MutableLiveData<ApiResult<Journey>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        if (!com.safeme.app.utils.SecureTokenManager.getInstance(appContext).isLocalBackendMode()
                && !NetworkUtils.isInternetAvailable(appContext)) {
            loadCachedActiveJourney(liveData, "Offline: displaying cached journey state.");
            return liveData;
        }

        api.getActiveJourney().enqueue(new Callback<ApiResponse<Journey>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Journey>> call,
                                   @NonNull Response<ApiResponse<Journey>> response) {
                if (response.code() == 401) {
                    liveData.postValue(ApiResult.unauthorized("Session expired."));
                } else if (response.code() == 404) {
                    liveData.postValue(ApiResult.success(null));
                } else if (response.isSuccessful() && response.body() != null) {
                    Journey active = response.body().getData();
                    if (active != null) cacheJourney(active);
                    liveData.postValue(ApiResult.success(active));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Journey>> call, @NonNull Throwable t) {
                loadCachedActiveJourney(liveData, ApiErrorParser.parseThrowable(t));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<List<Journey>>> getJourneyHistory() {
        MutableLiveData<ApiResult<List<Journey>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getJourneys().enqueue(new Callback<ApiResponse<List<Journey>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Journey>>> call,
                                   @NonNull Response<ApiResponse<List<Journey>>> response) {
                if (response.code() == 401) {
                    liveData.postValue(ApiResult.unauthorized("Session expired."));
                } else if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Journey> list = response.body().getData();
                    liveData.postValue(ApiResult.success(list != null ? list : new ArrayList<>()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Journey>>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Journey>> completeJourney(String journeyId) {
        MutableLiveData<ApiResult<Journey>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.completeJourney(journeyId).enqueue(new Callback<ApiResponse<Journey>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Journey>> call, @NonNull Response<ApiResponse<Journey>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "Journey completed safely."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Journey>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Journey>> extendJourney(String journeyId, int extraMinutes) {
        MutableLiveData<ApiResult<Journey>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.extendJourney(journeyId, new JourneyDtos.ExtendJourneyRequest(extraMinutes)).enqueue(new Callback<ApiResponse<Journey>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Journey>> call, @NonNull Response<ApiResponse<Journey>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "Journey extended by " + extraMinutes + " minutes."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Journey>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Journey>> updateDestination(String journeyId, String destName, double lat, double lng) {
        MutableLiveData<ApiResult<Journey>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.updateJourney(journeyId, new JourneyDtos.UpdateJourneyRequest(destName, lat, lng))
                .enqueue(new Callback<ApiResponse<Journey>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Journey>> call, @NonNull Response<ApiResponse<Journey>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            liveData.postValue(ApiResult.success(response.body().getData(), "Destination updated."));
                        } else {
                            liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Journey>> call, @NonNull Throwable t) {
                        liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
                    }
                });
        return liveData;
    }

    public LiveData<ApiResult<SafetyCheckIn>> submitCheckIn(String journeyId, SafetyCheckIn checkIn) {
        MutableLiveData<ApiResult<SafetyCheckIn>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.submitCheckIn(journeyId, checkIn).enqueue(new Callback<ApiResponse<SafetyCheckIn>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SafetyCheckIn>> call, @NonNull Response<ApiResponse<SafetyCheckIn>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "Check-in confirmed."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SafetyCheckIn>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<JourneyDtos.EmergencyEscalation>> escalateJourney(String journeyId, int stage, String reason) {
        MutableLiveData<ApiResult<JourneyDtos.EmergencyEscalation>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.escalateJourney(journeyId, new JourneyDtos.EscalationRequest(stage, reason))
                .enqueue(new Callback<ApiResponse<JourneyDtos.EmergencyEscalation>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<JourneyDtos.EmergencyEscalation>> call,
                                           @NonNull Response<ApiResponse<JourneyDtos.EmergencyEscalation>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            liveData.postValue(ApiResult.success(response.body().getData(), "Escalation Stage " + stage + " triggered."));
                        } else {
                            liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<JourneyDtos.EmergencyEscalation>> call, @NonNull Throwable t) {
                        liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
                    }
                });
        return liveData;
    }

    private void cacheJourney(Journey j) {
        if (j.getId() == null) return;
        executor.execute(() -> {
            CachedJourneyEntity entity = new CachedJourneyEntity(
                    j.getId(), j.getDestinationName(), j.getStartLocationName(), j.getTransportMode(),
                    j.getStatus(), j.getExpectedArrivalTime(), j.getNextCheckInDueAt(),
                    j.getLastLatitude(), j.getLastLongitude(), j.getLastLocationTimestamp(),
                    System.currentTimeMillis()
            );
            database.safeMeDao().upsertJourneys(Collections.singletonList(entity));
        });
    }

    private void loadCachedActiveJourney(MutableLiveData<ApiResult<Journey>> liveData, String msg) {
        executor.execute(() -> {
            CachedJourneyEntity e = database.safeMeDao().getCachedActiveJourney();
            if (e != null) {
                Journey j = new Journey();
                j.setId(e.id);
                j.setDestinationName(e.destinationName);
                j.setStartLocationName(e.startLocationName);
                j.setTransportMode(e.transportMode);
                j.setStatus(e.status);
                j.setExpectedArrivalTime(e.expectedArrivalTime);
                j.setNextCheckInDueAt(e.nextCheckInDueAt);
                j.setLastLatitude(e.lastLatitude);
                j.setLastLongitude(e.lastLongitude);
                j.setLastLocationTimestamp(e.lastLocationTimestamp);
                liveData.postValue(ApiResult.cached(j, msg));
            } else {
                liveData.postValue(ApiResult.error(msg));
            }
        });
    }
}
