package com.safeme.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * Standard Spring Boot REST API response envelope.
 */
public class ApiResponse<T> {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    @SerializedName("errors")
    private Map<String, String> errors;

    @SerializedName("timestamp")
    private String timestamp;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
