package com.safeme.app.api;

import android.content.Context;

import com.example.BuildConfig;
import com.safeme.app.utils.SecureTokenManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Provides the configured Retrofit instance using a dynamic base URL from SecureTokenManager.
 */
public final class RetrofitClient {

    private static volatile Retrofit retrofit;
    private static volatile SafeMeApiService apiService;
    private static volatile String activeBaseUrl;
    private static volatile Boolean activeLocalMode;
    private static volatile AuthInterceptor authInterceptor;

    private RetrofitClient() {}

    public static synchronized SafeMeApiService getApiService(Context context) {
        SecureTokenManager tokenManager = SecureTokenManager.getInstance(context);
        String configuredUrl = tokenManager.getApiBaseUrl();
        boolean localMode = tokenManager.isLocalBackendMode();

        if (apiService == null || !configuredUrl.equals(activeBaseUrl)
                || activeLocalMode == null || activeLocalMode != localMode) {
            activeBaseUrl = configuredUrl;
            activeLocalMode = localMode;
            authInterceptor = new AuthInterceptor(tokenManager);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BASIC
                    : HttpLoggingInterceptor.Level.NONE);
            // Redact Authorization header from logs
            logging.redactHeader("Authorization");

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .writeTimeout(25, TimeUnit.SECONDS)
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging);

            if (localMode) {
                clientBuilder.addInterceptor(new com.safeme.app.localbackend.LocalBackendInterceptor(context));
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(activeBaseUrl)
                    .client(clientBuilder.build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(SafeMeApiService.class);
        }
        return apiService;
    }

    public static synchronized void resetClient() {
        retrofit = null;
        apiService = null;
        activeBaseUrl = null;
        activeLocalMode = null;
    }

    public static AuthInterceptor getAuthInterceptor() {
        return authInterceptor;
    }
}
