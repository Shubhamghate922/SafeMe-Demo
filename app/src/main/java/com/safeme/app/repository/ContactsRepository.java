package com.safeme.app.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.safeme.app.api.ApiErrorParser;
import com.safeme.app.api.ApiResult;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.api.SafeMeApiService;
import com.safeme.app.database.CachedContactEntity;
import com.safeme.app.database.SafeMeDatabase;
import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.TrustedContact;
import com.safeme.app.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Manages CRUD operations for Trusted Contacts with Room offline fallback cache.
 */
public class ContactsRepository {

    private final Context appContext;
    private final SafeMeApiService api;
    private final SafeMeDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ContactsRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.api = RetrofitClient.getApiService(appContext);
        this.database = SafeMeDatabase.getInstance(appContext);
    }

    public LiveData<ApiResult<List<TrustedContact>>> getContacts() {
        MutableLiveData<ApiResult<List<TrustedContact>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        if (!com.safeme.app.utils.SecureTokenManager.getInstance(appContext).isLocalBackendMode()
                && !NetworkUtils.isInternetAvailable(appContext)) {
            loadCachedContacts(liveData, "Offline mode: showing cached contacts.");
            return liveData;
        }

        api.getContacts().enqueue(new Callback<ApiResponse<List<TrustedContact>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<TrustedContact>>> call,
                                   @NonNull Response<ApiResponse<List<TrustedContact>>> response) {
                if (response.code() == 401) {
                    liveData.postValue(ApiResult.unauthorized("Session expired."));
                } else if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<TrustedContact> contacts = response.body().getData();
                    if (contacts == null) contacts = new ArrayList<>();
                    cacheContacts(contacts);
                    liveData.postValue(ApiResult.success(contacts));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<TrustedContact>>> call, @NonNull Throwable t) {
                loadCachedContacts(liveData, ApiErrorParser.parseThrowable(t));
            }
        });

        return liveData;
    }

    public LiveData<ApiResult<TrustedContact>> addContact(TrustedContact contact) {
        MutableLiveData<ApiResult<TrustedContact>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.addContact(contact).enqueue(new Callback<ApiResponse<TrustedContact>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TrustedContact>> call,
                                   @NonNull Response<ApiResponse<TrustedContact>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "Trusted contact added."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TrustedContact>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<TrustedContact>> updateContact(String id, TrustedContact contact) {
        MutableLiveData<ApiResult<TrustedContact>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.updateContact(id, contact).enqueue(new Callback<ApiResponse<TrustedContact>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TrustedContact>> call,
                                   @NonNull Response<ApiResponse<TrustedContact>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.postValue(ApiResult.success(response.body().getData(), "Contact updated."));
                } else {
                    liveData.postValue(ApiResult.error(ApiErrorParser.parseHttpError(response)));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TrustedContact>> call, @NonNull Throwable t) {
                liveData.postValue(ApiResult.error(ApiErrorParser.parseThrowable(t)));
            }
        });
        return liveData;
    }

    public LiveData<ApiResult<Void>> deleteContact(String id) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        api.deleteContact(id).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    liveData.postValue(ApiResult.success(null, "Contact removed."));
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

    private void cacheContacts(List<TrustedContact> contacts) {
        executor.execute(() -> {
            List<CachedContactEntity> entities = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (TrustedContact c : contacts) {
                if (c.getId() != null) {
                    entities.add(new CachedContactEntity(
                            c.getId(), c.getName(), c.getPhoneNumber(), c.getRelationship(),
                            c.getPriority(), c.getVerificationStatus(), now));
                }
            }
            database.safeMeDao().clearContacts();
            database.safeMeDao().upsertContacts(entities);
        });
    }

    private void loadCachedContacts(MutableLiveData<ApiResult<List<TrustedContact>>> liveData, String fallbackMsg) {
        executor.execute(() -> {
            List<CachedContactEntity> cached = database.safeMeDao().getAllCachedContacts();
            if (cached != null && !cached.isEmpty()) {
                List<TrustedContact> result = new ArrayList<>();
                for (CachedContactEntity e : cached) {
                    TrustedContact tc = new TrustedContact();
                    tc.setId(e.id);
                    tc.setName(e.name);
                    tc.setPhoneNumber(e.phoneNumber);
                    tc.setRelationship(e.relationship);
                    tc.setPriority(e.priority);
                    tc.setVerificationStatus(e.verificationStatus);
                    result.add(tc);
                }
                liveData.postValue(ApiResult.cached(result, fallbackMsg));
            } else {
                liveData.postValue(ApiResult.error(fallbackMsg));
            }
        });
    }
}
