package com.safeme.app.api;

import androidx.annotation.NonNull;

import com.safeme.app.utils.SecureTokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Attaches the Keystore-protected JWT Bearer token to outgoing requests
 * and notifies listeners when the server rejects an expired or invalid token (401).
 */
public class AuthInterceptor implements Interceptor {

    public interface SessionExpiredListener {
        void onSessionExpired();
    }

    private final SecureTokenManager tokenManager;
    private volatile SessionExpiredListener sessionExpiredListener;

    public AuthInterceptor(SecureTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public void setSessionExpiredListener(SessionExpiredListener listener) {
        this.sessionExpiredListener = listener;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();

        boolean isPublicAuthPath = path.endsWith("/auth/login")
                || path.endsWith("/auth/register")
                || path.endsWith("/auth/verify-otp")
                || path.endsWith("/auth/resend-otp")
                || path.endsWith("/auth/forgot-password")
                || path.endsWith("/auth/reset-password");

        Request.Builder builder = original.newBuilder()
                .header("Accept", "application/json");

        String token = tokenManager.getToken();
        if (!isPublicAuthPath && token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        Response response = chain.proceed(builder.build());

        if (response.code() == 401 && !isPublicAuthPath) {
            tokenManager.clearSession();
            if (sessionExpiredListener != null) {
                sessionExpiredListener.onSessionExpired();
            }
        }
        return response;
    }
}
