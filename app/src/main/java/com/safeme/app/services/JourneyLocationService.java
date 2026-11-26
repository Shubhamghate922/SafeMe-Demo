package com.safeme.app.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.safeme.app.SafeMeApplication;
import com.safeme.app.activities.MainActivity;
import com.safeme.app.api.RetrofitClient;
import com.safeme.app.api.SafeMeApiService;
import com.safeme.app.models.ApiResponse;
import com.safeme.app.models.LocationUpdate;
import com.safeme.app.utils.PermissionHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Foreground location service for active user-initiated Journey Monitoring and Emergency SOS sessions.
 * Strictly stops when the journey or emergency ends.
 */
public class JourneyLocationService extends Service {

    private static final String TAG = "JourneyLocationService";
    private static final int NOTIFICATION_ID = 4102;

    public static final String ACTION_START_JOURNEY = "com.safeme.app.ACTION_START_JOURNEY";
    public static final String ACTION_START_SOS = "com.safeme.app.ACTION_START_SOS";
    public static final String ACTION_STOP = "com.safeme.app.ACTION_STOP";

    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_LABEL = "extra_label";

    private static final MutableLiveData<LocationUpdate> latestLocationLiveData = new MutableLiveData<>();
    private static final MutableLiveData<Boolean> serviceRunningLiveData = new MutableLiveData<>(false);

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String currentSessionId;
    private boolean isSosMode = false;

    public static LiveData<LocationUpdate> getLatestLocationLiveData() {
        return latestLocationLiveData;
    }

    public static LiveData<Boolean> isServiceRunning() {
        return serviceRunningLiveData;
    }

    public static void startForJourney(Context context, String journeyId, String destinationName) {
        Intent intent = new Intent(context, JourneyLocationService.class);
        intent.setAction(ACTION_START_JOURNEY);
        intent.putExtra(EXTRA_SESSION_ID, journeyId);
        intent.putExtra(EXTRA_LABEL, destinationName);
        startServiceCompat(context, intent);
    }

    public static void startForSos(Context context, String sosId) {
        Intent intent = new Intent(context, JourneyLocationService.class);
        intent.setAction(ACTION_START_SOS);
        intent.putExtra(EXTRA_SESSION_ID, sosId);
        intent.putExtra(EXTRA_LABEL, "Emergency SOS");
        startServiceCompat(context, intent);
    }

    public static void stopMonitoring(Context context) {
        Intent intent = new Intent(context, JourneyLocationService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    private static void startServiceCompat(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location last = locationResult.getLastLocation();
                if (last != null) {
                    handleNewLocation(last);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelfCleanly();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelfCleanly();
            return START_NOT_STICKY;
        }

        if (!PermissionHelper.hasLocationPermission(this)) {
            Log.w(TAG, "Cannot start location foreground service without runtime location permission.");
            stopSelfCleanly();
            return START_NOT_STICKY;
        }

        currentSessionId = intent.getStringExtra(EXTRA_SESSION_ID);
        String label = intent.getStringExtra(EXTRA_LABEL);
        isSosMode = ACTION_START_SOS.equals(action);

        Notification notification = buildOngoingNotification(label);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        serviceRunningLiveData.postValue(true);
        startLocationUpdates();
        return START_STICKY;
    }

    private Notification buildOngoingNotification(String label) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = isSosMode
                ? "SafeMe Emergency SOS Active"
                : "SafeMe Journey Monitoring Active";
        String text = isSosMode
                ? "Sharing real-time GPS coordinates with trusted contacts."
                : "Monitoring route to " + (label != null ? label : "destination");

        String channel = isSosMode ? SafeMeApplication.CHANNEL_EMERGENCY : SafeMeApplication.CHANNEL_JOURNEY;

        return new NotificationCompat.Builder(this, channel)
                .setSmallIcon(R.drawable.ic_safety)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setPriority(isSosMode ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void startLocationUpdates() {
        try {
            long intervalMs = isSosMode ? 8000L : 15000L;
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
                    .setMinUpdateIntervalMillis(5000L)
                    .build();

            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing while requesting updates", e);
            stopSelfCleanly();
        }
    }

    private void handleNewLocation(Location location) {
        LocationUpdate update = new LocationUpdate(
                location.getLatitude(),
                location.getLongitude(),
                location.getAccuracy(),
                location.getSpeed(),
                location.getBearing(),
                System.currentTimeMillis()
        );
        latestLocationLiveData.postValue(update);

        if (currentSessionId == null || currentSessionId.trim().isEmpty()) {
            return;
        }

        SafeMeApiService api = RetrofitClient.getApiService(this);
        Call<ApiResponse<LocationUpdate>> call = isSosMode
                ? api.sendSosLocation(currentSessionId, update)
                : api.sendJourneyLocation(currentSessionId, update);

        call.enqueue(new Callback<ApiResponse<LocationUpdate>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<LocationUpdate>> call,
                                   @NonNull Response<ApiResponse<LocationUpdate>> response) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "Server rejected location sync HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<LocationUpdate>> call, @NonNull Throwable t) {
                Log.w(TAG, "Offline or failed sending location update: " + t.getMessage());
            }
        });
    }

    private void stopSelfCleanly() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        serviceRunningLiveData.postValue(false);
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        serviceRunningLiveData.postValue(false);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
