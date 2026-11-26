package com.safeme.app.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.safeme.app.api.ApiErrorParser;
import com.safeme.app.api.ApiResult;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.api.SafeMeApiService;
import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.JourneyDtos;
import com.safeme.app.models.LocationUpdate;
import com.safeme.app.models.ResponseAcknowledgement;
import com.safeme.app.models.SosAlert;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles SOS emergency alert dispatch, live GPS updates, resolution, and responder acknowledgements.
 */
public class SosRepository {

    private final SafeMeApiService api;

    public SosRepository(Context context) {
        this.api = RetrofitClient.getApiService(context.getApplicationContext());
    }

    public LiveData<ApiResult<SosAlert>> triggerSos(JourneyDtos.TriggerSosRequest request) {
        MutableLiveData<ApiResult<SosAlert>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.triggerSos(request).enqueue(new Callback<ApiResponse<SosAlert>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SosAlert>> call, @NonNull Response<ApiResponse<SosAlert>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "SOS emergency dispatched."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SosAlert>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<SosAlert>> getActiveSos() {
        MutableLiveData<ApiResult<SosAlert>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getActiveSos().enqueue(new Callback<ApiResponse<SosAlert>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SosAlert>> call, @NonNull Response<ApiResponse<SosAlert>> response) {
                if (response.code() == 401) {
                    liveData.postValue(ApiResult.unauthorized("Session expired."));
                } else if (response.code() == 404) {
                    liveData.postValue(ApiResult.success(null));
                } else if (response.isSuccessful() && response.body() != null) {
                    liveData.postValue(ApiResult.success(response.body().getData()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SosAlert>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<List<SosAlert>>> getSosHistory() {
        MutableLiveData<ApiResult<List<SosAlert>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getSosHistory().enqueue(new Callback<ApiResponse<List<SosAlert>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<SosAlert>>> call,
                                   @NonNull Response<ApiResponse<List<SosAlert>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<SosAlert> list = response.body().getData();
                    liveData.postValue(ApiResult.success(list != null ? list : new ArrayList<>()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<SosAlert>>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<SosAlert>> resolveSos(String sosId, String resolutionNote) {
        MutableLiveData<ApiResult<SosAlert>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.resolveSos(sosId, new JourneyDtos.ResolveSosRequest(resolutionNote)).enqueue(new Callback<ApiResponse<SosAlert>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SosAlert>> call, @NonNull Response<ApiResponse<SosAlert>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "Emergency resolved safely."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SosAlert>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<List<ResponseAcknowledgement>>> getAcknowledgements(String sosId) {
        MutableLiveData<ApiResult<List<ResponseAcknowledgement>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getAcknowledgements(sosId).enqueue(new Callback<ApiResponse<List<ResponseAcknowledgement>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<ResponseAcknowledgement>>> call,
                                   @NonNull Response<ApiResponse<List<ResponseAcknowledgement>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<ResponseAcknowledgement> list = response.body().getData();
                    liveData.postValue(ApiResult.success(list != null ? list : new ArrayList<>()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<ResponseAcknowledgement>>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<ResponseAcknowledgement>> submitAcknowledgement(String sosId, String status, String note) {
        MutableLiveData<ApiResult<ResponseAcknowledgement>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.submitAcknowledgement(sosId, new ResponseAcknowledgement(status, note))
                .enqueue(new Callback<ApiResponse<ResponseAcknowledgement>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<ResponseAcknowledgement>> call,
                                           @NonNull Response<ApiResponse<ResponseAcknowledgement>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            liveData.postValue(ApiResult.success(response.body().getData(), "Response recorded."));
                        } else {
                            liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<ResponseAcknowledgement>> call, @NonNull Throwable t) {
                        liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
                    }
                });
        return liveData;
    }

    public LiveData<ApiResult<LocationUpdate>> getLatestSosLocation(String sosId) {
        MutableLiveData<ApiResult<LocationUpdate>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getLatestSosLocation(sosId).enqueue(new Callback<ApiResponse<LocationUpdate>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LocationUpdate>> call,
                                   @NonNull Response<ApiResponse<LocationUpdate>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LocationUpdate>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }
}
