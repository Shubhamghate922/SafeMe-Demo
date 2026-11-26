package com.safeme.app.localbackend;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * OkHttp Interceptor active only when On-Device Local Backend Mode is enabled.
 * Routes Retrofit REST calls through LocalBackendRouter + SQLite instead of remote HTTP.
 */
public class LocalBackendInterceptor implements Interceptor {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final LocalBackendRouter router;

    public LocalBackendInterceptor(Context context) {
        this.router = new LocalBackendRouter(context.getApplicationContext());
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String method = request.method();
        String path = request.url().encodedPath();
        String authHeader = request.header("Authorization");

        String jsonBody = null;
        Map<String, String> multipartFields = null;
        boolean hasPhoto = false;
        boolean hasAudio = false;

        RequestBody body = request.body();
        if (body instanceof MultipartBody) {
            MultipartBody mb = (MultipartBody) body;
            multipartFields = new HashMap<>();
            for (MultipartBody.Part part : mb.parts()) {
                Headers headers = part.headers();
                String disposition = headers != null ? headers.get("Content-Disposition") : null;
                String fieldName = extractFormName(disposition);
                if ("photo".equals(fieldName)) {
                    hasPhoto = true;
                } else if ("audio".equals(fieldName)) {
                    hasAudio = true;
                } else if (fieldName != null) {
                    Buffer buf = new Buffer();
                    part.body().writeTo(buf);
                    multipartFields.put(fieldName, buf.readString(StandardCharsets.UTF_8));
                }
            }
        } else if (body != null) {
            Buffer buf = new Buffer();
            body.writeTo(buf);
            jsonBody = buf.readString(StandardCharsets.UTF_8);
        }

        LocalBackendRouter.HttpResult result = router.route(
                method, path, authHeader, jsonBody, multipartFields, hasPhoto, hasAudio);

        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(result.statusCode)
                .message(result.statusCode >= 200 && result.statusCode < 300 ? "OK" : "Error")
                .body(ResponseBody.create(JSON_MEDIA_TYPE, result.jsonBody))
                .build();
    }

    private String extractFormName(String contentDisposition) {
        if (contentDisposition == null) return null;
        int idx = contentDisposition.indexOf("name=\"");
        if (idx < 0) return null;
        int start = idx + 6;
        int end = contentDisposition.indexOf("\"", start);
        return end > start ? contentDisposition.substring(start, end) : null;
    }
}
