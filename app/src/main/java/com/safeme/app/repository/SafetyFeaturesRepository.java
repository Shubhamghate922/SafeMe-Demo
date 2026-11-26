package com.safeme.app.repository;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.safeme.app.api.ApiErrorParser;
import com.safeme.app.api.ApiResult;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.api.SafeMeApiService;
import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.AppNotification;
import com.safeme.app.models.CommunityReport;
import com.safeme.app.models.IncidentReport;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles Community Safety Map reports, Incident Reporting with secure multipart uploads, and Notifications.
 */
public class SafetyFeaturesRepository {

    private final Context appContext;
    private final SafeMeApiService api;

    public SafetyFeaturesRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.api = RetrofitClient.getApiService(appContext);
    }

    public LiveData<ApiResult<List<CommunityReport>>> getCommunityReports() {
        MutableLiveData<ApiResult<List<CommunityReport>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getCommunityReports().enqueue(new Callback<ApiResponse<List<CommunityReport>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<CommunityReport>>> call,
                                   @NonNull Response<ApiResponse<List<CommunityReport>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<CommunityReport> reports = response.body().getData();
                    liveData.postValue(ApiResult.success(reports != null ? reports : new ArrayList<>()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<CommunityReport>>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<CommunityReport>> submitCommunityReport(CommunityReport report) {
        MutableLiveData<ApiResult<CommunityReport>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.submitCommunityReport(report).enqueue(new Callback<ApiResponse<CommunityReport>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<CommunityReport>> call,
                                   @NonNull Response<ApiResponse<CommunityReport>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "Safety report submitted for moderation."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<CommunityReport>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<List<IncidentReport>>> getIncidentReports() {
        MutableLiveData<ApiResult<List<IncidentReport>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getIncidentReports().enqueue(new Callback<ApiResponse<List<IncidentReport>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<IncidentReport>>> call,
                                   @NonNull Response<ApiResponse<List<IncidentReport>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<IncidentReport> reports = response.body().getData();
                    liveData.postValue(ApiResult.success(reports != null ? reports : new ArrayList<>()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<IncidentReport>>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<IncidentReport>> submitIncidentReport(
            String category, String description, String date, String locationName,
            double latitude, double longitude, Uri photoUri, Uri audioUri) {

        MutableLiveData<ApiResult<IncidentReport>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        MediaType textType = MediaType.parse("text/plain");
        RequestBody rbCategory = RequestBody.create(textType, category);
        RequestBody rbDesc = RequestBody.create(textType, description);
        RequestBody rbDate = RequestBody.create(textType, date);
        RequestBody rbLoc = RequestBody.create(textType, locationName);
        RequestBody rbLat = RequestBody.create(textType, String.valueOf(latitude));
        RequestBody rbLng = RequestBody.create(textType, String.valueOf(longitude));

        MultipartBody.Part photoPart = createFilePart("photo", photoUri, "image/jpeg");
        MultipartBody.Part audioPart = createFilePart("audio", audioUri, "audio/mpeg");

        api.submitIncidentReport(rbCategory, rbDesc, rbDate, rbLoc, rbLat, rbLng, photoPart, audioPart)
                .enqueue(new Callback<ApiResponse<IncidentReport>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<IncidentReport>> call,
                                           @NonNull Response<ApiResponse<IncidentReport>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            liveData.postValue(ApiResult.success(response.body().getData(), "Incident report submitted securely."));
                        } else {
                            liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<IncidentReport>> call, @NonNull Throwable t) {
                        liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
                    }
                });
        return liveData;
    }

    private MultipartBody.Part createFilePart(String partName, Uri uri, String defaultMime) {
        if (uri == null) return null;
        try {
            ContentResolver resolver = appContext.getContentResolver();
            String mimeType = resolver.getType(uri);
            if (mimeType == null) mimeType = defaultMime;
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (ext == null) ext = "bin";

            InputStream is = resolver.openInputStream(uri);
            if (is == null) return null;
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            int totalBytes = 0;
            final int maxBytes = 10 * 1024 * 1024; // 10MB upload limit
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                totalBytes += nRead;
                if (totalBytes > maxBytes) {
                    is.close();
                    return null;
                }
                buffer.write(data, 0, nRead);
            }
            is.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), buffer.toByteArray());
            return MultipartBody.Part.createFormData(partName, partName + "_" + System.currentTimeMillis() + "." + ext, requestFile);
        } catch (Exception e) {
            return null;
        }
    }

    public LiveData<ApiResult<List<AppNotification>>> getNotifications() {
        MutableLiveData<ApiResult<List<AppNotification>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.getNotifications().enqueue(new Callback<ApiResponse<List<AppNotification>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<AppNotification>>> call,
                                   @NonNull Response<ApiResponse<List<AppNotification>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<AppNotification> list = response.body().getData();
                    liveData.postValue(ApiResult.success(list != null ? list : new ArrayList<>()));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<AppNotification>>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public void markNotificationRead(String id) {
        api.markNotificationRead(id).enqueue(new Callback<ApiResponse<AppNotification>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AppNotification>> call,
                                   @NonNull Response<ApiResponse<AppNotification>> response) {}

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AppNotification>> call, @NonNull Throwable t) {}
        });
    }
}
