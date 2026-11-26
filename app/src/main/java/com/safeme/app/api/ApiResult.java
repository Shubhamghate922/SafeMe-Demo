package com.safeme.app.api;

/**
 * Represents loading, success, error, or unauthorized API states for ViewModels and Fragments.
 */
public class ApiResult<T> {

    public enum Status {
        LOADING,
        SUCCESS,
        ERROR,
        UNAUTHORIZED
    }

    private final Status status;
    private final T data;
    private final String message;
    private final boolean offlineCached;

    private ApiResult(Status status, T data, String message, boolean offlineCached) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.offlineCached = offlineCached;
    }

    public static <T> ApiResult<T> loading() {
        return new ApiResult<>(Status.LOADING, null, null, false);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(Status.SUCCESS, data, null, false);
    }

    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(Status.SUCCESS, data, message, false);
    }

    public static <T> ApiResult<T> cached(T data, String message) {
        return new ApiResult<>(Status.SUCCESS, data, message, true);
    }

    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(Status.ERROR, null, message, false);
    }

    public static <T> ApiResult<T> unauthorized(String message) {
        return new ApiResult<>(Status.UNAUTHORIZED, null, message, false);
    }

    public Status getStatus() { return status; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public boolean isOfflineCached() { return offlineCached; }
}
