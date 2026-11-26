package com.safeme.app.api;

import com.google.gson.Gson;
import com.safeme.app.models.ApiResponse;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Translates HTTP error responses and network exceptions into clear user-facing messages.
 */
public final class ApiErrorParser {

    private static final Gson GSON = new Gson();

    private ApiErrorParser() {}

    public static String parseHttpError(Response<?> response) {
        if (response == null) {
            return "Unexpected empty server response.";
        }
        int code = response.code();
        if (code == 401) {
            return "Authentication failed or session expired. Please sign in again.";
        }
        if (code == 403) {
            return "You do not have permission to perform this action.";
        }
        if (code == 404) {
            return "Requested resource or endpoint was not found on the server.";
        }

        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                String raw = errorBody.string();
                if (raw != null && !raw.trim().isEmpty()) {
                    ApiResponse<?> parsed = GSON.fromJson(raw, ApiResponse.class);
                    if (parsed != null) {
                        if (parsed.getErrors() != null && !parsed.getErrors().isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (Map.Entry<String, String> entry : parsed.getErrors().entrySet()) {
                                if (sb.length() > 0) sb.append("\n");
                                sb.append(entry.getValue());
                            }
                            return sb.toString();
                        }
                        if (parsed.getMessage() != null && !parsed.getMessage().trim().isEmpty()) {
                            return parsed.getMessage();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to status code description
        }

        if (code >= 500) {
            return "Server encountered an internal error (" + code + "). Please try again shortly.";
        }
        return "Request failed with HTTP status " + code + ".";
    }

    public static String parseThrowable(Throwable t) {
        if (t == null) {
            return "Unknown network error occurred.";
        }
        if (t instanceof SocketTimeoutException) {
            return "Connection timed out while contacting the SafeMe backend. Please retry.";
        }
        if (t instanceof ConnectException || t instanceof UnknownHostException) {
            return "Unable to reach the SafeMe backend server. Check your internet connection or verify the Server URL in Settings.";
        }
        String msg = t.getMessage();
        return (msg != null && !msg.trim().isEmpty())
                ? "Network error: " + msg
                : "Network connection error. Please check connectivity and retry.";
    }
}
